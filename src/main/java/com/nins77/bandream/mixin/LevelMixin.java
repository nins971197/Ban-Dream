package com.nins77.bandream.mixin;

import com.nins77.bandream.enforce.BlockBanSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Level.class)
public abstract class LevelMixin {
    @Redirect(
        method = "tickBlockEntities",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/TickingBlockEntity;tick()V")
    )
    private void bandream$skipBannedBlockEntity(TickingBlockEntity ticker) {
        Level level = (Level) (Object) this;
        if (!BlockBanSupport.shouldSkipTick(level, ticker.getPos())) {
            ticker.tick();
        }
    }

    @Inject(
        method = "neighborChanged(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/core/BlockPos;Z)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void bandream$skipBannedNeighborUpdate(
        BlockState state,
        BlockPos pos,
        Block block,
        BlockPos fromPos,
        boolean movedByPiston,
        CallbackInfo callback
    ) {
        if (BlockBanSupport.shouldSkipTick((Level) (Object) this, pos)) {
            callback.cancel();
        }
    }
}
