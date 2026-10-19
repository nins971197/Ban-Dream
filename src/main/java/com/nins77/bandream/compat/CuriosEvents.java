package com.nins77.bandream.compat;

import com.nins77.bandream.config.BanRules;
import com.nins77.bandream.config.BanTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

public final class CuriosEvents {
    private CuriosEvents() {
    }

    @SubscribeEvent
    public static void onCurioChanged(CurioChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
            || player.level().isClientSide
            || BanRules.isPlayerWhitelisted(player)) {
            return;
        }
        boolean scanItems = BanRules.hasItemRules(BanTrigger.INVENTORY)
            || BanRules.hasItemRules(BanTrigger.USE);
        boolean scanEnchantments = BanRules.hasEnchantmentRules();
        if (!scanItems && !scanEnchantments) {
            return;
        }
        ResourceLocation dimension = player.level().dimension().location();
        CuriosCompat.enforce(player, dimension, scanItems, scanEnchantments);
    }
}
