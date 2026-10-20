package com.nins77.bandream.mixin.client;

import com.nins77.bandream.client.BanVisualState;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @Inject(method = "getModel", at = @At("HEAD"), cancellable = true)
    private void bandream$replaceBannedIcon(
        ItemStack stack,
        Level level,
        LivingEntity entity,
        int seed,
        CallbackInfoReturnable<BakedModel> callback
    ) {
        Item replacement = BanVisualState.replacementFor(stack, level);
        if (replacement == null) {
            return;
        }
        BanVisualState.enterOverride();
        try {
            callback.setReturnValue(((ItemRenderer) (Object) this)
                .getModel(new ItemStack(replacement), level, entity, seed));
        } finally {
            BanVisualState.exitOverride();
        }
    }
}
