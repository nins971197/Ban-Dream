package com.nins77.bandream.mixin;

import com.nins77.bandream.config.BanRules;
import com.nins77.bandream.enforce.BanEnforcement;
import com.nins77.bandream.item.BanItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Shadow
    @Final
    private ServerPlayer player;

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void bandream$blockDestroy(BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        if (BanEnforcement.isItemBlocked(player.getMainHandItem(), player.level(), player)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void bandream$blockUseOn(
        ServerPlayer player,
        Level level,
        ItemStack stack,
        InteractionHand hand,
        BlockHitResult hit,
        CallbackInfoReturnable<InteractionResult> callback
    ) {
        if (!stack.is(BanItems.BAN_STICK.get())
            && (BanRules.isBlockBanned(level.getBlockState(hit.getBlockPos()).getBlock(),
                level.dimension().location())
                || BanEnforcement.isItemBlocked(stack, level, player))) {
            callback.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void bandream$blockUseItem(
        ServerPlayer player,
        Level level,
        ItemStack stack,
        InteractionHand hand,
        CallbackInfoReturnable<InteractionResult> callback
    ) {
        if (BanEnforcement.isItemBlocked(stack, level, player)) {
            callback.setReturnValue(InteractionResult.FAIL);
        }
    }
}
