package com.nins77.bandream.client;

import java.util.List;

import com.nins77.bandream.config.BanRules;
import com.nins77.bandream.config.BanType;
import com.nins77.bandream.item.BanItems;
import com.nins77.bandream.network.SyncVisualPacket;
import com.nins77.bandream.network.VisualRule;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class BanVisualState {
    private static final ThreadLocal<Boolean> RESOLVING = ThreadLocal.withInitial(() -> false);
    private static volatile List<VisualRule> rules = List.of();
    private static volatile boolean enabled = true;

    private BanVisualState() {
    }

    public static void accept(SyncVisualPacket packet) {
        rules = List.copyOf(packet.rules());
        enabled = packet.enabled();
    }

    public static Item replacementFor(ItemStack stack, Level level) {
        if (!enabled || stack.isEmpty() || RESOLVING.get()) {
            return null;
        }
        String dimension = dimension(level);
        String itemId = idOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        if (matchesItem(itemId, dimension, stack)) {
            return BanItems.BAN_999.get();
        }
        if (stack.getItem() instanceof BlockItem blockItem
            && matchesBlock(idOf(BuiltInRegistries.BLOCK.getKey(blockItem.getBlock())), dimension)) {
            return BanItems.BAKABAN.get();
        }
        return null;
    }

    public static void enterOverride() {
        RESOLVING.set(true);
    }

    public static void exitOverride() {
        RESOLVING.remove();
    }

    private static boolean matchesItem(String itemId, String dimension, ItemStack stack) {
        for (VisualRule rule : rules) {
            if (!rule.matchesDimension(dimension)) {
                continue;
            }
            if (rule.type() == BanType.ITEM && rule.id().equals(itemId)) {
                return true;
            }
            if (rule.type() == BanType.NBT
                && ("*".equals(rule.id()) || rule.id().equals(itemId))
                && BanRules.matchesNbtPattern(stack, rule.path(), rule.value())) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesBlock(String blockId, String dimension) {
        for (VisualRule rule : rules) {
            if (rule.type() == BanType.BLOCK
                && rule.matchesDimension(dimension)
                && rule.id().equals(blockId)) {
                return true;
            }
        }
        return false;
    }

    private static String dimension(Level level) {
        Level current = level == null ? Minecraft.getInstance().level : level;
        return current == null ? "" : current.dimension().location().toString();
    }

    private static String idOf(ResourceLocation id) {
        return id == null ? "" : id.toString();
    }
}
