package com.nins77.bandream.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class BakabanBlock extends BaseEntityBlock {
    public BakabanBlock() {
        super(Properties.of().strength(1.2F).sound(SoundType.STONE));
    }

    @Override
    public InteractionResult use(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit
    ) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        return BakabanManager.restore(level, pos, player)
            ? InteractionResult.SUCCESS
            : InteractionResult.PASS;
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide()) {
            BakabanManager.restore(level, pos, player);
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BakabanBlockEntity(pos, state);
    }
}
