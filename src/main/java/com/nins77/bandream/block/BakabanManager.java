package com.nins77.bandream.block;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.nins77.bandream.enforce.BanEnforcement;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

public final class BakabanManager {
    private static final int CHUNKS_PER_TICK = 4;
    private static final Map<ResourceKey<Level>, Deque<ChunkScan>> SCAN_QUEUE = new HashMap<>();
    private static final Map<ResourceKey<Level>, Set<Long>> QUEUED = new HashMap<>();

    private BakabanManager() {
    }

    public static void clear() {
        SCAN_QUEUE.clear();
        QUEUED.clear();
    }

    public static void onChunkLoad(ServerLevel level, ChunkPos pos) {
        if (BakabanSavedData.get(level).hasEntriesInChunk(pos)) {
            enqueue(level.dimension(), pos.toLong());
        }
    }

    public static void onChunkUnload(ServerLevel level, ChunkPos pos) {
        long key = pos.toLong();
        Set<Long> queued = QUEUED.get(level.dimension());
        if (queued != null && queued.remove(key)) {
            Deque<ChunkScan> queue = SCAN_QUEUE.get(level.dimension());
            if (queue != null) {
                queue.removeIf(scan -> scan.key == key);
            }
        }
    }

    public static void refresh(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            BakabanSavedData data = BakabanSavedData.get(level);
            for (BlockPos pos : data.positions()) {
                if (isLoaded(level, pos)) {
                    restoreStored(level, pos, null);
                }
            }
        }
    }

    public static void tick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            Deque<ChunkScan> queue = SCAN_QUEUE.get(level.dimension());
            Set<Long> queued = QUEUED.get(level.dimension());
            if (queue == null || queued == null) {
                continue;
            }
            int processed = 0;
            while (processed < CHUNKS_PER_TICK && !queue.isEmpty()) {
                ChunkScan scan = queue.pollFirst();
                if (scan == null) {
                    break;
                }
                queued.remove(scan.key);
                LevelChunk chunk = level.getChunkSource().getChunkNow(scan.pos.x, scan.pos.z);
                if (chunk != null) {
                    processChunk(level, chunk);
                }
                processed++;
            }
        }
    }

    public static boolean restore(Level level, BlockPos pos, Player player) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        return restoreStored(serverLevel, pos,
            player instanceof ServerPlayer serverPlayer ? serverPlayer : null);
    }

    public static boolean hasSnapshot(ServerLevel level, BlockPos pos) {
        return BakabanSavedData.get(level).get(pos) != null;
    }

    private static boolean restoreStored(ServerLevel level, BlockPos pos, ServerPlayer player) {
        BakabanSavedData data = BakabanSavedData.get(level);
        BakabanSavedData.StoredBlock stored = data.get(pos);
        if (stored == null) {
            return false;
        }
        BlockState current = level.getBlockState(pos);
        if (!current.is(BanBlocks.BAKABAN.get()) && !current.isAir()) {
            if (current.equals(stored.state())) {
                data.remove(pos);
            }
            return false;
        }
        if (!level.setBlock(pos, stored.state(), Block.UPDATE_ALL)) {
            return false;
        }
        boolean loaded = true;
        if (stored.blockEntity() != null) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                try {
                    blockEntity.load(stored.blockEntity().copy());
                    blockEntity.setChanged();
                } catch (RuntimeException ignored) {
                    loaded = false;
                }
            } else {
                loaded = false;
            }
        }
        if (!loaded) {
            level.setBlock(pos, BanBlocks.BAKABAN.get().defaultBlockState(), Block.UPDATE_ALL);
            if (player != null) {
                BanEnforcement.notify(player, "原方块内容无法完整恢复，已保留数据");
            }
            return false;
        }
        data.remove(pos);
        if (player != null) {
            BanEnforcement.notify(player, "已恢复原方块");
        }
        return true;
    }

    private static void processChunk(ServerLevel level, LevelChunk chunk) {
        for (BlockPos pos : BakabanSavedData.get(level).positionsInChunk(chunk.getPos())) {
            restoreStored(level, pos, null);
        }
    }

    private static boolean isLoaded(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
    }

    private static void enqueue(ResourceKey<Level> dimension, long key) {
        Set<Long> queued = QUEUED.computeIfAbsent(dimension, ignored -> new HashSet<>());
        if (queued.add(key)) {
            SCAN_QUEUE.computeIfAbsent(dimension, ignored -> new ArrayDeque<>())
                .add(new ChunkScan(key));
        }
    }

    private static final class ChunkScan {
        private final long key;
        private final ChunkPos pos;

        private ChunkScan(long key) {
            this.key = key;
            this.pos = new ChunkPos(key);
        }
    }
}
