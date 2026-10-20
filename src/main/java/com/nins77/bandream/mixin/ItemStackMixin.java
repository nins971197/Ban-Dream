package com.nins77.bandream.mixin;

import com.nins77.bandream.enforce.BanEnforcement;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void bandream$blockUse(
        Level level,
        Player player,
        InteractionHand hand,
        CallbackInfoReturnable<InteractionResultHolder<ItemStack>> callback
    ) {
        ItemStack self = (ItemStack) (Object) this;
        if (BanEnforcement.isItemBlocked(self, level, player)) {
            callback.setReturnValue(new InteractionResultHolder<>(InteractionResult.FAIL, self));
        }
    }

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void bandream$blockUseOn(
        UseOnContext context,
        CallbackInfoReturnable<InteractionResult> callback
    ) {
        ItemStack self = (ItemStack) (Object) this;
        if (BanEnforcement.isItemBlocked(self, context.getLevel(), context.getPlayer())) {
            callback.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "inventoryTick", at = @At("HEAD"), cancellable = true)
    private void bandream$blockInventoryTick(
        Level level,
        Entity entity,
        int slot,
        boolean selected,
        CallbackInfo callback
    ) {
        ItemStack self = (ItemStack) (Object) this;
        if (BanEnforcement.isItemBlocked(self, level, entity instanceof Player player ? player : null)) {
            callback.cancel();
        }
    }

    @Inject(method = "onUseTick", at = @At("HEAD"), cancellable = true)
    private void bandream$blockUseTick(
        Level level,
        LivingEntity entity,
        int remainingUseDuration,
        CallbackInfo callback
    ) {
        ItemStack self = (ItemStack) (Object) this;
        if (BanEnforcement.isItemBlocked(self, level, entity instanceof Player player ? player : null)) {
            callback.cancel();
        }
    }

    @Inject(method = "releaseUsing", at = @At("HEAD"), cancellable = true)
    private void bandream$blockReleaseUsing(
        Level level,
        LivingEntity entity,
        int remainingUseDuration,
        CallbackInfo callback
    ) {
        ItemStack self = (ItemStack) (Object) this;
        if (BanEnforcement.isItemBlocked(self, level, entity instanceof Player player ? player : null)) {
            callback.cancel();
        }
    }

    @Inject(method = "finishUsingItem", at = @At("HEAD"), cancellable = true)
    private void bandream$blockFinishUsing(
        Level level,
        LivingEntity entity,
        CallbackInfoReturnable<ItemStack> callback
    ) {
        ItemStack self = (ItemStack) (Object) this;
        if (BanEnforcement.isItemBlocked(self, level, entity instanceof Player player ? player : null)) {
            callback.setReturnValue(self);
        }
    }

    @Inject(method = "interactLivingEntity", at = @At("HEAD"), cancellable = true)
    private void bandream$blockInteractEntity(
        Player player,
        LivingEntity target,
        InteractionHand hand,
        CallbackInfoReturnable<InteractionResult> callback
    ) {
        ItemStack self = (ItemStack) (Object) this;
        if (BanEnforcement.isItemBlocked(self, player.level(), player)) {
            callback.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "mineBlock", at = @At("HEAD"), cancellable = true)
    private void bandream$blockMining(
        Level level,
        net.minecraft.world.level.block.state.BlockState state,
        net.minecraft.core.BlockPos pos,
        Player player,
        CallbackInfo callback
    ) {
        ItemStack self = (ItemStack) (Object) this;
        if (BanEnforcement.isItemBlocked(self, level, player)) {
            callback.cancel();
        }
    }

    @Inject(method = "hurtEnemy", at = @At("HEAD"), cancellable = true)
    private void bandream$blockHurtEnemy(
        LivingEntity target,
        Player player,
        CallbackInfo callback
    ) {
        ItemStack self = (ItemStack) (Object) this;
        if (BanEnforcement.isItemBlocked(self, player.level(), player)) {
            callback.cancel();
        }
    }
}
