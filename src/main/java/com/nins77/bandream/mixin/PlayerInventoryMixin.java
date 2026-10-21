package com.nins77.bandream.mixin;

import com.nins77.bandream.item.ItemRedirectService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public abstract class PlayerInventoryMixin {
    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void bandream$redirectAdd(
        int slot,
        ItemStack stack,
        CallbackInfoReturnable<Boolean> callback
    ) {
        Inventory inventory = (Inventory) (Object) this;
        if (!(inventory.player instanceof net.minecraft.server.level.ServerPlayer player)
            || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack marker = ItemRedirectService.redirect(stack, level);
        if (marker != null) {
            if (inventory.add(slot, marker)) {
                stack.setCount(0);
                callback.setReturnValue(true);
            } else {
                ItemRedirectService.discardSnapshot(marker, level);
            }
        }
    }

    @Inject(method = "setItem(ILnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"), cancellable = true)
    private void bandream$redirectSet(
        int slot,
        ItemStack stack,
        CallbackInfo callback
    ) {
        Inventory inventory = (Inventory) (Object) this;
        if (!(inventory.player instanceof net.minecraft.server.level.ServerPlayer player)
            || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack marker = ItemRedirectService.redirect(stack, level);
        if (marker != null) {
            stack.setCount(0);
            inventory.setItem(slot, marker);
            callback.cancel();
        }
    }
}
