package com.nins77.bandream.block;

import java.util.UUID;

import com.nins77.bandream.config.BanRules;
import com.nins77.bandream.item.BakabanBlockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class BakabanService {
    private static boolean suppressingContainerDrops;
    private static boolean suppressingMarkerDrops;

    private BakabanService() {
    }

    public static boolean redirect(ServerLevel level, BlockPos pos) {
        return redirect(level, pos, Block.UPDATE_ALL);
    }

    public static boolean redirect(ServerLevel level, BlockPos pos, int updateFlags) {
        if (!BanRules.hasBlockRules()) {
            return false;
        }
        BlockState original = level.getBlockState(pos);
        if (original.isAir() || original.is(BanBlocks.BAKABAN.get())
            || !BanRules.isBlockBanned(original.getBlock(), level.dimension().location())) {
            return false;
        }

        CompoundTag blockEntity = null;
        BlockEntity existing = level.getBlockEntity(pos);
        if (existing != null) {
            try {
                blockEntity = existing.saveWithoutMetadata();
            } catch (RuntimeException ignored) {
                return false;
            }
        }

        BakabanSnapshotStore store = BakabanSnapshotStore.get(level);
        UUID snapshotId = store.put(original, blockEntity);
        if (snapshotId == null) {
            return false;
        }
        if (!setBlockSuppressingDrops(
            level, pos, BanBlocks.BAKABAN.get().defaultBlockState(), updateFlags)) {
            store.delete(snapshotId);
            return false;
        }
        if (level.getBlockEntity(pos) instanceof BakabanBlockEntity marker) {
            marker.setSnapshotId(snapshotId);
            return true;
        }
        setBlockSuppressingDrops(level, pos, original, updateFlags);
        store.delete(snapshotId);
        restoreBlockEntity(level, pos, blockEntity);
        return false;
    }

    public static boolean restore(ServerLevel level, BlockPos pos) {
        return restore(level, pos, Block.UPDATE_ALL);
    }

    public static boolean restore(ServerLevel level, BlockPos pos, int updateFlags) {
        if (!(level.getBlockEntity(pos) instanceof BakabanBlockEntity marker) || marker.snapshotId() == null) {
            return false;
        }
        UUID snapshotId = marker.snapshotId();
        BakabanSnapshotStore store = BakabanSnapshotStore.get(level);
        BakabanSnapshotStore.StoredBlock stored = store.get(snapshotId);
        if (stored == null
            || BanRules.isBlockBanned(stored.state().getBlock(), level.dimension().location())) {
            return false;
        }
        if (!setBlockSuppressingDrops(level, pos, stored.state(), updateFlags)) {
            return false;
        }
        if (!restoreBlockEntity(level, pos, stored.blockEntity())) {
            setBlockSuppressingDrops(
                level, pos, BanBlocks.BAKABAN.get().defaultBlockState(), updateFlags);
            if (level.getBlockEntity(pos) instanceof BakabanBlockEntity replacement) {
                replacement.setSnapshotId(snapshotId);
            }
            return false;
        }
        if (!store.delete(snapshotId)) {
            return false;
        }
        return true;
    }

    public static void dropMarker(Level level, BlockPos pos, UUID snapshotId) {
        if (!level.isClientSide() && !suppressingMarkerDrops && snapshotId != null) {
            Block.popResource(level, pos, BakabanBlockItem.withSnapshot(snapshotId));
        }
    }

    public static String originalBlockId(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof BakabanBlockEntity marker) || marker.snapshotId() == null) {
            return "";
        }
        BakabanSnapshotStore.StoredBlock stored =
            BakabanSnapshotStore.get(level).get(marker.snapshotId());
        return stored == null ? "" : BanRules.blockId(stored.state().getBlock());
    }

    public static BlockState originalState(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof BakabanBlockEntity marker) || marker.snapshotId() == null) {
            return null;
        }
        BakabanSnapshotStore.StoredBlock stored =
            BakabanSnapshotStore.get(level).get(marker.snapshotId());
        return stored == null ? null : stored.state();
    }

    public static boolean suppressesContainerDrops(Level level) {
        return suppressingContainerDrops && !level.isClientSide();
    }

    private static boolean setBlockSuppressingDrops(
        ServerLevel level,
        BlockPos pos,
        BlockState state,
        int updateFlags
    ) {
        boolean previous = suppressingContainerDrops;
        boolean previousMarker = suppressingMarkerDrops;
        suppressingContainerDrops = true;
        suppressingMarkerDrops = true;
        try {
            return level.setBlock(pos, state, updateFlags);
        } finally {
            suppressingContainerDrops = previous;
            suppressingMarkerDrops = previousMarker;
        }
    }

    private static boolean restoreBlockEntity(ServerLevel level, BlockPos pos, CompoundTag data) {
        if (data == null) {
            return true;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return false;
        }
        try {
            CompoundTag content = data.copy();
            content.remove("id");
            content.remove("x");
            content.remove("y");
            content.remove("z");
            blockEntity.load(content);
            blockEntity.setChanged();
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
