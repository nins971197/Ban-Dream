package com.nins77.bandream.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class BakabanBlockEntity extends BlockEntity {
    public BakabanBlockEntity(BlockPos pos, BlockState state) {
        super(BanBlocks.BAKABAN_ENTITY.get(), pos, state);
    }
}
