package com.nins77.bandream.mixin;

import com.nins77.bandream.enforce.InventoryEnforcement;
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
    private void bandream$rejectBannedAdd(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        Inventory inventory = (Inventory) (Object) this;
        if (inventory.player instanceof net.minecraft.server.level.ServerPlayer player
            && InventoryEnforcement.shouldDiscard(player, stack)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "setItem(ILnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"), cancellable = true)
    private void bandream$rejectBannedSet(int slot, ItemStack stack, CallbackInfo callback) {
        Inventory inventory = (Inventory) (Object) this;
        if (inventory.player instanceof net.minecraft.server.level.ServerPlayer player
            && InventoryEnforcement.shouldDiscard(player, stack)) {
            inventory.setItem(slot, ItemStack.EMPTY);
            callback.cancel();
        }
    }
}
