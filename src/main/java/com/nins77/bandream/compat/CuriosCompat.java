package com.nins77.bandream.compat;

import com.nins77.bandream.config.BanRules;
import com.nins77.bandream.enforce.BanEnforcement;
import com.nins77.bandream.item.ItemRedirectService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public final class CuriosCompat {
    private CuriosCompat() {
    }

    public static boolean enforce(
        ServerPlayer player,
        ResourceLocation dimension,
        boolean scanItems,
        boolean scanEnchantments
    ) {
        ICuriosItemHandler handler = CuriosApi.getCuriosInventory(player).orElse(null);
        return enforce(player, handler, dimension, scanItems, scanEnchantments);
    }

    public static boolean enforce(
        ServerPlayer player,
        ICuriosItemHandler handler,
        ResourceLocation dimension,
        boolean scanItems,
        boolean scanEnchantments
    ) {
        if (handler == null) {
            return false;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        boolean bypassBans = BanRules.isPlayerWhitelisted(player);
        int[] removed = {0};
        boolean changed = false;
        for (ICurioStacksHandler stacksHandler : handler.getCurios().values()) {
            boolean stackChanged = clean(
                stacksHandler.getStacks(), player, level, dimension, scanItems, scanEnchantments,
                bypassBans, removed);
            changed |= stackChanged;
            if (stacksHandler.hasCosmetic()) {
                IDynamicStackHandler cosmetic = stacksHandler.getCosmeticStacks();
                if (cosmetic != null) {
                    stackChanged |= clean(
                        cosmetic, player, level, dimension, scanItems, scanEnchantments,
                        bypassBans, removed);
                    changed |= stackChanged;
                }
            }
            if (stackChanged) {
                stacksHandler.update();
            }
        }
        if (removed[0] > 0) {
            BanEnforcement.notify(player, "已封存饰品栏封禁物品: " + removed[0] + " 件");
        }
        return changed;
    }

    private static boolean clean(
        IDynamicStackHandler stacks,
        ServerPlayer player,
        ServerLevel level,
        ResourceLocation dimension,
        boolean scanItems,
        boolean scanEnchantments,
        boolean bypassBans,
        int[] removed
    ) {
        boolean changed = false;
        for (int slot = 0; slot < stacks.getSlots(); slot++) {
            ItemStack stack = stacks.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack restored = ItemRedirectService.restore(stack, level, dimension, bypassBans);
            if (restored != null) {
                stacks.setStackInSlot(slot, restored);
                changed = true;
                stack = restored;
            }
            if (scanItems && !bypassBans) {
                ItemStack marker = ItemRedirectService.redirect(stack, level);
                if (marker != null) {
                    stacks.setStackInSlot(slot, marker);
                    removed[0]++;
                    changed = true;
                    continue;
                }
            }
            if (scanEnchantments && BanEnforcement.stripBannedEnchantments(player, stack, dimension)) {
                stacks.setStackInSlot(slot, stack);
                changed = true;
            }
        }
        return changed;
    }
}
