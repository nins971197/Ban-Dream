package com.nins77.bandream.mixin;

import com.nins77.bandream.block.BakabanService;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.world.Containers.class)
public abstract class ContainersMixin {
    @Inject(
        method = "dropContents(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/Container;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void bandream$suppressRedirectDrops(
        Level level,
        net.minecraft.core.BlockPos pos,
        Container container,
        CallbackInfo callback
    ) {
        if (BakabanService.suppressesContainerDrops(level)) {
            callback.cancel();
        }
    }
}
