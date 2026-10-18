package com.nins77.bandream.action;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.nins77.bandream.config.BanEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

public final class PendingBanManager {
    private static final long CONFIRM_TIMEOUT_MS = 60_000L;
    private static final Map<UUID, PendingBan> PENDING = new ConcurrentHashMap<>();

    private PendingBanManager() {
    }

    public static void offer(ServerPlayer player, List<BanEntry> entries, String summary) {
        if (entries.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        PendingBan existing = PENDING.get(player.getUUID());
        if (existing != null && existing.expiresAt() >= now && existing.summary().equals(summary)
            && sameEntries(existing.entries(), entries)) {
            return;
        }
        PENDING.put(player.getUUID(), new PendingBan(
            entries.stream().map(BanEntry::copy).toList(),
            summary,
            now + CONFIRM_TIMEOUT_MS));

        Style confirmStyle = Style.EMPTY
            .withColor(ChatFormatting.GREEN)
            .withBold(true)
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bbban confirm"))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                Component.literal("执行 /bbban confirm")));
        Style cancelStyle = Style.EMPTY
            .withColor(ChatFormatting.RED)
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bbban cancel"));

        player.sendSystemMessage(Component.literal("==== Ban Dream · 等待确认 ====")
            .withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal(summary).withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.literal("60 秒内输入 ")
            .append(Component.literal("[/bbban confirm]").withStyle(confirmStyle))
            .append(Component.literal(" 确认，或 "))
            .append(Component.literal("[/bbban cancel]").withStyle(cancelStyle))
            .append(Component.literal(" 取消"))
            .withStyle(ChatFormatting.GRAY));
    }

    public static PendingBan get(ServerPlayer player) {
        PendingBan pending = PENDING.get(player.getUUID());
        if (pending == null) {
            return null;
        }
        if (pending.expiresAt() < System.currentTimeMillis()) {
            PENDING.remove(player.getUUID());
            return null;
        }
        return pending;
    }

    public static void clear(ServerPlayer player) {
        PENDING.remove(player.getUUID());
    }

    private static boolean sameEntries(List<BanEntry> left, List<BanEntry> right) {
        if (left.size() != right.size()) {
            return false;
        }
        for (int index = 0; index < left.size(); index++) {
            if (!left.get(index).sameKey(right.get(index))) {
                return false;
            }
        }
        return true;
    }

    public record PendingBan(List<BanEntry> entries, String summary, long expiresAt) {
    }
}
