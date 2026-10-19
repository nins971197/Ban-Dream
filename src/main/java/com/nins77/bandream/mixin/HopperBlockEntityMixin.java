package com.nins77.bandream.mixin;

import com.nins77.bandream.enforce.BlockBanSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {
    @Inject(method = "pushItemsTick", at = @At("HEAD"), cancellable = true)
    private static void bandream$blockHopperTick(
        Level level,
        BlockPos pos,
        BlockState state,
        HopperBlockEntity hopper,
        CallbackInfo callback
    ) {
        if (blocked(level, pos) || blocked(level, pos.above()) || blocked(level, pos.below())) {
            callback.cancel();
        }
    }

    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private static void bandream$blockHopperPickup(
        Level level,
        BlockPos pos,
        BlockState state,
        Entity entity,
        HopperBlockEntity hopper,
        CallbackInfo callback
    ) {
        if (blocked(level, pos)) {
            callback.cancel();
        }
    }

    @Inject(method = "suckInItems", at = @At("HEAD"), cancellable = true)
    private static void bandream$blockHopperSuck(
        Level level,
        Hopper hopper,
        CallbackInfoReturnable<Boolean> callback
    ) {
        if (hopper instanceof HopperBlockEntity hopperBlock
            && (blocked(level, hopperBlock.getBlockPos())
                || blocked(level, hopperBlock.getBlockPos().above()))) {
            callback.setReturnValue(false);
        }
    }

    @Inject(
        method = "getContainerAt(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/Container;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void bandream$blockContainerAtPos(
        Level level,
        BlockPos pos,
        CallbackInfoReturnable<Container> callback
    ) {
        if (blocked(level, pos)) {
            callback.setReturnValue(null);
        }
    }

    @Inject(
        method = "getContainerAt(Lnet/minecraft/world/level/Level;DDD)Lnet/minecraft/world/Container;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void bandream$blockContainerAtPoint(
        Level level,
        double x,
        double y,
        double z,
        CallbackInfoReturnable<Container> callback
    ) {
        if (blocked(level, BlockPos.containing(x, y, z))) {
            callback.setReturnValue(null);
        }
    }

    private static boolean blocked(Level level, BlockPos pos) {
        return BlockBanSupport.blocksItemIoAt(level, pos);
    }
}
