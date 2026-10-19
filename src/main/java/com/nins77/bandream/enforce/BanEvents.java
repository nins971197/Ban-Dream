package com.nins77.bandream.enforce;

import com.nins77.bandream.action.PendingActionManager;
import com.nins77.bandream.config.BanRules;
import com.nins77.bandream.config.BanTrigger;
import com.nins77.bandream.network.BanNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class BanEvents {
    private BanEvents() {
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        try {
            BanRules.load();
        } catch (Exception ignored) {
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (BanRules.isPlayerWhitelisted(player)) {
            return;
        }
        ResourceLocation dimension = player.level().dimension().location();
        if (BanRules.isDimensionBanned(dimension)) {
            if (player.tickCount % 10 == 0) {
                BanEnforcement.escapeBannedDimension(player);
            }
        }
        boolean fullCheck = player.tickCount % 10 == 0;
        boolean inventoryCheck = BanRules.hasItemRules(BanTrigger.INVENTORY)
            && player.tickCount % 5 == 0;
        if (inventoryCheck
            || (fullCheck && (BanRules.hasItemRules(BanTrigger.USE) || BanRules.hasEnchantmentRules()))) {
            BanEnforcement.enforcePlayer(player, fullCheck);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.level().isClientSide && BanRules.hasEffectRules() && entity.tickCount % 20 == 0) {
            BanEnforcement.enforceLivingEffects(entity);
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!BanRules.hasEntityRules() || event.getLevel().isClientSide()
            || event.getEntity() instanceof Player) {
            return;
        }
        Entity entity = event.getEntity();
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (id != null && BanRules.isEntityBanned(id, event.getLevel().dimension().location())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onTravelToDimension(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof Player player && BanRules.isPlayerWhitelisted(player)) {
            return;
        }
        ResourceLocation id = event.getDimension().location();
        if (!BanRules.isDimensionBanned(id)) {
            return;
        }
        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer player) {
            player.displayClientMessage(
                Component.literal("维度 " + id + " 已被封禁").withStyle(ChatFormatting.RED), true);
        }
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (!BanRules.hasEffectRules()) {
            return;
        }
        LivingEntity entity = event.getEntity();
        MobEffectInstance effect = event.getEffectInstance();
        if (entity instanceof Player player && BanRules.isPlayerWhitelisted(player)) {
            return;
        }
        if (!entity.level().isClientSide
            && BanRules.isEffectBanned(effect.getEffect(), entity.level().dimension().location())) {
            entity.removeEffect(effect.getEffect());
        }
    }

    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (BanEnforcement.enforceUse(player, event.getItemStack())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) {
            return;
        }
        if (BanEnforcement.enforceUse(player, player.getMainHandItem())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) {
            return;
        }
        if (BanEnforcement.enforceUse(player, event.getItem())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BanNetwork.sync(player);
            if (BanRules.isPlayerWhitelisted(player)) {
                return;
            }
            BanEnforcement.escapeBannedDimension(player);
            BanEnforcement.enforcePlayer(player, true);
        }
    }

    @SubscribeEvent
    public static void onItemPickup(PlayerEvent.ItemPickupEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && BanRules.hasItemRules(BanTrigger.INVENTORY)) {
            BanEnforcement.enforcePlayer(player, false);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PendingActionManager.clear(player);
            BanStickEvents.clearPlayer(player);
            BanEnforcement.clearPlayerState(player);
        }
    }

}
