package com.nins77.bandream.enforce;

import java.util.Optional;

import com.nins77.bandream.config.BanEntry;
import com.nins77.bandream.config.BanRules;
import com.nins77.bandream.config.BanTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class InventoryEnforcement {
    private InventoryEnforcement() {
    }

    public static boolean shouldDiscard(ServerPlayer player, ItemStack stack) {
        if (player == null || stack.isEmpty() || player.level().isClientSide
            || BanRules.isPlayerWhitelisted(player)) {
            return false;
        }
        ResourceLocation dimension = player.level().dimension().location();
        if (BanRules.hasItemRules(BanTrigger.INVENTORY)) {
            Optional<BanEntry> entry = BanRules.findItem(stack, dimension, BanTrigger.INVENTORY);
            if (entry.isPresent()) {
                String name = stack.getHoverName().getString();
                stack.setCount(0);
                BanEnforcement.notify(player, "已移除封禁物品: " + name);
                return true;
            }
        }
        if (BanRules.hasEnchantmentRules()
            && BanEnforcement.stripBannedEnchantments(player, stack, dimension)) {
            BanEnforcement.notify(player, "已移除封禁附魔: " + stack.getHoverName().getString());
        }
        return false;
    }
}
