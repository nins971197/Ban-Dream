package com.nins77.bandream.block;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.nins77.bandream.config.BanRules;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public final class BakabanScanner {
    private static final int CELLS_PER_TICK = 8192;
    private static final int REDIRECTS_PER_TICK = 128;
    private static final Map<ResourceKey<Level>, Set<Long>> LOADED_CHUNKS = new HashMap<>();
    private static final Map<ResourceKey<Level>, Deque<ChunkScan>> QUEUE = new HashMap<>();
    private static final Map<ResourceKey<Level>, Set<Long>> QUEUED = new HashMap<>();

    private BakabanScanner() {
    }

    public static void clear() {
        LOADED_CHUNKS.clear();
        QUEUE.clear();
        QUEUED.clear();
    }

    public static void onChunkLoad(ServerLevel level, ChunkPos pos) {
        LOADED_CHUNKS.computeIfAbsent(level.dimension(), ignored -> new HashSet<>()).add(pos.toLong());
        if (shouldScan(level)) {
            enqueue(level.dimension(), pos.toLong());
        }
    }

    public static void onChunkUnload(ServerLevel level, ChunkPos pos) {
        long key = pos.toLong();
        Set<Long> loaded = LOADED_CHUNKS.get(level.dimension());
        if (loaded != null) {
            loaded.remove(key);
        }
        Set<Long> queued = QUEUED.get(level.dimension());
        if (queued != null && queued.remove(key)) {
            Deque<ChunkScan> queue = QUEUE.get(level.dimension());
            if (queue != null) {
                queue.removeIf(scan -> scan.key == key);
            }
        }
    }

    public static void refresh(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            if (!shouldScan(level)) {
                continue;
            }
            for (long key : level.getForcedChunks()) {
                enqueue(level.dimension(), key);
            }
            Set<Long> loaded = LOADED_CHUNKS.get(level.dimension());
            if (loaded != null) {
                for (long key : loaded) {
                    enqueue(level.dimension(), key);
                }
            }
        }
    }

    public static void tick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            Deque<ChunkScan> queue = QUEUE.get(level.dimension());
            Set<Long> queued = QUEUED.get(level.dimension());
            if (queue == null || queued == null) {
                continue;
            }
            ScanBudget budget = new ScanBudget();
            while (!queue.isEmpty()
                && budget.cells < CELLS_PER_TICK
                && budget.redirects < REDIRECTS_PER_TICK) {
                ChunkScan scan = queue.pollFirst();
                if (scan == null) {
                    break;
                }
                LevelChunk chunk = level.getChunkSource().getChunkNow(scan.pos.x, scan.pos.z);
                if (chunk == null) {
                    queued.remove(scan.key);
                    continue;
                }
                if (processChunk(level, chunk, scan, budget)) {
                    queued.remove(scan.key);
                } else {
                    queue.addLast(scan);
                    break;
                }
            }
        }
    }

    private static boolean processChunk(
        ServerLevel level,
        LevelChunk chunk,
        ChunkScan scan,
        ScanBudget budget
    ) {
        if (!scan.restored) {
            for (BlockEntity blockEntity : new ArrayList<>(chunk.getBlockEntities().values())) {
                if (blockEntity instanceof BakabanBlockEntity marker && marker.snapshotId() != null) {
                    BakabanService.restore(level, blockEntity.getBlockPos());
                }
            }
            scan.restored = true;
        }
        if (!BanRules.hasBlockRules()) {
            return true;
        }

        Predicate<BlockState> banned = state ->
            !state.isAir()
                && !state.is(BanBlocks.BAKABAN.get())
                && BanRules.isBlockBanned(state.getBlock(), level.dimension().location());
        LevelChunkSection[] sections = chunk.getSections();
        while (scan.sectionIndex < sections.length) {
            LevelChunkSection section = sections[scan.sectionIndex];
            if (section == null || section.hasOnlyAir() || !section.maybeHas(banned)) {
                scan.sectionIndex++;
                scan.cellIndex = 0;
                continue;
            }
            int sectionY = level.getSectionYFromSectionIndex(scan.sectionIndex);
            while (scan.cellIndex < 4096) {
                if (budget.cells >= CELLS_PER_TICK) {
                    return false;
                }
                if (budget.redirects >= REDIRECTS_PER_TICK) {
                    return false;
                }
                int cell = scan.cellIndex++;
                budget.cells++;
                int x = cell & 15;
                int y = (cell >> 8) & 15;
                int z = (cell >> 4) & 15;
                BlockState state = section.getBlockState(x, y, z);
                if (banned.test(state)) {
                    BlockPos pos = new BlockPos(
                        scan.pos.getMinBlockX() + x,
                        sectionY * 16 + y,
                        scan.pos.getMinBlockZ() + z);
                    if (BakabanService.redirect(level, pos)) {
                        budget.redirects++;
                    }
                }
            }
            scan.sectionIndex++;
            scan.cellIndex = 0;
        }
        return true;
    }

    private static boolean shouldScan(ServerLevel level) {
        return BanRules.hasBlockRules() || BakabanSnapshotStore.get(level).hasSnapshots();
    }

    private static void enqueue(ResourceKey<Level> dimension, long key) {
        Set<Long> queued = QUEUED.computeIfAbsent(dimension, ignored -> new HashSet<>());
        if (queued.add(key)) {
            QUEUE.computeIfAbsent(dimension, ignored -> new ArrayDeque<>()).add(new ChunkScan(key));
        }
    }

    private static final class ChunkScan {
        private final long key;
        private final ChunkPos pos;
        private boolean restored;
        private int sectionIndex;
        private int cellIndex;

        private ChunkScan(long key) {
            this.key = key;
            this.pos = new ChunkPos(key);
        }
    }

    private static final class ScanBudget {
        private int cells;
        private int redirects;
    }
}
