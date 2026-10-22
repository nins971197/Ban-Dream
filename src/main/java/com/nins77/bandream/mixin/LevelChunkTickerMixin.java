package com.nins77.bandream.mixin;

import com.nins77.bandream.enforce.BlockBanSupport;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelChunk.class)
public abstract class LevelChunkTickerMixin {
    @Inject(method = "updateBlockEntityTicker", at = @At("HEAD"), cancellable = true)
    private void bandream$skipBannedBlockEntity(BlockEntity blockEntity, CallbackInfo callback) {
        if (blockEntity.getLevel() != null
            && BlockBanSupport.shouldSkipTick(blockEntity.getLevel(), blockEntity.getBlockPos())) {
            callback.cancel();
        }
    }
}
