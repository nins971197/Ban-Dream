package com.nins77.bandream.block;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class BakabanBlockEntity extends BlockEntity {
    private UUID snapshotId;

    public BakabanBlockEntity(BlockPos pos, BlockState state) {
        super(BanBlocks.BAKABAN_ENTITY.get(), pos, state);
    }

    public UUID snapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(UUID snapshotId) {
        this.snapshotId = snapshotId;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (snapshotId != null) {
            tag.putUUID("snapshot", snapshotId);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        snapshotId = tag.hasUUID("snapshot") ? tag.getUUID("snapshot") : null;
    }
}
