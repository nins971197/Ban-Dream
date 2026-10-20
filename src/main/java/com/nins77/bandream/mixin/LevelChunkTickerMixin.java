package com.nins77.bandream.mixin;

import com.nins77.bandream.enforce.BlockBanSupport;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$RebindableTickingBlockEntityWrapper")
public abstract class LevelChunkTickerMixin {
    @Shadow
    @Final
    private LevelChunk this$0;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void bandream$skipBannedBlockEntity(CallbackInfo callback) {
        TickingBlockEntity ticker = (TickingBlockEntity) (Object) this;
        if (BlockBanSupport.shouldSkipTick(this$0.getLevel(), ticker.getPos())) {
            callback.cancel();
        }
    }
}
