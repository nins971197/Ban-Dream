package com.nins77.bandream.mixin;

import com.nins77.bandream.enforce.BlockBanSupport;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BaseContainerBlockEntity.class)
public abstract class BaseContainerBlockEntityMixin {
    @Inject(method = "getCapability", at = @At("HEAD"), cancellable = true, remap = false)
    private void bandream$blockItemIo(
        Capability<?> capability,
        Direction side,
        CallbackInfoReturnable<LazyOptional<?>> callback
    ) {
        BaseContainerBlockEntity self = (BaseContainerBlockEntity) (Object) this;
        if (capability == ForgeCapabilities.ITEM_HANDLER && BlockBanSupport.blocksItemIo(self)) {
            callback.setReturnValue(LazyOptional.empty());
        }
    }
}
