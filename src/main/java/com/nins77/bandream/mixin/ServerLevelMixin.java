package com.nins77.bandream.mixin;

import com.nins77.bandream.enforce.BlockBanSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "tickBlock", at = @At("HEAD"), cancellable = true)
    private void bandream$skipScheduledBlockTick(BlockPos pos, Block block, CallbackInfo callback) {
        if (BlockBanSupport.shouldSkipTick((ServerLevel) (Object) this, pos)) {
            callback.cancel();
        }
    }

    @Inject(method = "tickFluid", at = @At("HEAD"), cancellable = true)
    private void bandream$skipScheduledFluidTick(BlockPos pos, Fluid fluid, CallbackInfo callback) {
        if (BlockBanSupport.shouldSkipTick((ServerLevel) (Object) this, pos)) {
            callback.cancel();
        }
    }

    @Redirect(
        method = "tickChunk",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;randomTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"
        )
    )
    private void bandream$skipBannedRandomTick(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random
    ) {
        if (!BlockBanSupport.shouldSkipTick(level, pos)) {
            state.randomTick(level, pos, random);
        }
    }
}
