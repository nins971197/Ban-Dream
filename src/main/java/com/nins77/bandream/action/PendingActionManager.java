package com.nins77.bandream.action;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.nins77.bandream.config.BanType;
import net.minecraft.server.level.ServerPlayer;

public final class PendingActionManager {
    private static final long TIMEOUT_MS = 60_000L;
    private static final Map<UUID, Action> PENDING = new ConcurrentHashMap<>();
    private static final AtomicInteger NEXT_ID = new AtomicInteger();

    private PendingActionManager() {
    }

    public static int offer(ServerPlayer player, Kind kind, BanType type, List<String> ids) {
        int id = NEXT_ID.incrementAndGet();
        PENDING.put(player.getUUID(), new Action(
            id,
            kind,
            type,
            List.copyOf(ids),
            System.currentTimeMillis() + TIMEOUT_MS));
        return id;
    }

    public static Action take(ServerPlayer player, int id) {
        Action action = PENDING.get(player.getUUID());
        if (action == null) {
            return null;
        }
        if (action.expiresAt() < System.currentTimeMillis()) {
            PENDING.remove(player.getUUID());
            return null;
        }
        if (action.id() != id) {
            return null;
        }
        PENDING.remove(player.getUUID());
        return action;
    }

    public static void clear(ServerPlayer player) {
        PENDING.remove(player.getUUID());
    }

    public enum Kind {
        STICK_BLOCK,
        STICK_ENTITY,
        STICK_ITEMS,
        UNBAN,
        LIST_ENTRY
    }

    public record Action(int id, Kind kind, BanType type, List<String> ids, long expiresAt) {
    }
}
