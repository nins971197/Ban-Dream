package com.nins77.bandream.mixin;

import com.nins77.bandream.enforce.BlockBanSupport;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityProvider;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CapabilityProvider.class)
public abstract class CapabilityProviderMixin {
    @Inject(method = "getCapability", at = @At("HEAD"), cancellable = true, remap = false)
    private void bandream$blockItemIo(
        Capability<?> capability,
        Direction side,
        CallbackInfoReturnable<LazyOptional<?>> callback
    ) {
        if ((Object) this instanceof BlockEntity blockEntity
            && capability == ForgeCapabilities.ITEM_HANDLER
            && BlockBanSupport.blocksItemIo(blockEntity)) {
            callback.setReturnValue(LazyOptional.empty());
        }
    }
}
