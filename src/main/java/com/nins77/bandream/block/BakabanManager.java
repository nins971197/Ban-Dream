package com.nins77.bandream.block;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.nins77.bandream.config.BanRules;
import com.nins77.bandream.enforce.BanEnforcement;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.level.chunk.LevelChunkSection;

public final class BakabanManager {
    private static final int CELLS_PER_TICK = 8192;
    private static final int REDIRECTS_PER_TICK = 128;
    private static final Map<ResourceKey<Level>, Set<Long>> LOADED_CHUNKS = new HashMap<>();
    private static final Map<ResourceKey<Level>, Deque<ChunkScan>> SCAN_QUEUE = new HashMap<>();
    private static final Map<ResourceKey<Level>, Set<Long>> QUEUED = new HashMap<>();

    private BakabanManager() {
    }

    public static void clear() {
        LOADED_CHUNKS.clear();
        SCAN_QUEUE.clear();
        QUEUED.clear();
    }

    public static void onChunkLoad(ServerLevel level, ChunkPos pos) {
        ResourceKey<Level> dimension = level.dimension();
        LOADED_CHUNKS.computeIfAbsent(dimension, ignored -> new HashSet<>()).add(pos.toLong());
        BakabanSavedData data = BakabanSavedData.get(level);
        if (BanRules.hasBlockRules() || data.hasEntriesInChunk(pos)) {
            enqueue(dimension, pos.toLong());
        }
    }

    public static void onChunkUnload(ServerLevel level, ChunkPos pos) {
        ResourceKey<Level> dimension = level.dimension();
        Set<Long> loaded = LOADED_CHUNKS.get(dimension);
        if (loaded != null) {
            loaded.remove(pos.toLong());
        }
        long key = pos.toLong();
        Set<Long> queued = QUEUED.get(dimension);
        if (queued != null && queued.remove(key)) {
            Deque<ChunkScan> queue = SCAN_QUEUE.get(dimension);
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
            for (ServerPlayer player : level.players()) {
                enqueueFirst(level.dimension(), player.chunkPosition().toLong());
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
            ResourceKey<Level> dimension = level.dimension();
            Deque<ChunkScan> queue = SCAN_QUEUE.get(dimension);
            Set<Long> queued = QUEUED.get(dimension);
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

    public static boolean redirect(ServerLevel level, BlockPos pos) {
        if (!BanRules.hasBlockRules()) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.is(BanBlocks.BAKABAN.get())) {
            return false;
        }
        if (!BanRules.isBlockBanned(state.getBlock(), level.dimension().location())) {
            return false;
        }
        CompoundTag blockEntity = null;
        BlockEntity existing = level.getBlockEntity(pos);
        if (existing != null) {
            try {
                blockEntity = existing.saveWithFullMetadata();
            } catch (RuntimeException ignored) {
                return false;
            }
        }
        BakabanSavedData data = BakabanSavedData.get(level);
        data.put(pos, state, blockEntity);
        if (!level.setBlock(pos, BanBlocks.BAKABAN.get().defaultBlockState(), Block.UPDATE_ALL)) {
            data.remove(pos);
            return false;
        }
        return true;
    }

    public static boolean restore(Level level, BlockPos pos, Player player) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        return restoreStored(serverLevel, pos, player instanceof ServerPlayer serverPlayer ? serverPlayer : null);
    }

    public static boolean hasSnapshot(ServerLevel level, BlockPos pos) {
        return BakabanSavedData.get(level).get(pos) != null;
    }

    private static boolean restoreStored(ServerLevel level, BlockPos pos, ServerPlayer player) {
        BakabanSavedData data = BakabanSavedData.get(level);
        BakabanSavedData.StoredBlock stored = data.get(pos);
        if (stored == null) {
            if (player != null) {
                BanEnforcement.notify(player, "这里没有可恢复的原方块");
            }
            return false;
        }
        if (BanRules.isBlockBanned(stored.state().getBlock(), level.dimension().location())) {
            if (player != null) {
                BanEnforcement.notify(player, "该方块仍被封禁，无法恢复");
            }
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

    private static boolean processChunk(
        ServerLevel level,
        LevelChunk chunk,
        ChunkScan scan,
        ScanBudget budget
    ) {
        BakabanSavedData data = BakabanSavedData.get(level);
        if (!scan.restored) {
            for (BlockPos pos : data.positionsInChunk(scan.pos)) {
                restoreStored(level, pos, null);
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
                if (budget.redirects >= REDIRECTS_PER_TICK && banned.test(section.getBlockState(
                    scan.cellIndex & 15,
                    (scan.cellIndex >> 8) & 15,
                    (scan.cellIndex >> 4) & 15))) {
                    return false;
                }
                int cell = scan.cellIndex++;
                budget.cells++;
                int x = cell & 15;
                int y = (cell >> 8) & 15;
                int z = (cell >> 4) & 15;
                BlockState state = section.getBlockState(x, y, z);
                if (banned.test(state)) {
                    if (redirect(level, new BlockPos(
                        scan.pos.getMinBlockX() + x,
                        sectionY * 16 + y,
                        scan.pos.getMinBlockZ() + z))) {
                        budget.redirects++;
                    }
                }
            }
            scan.sectionIndex++;
            scan.cellIndex = 0;
        }
        return true;
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

    private static void enqueueFirst(ResourceKey<Level> dimension, long key) {
        Set<Long> queued = QUEUED.computeIfAbsent(dimension, ignored -> new HashSet<>());
        if (queued.add(key)) {
            SCAN_QUEUE.computeIfAbsent(dimension, ignored -> new ArrayDeque<>())
                .addFirst(new ChunkScan(key));
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
