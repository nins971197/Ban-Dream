package com.nins77.bandream.enforce;

import com.nins77.bandream.action.PendingActionManager;
import com.nins77.bandream.config.BanRules;
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
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BanEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(BanEvents.class);

    private BanEvents() {
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        try {
            BanRules.load();
        } catch (Exception exception) {
            LOGGER.error("Failed to load Ban Dream rules from {}", BanRules.configPath(), exception);
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
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.getEntity() instanceof Player) {
            return;
        }
        boolean entityRules = BanRules.hasEntityRules();
        boolean effectRules = BanRules.hasEffectRules();
        if (!entityRules && !effectRules) {
            return;
        }
        Entity entity = event.getEntity();
        if (entityRules && BanRules.isEntityBanned(
            entity.getType(), event.getLevel().dimension().location())) {
            event.setCanceled(true);
        } else if (effectRules && entity instanceof LivingEntity living) {
            BanEnforcement.enforceLivingEffects(living);
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
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !BanRules.isPlayerWhitelisted(player)) {
            BanEnforcement.escapeBannedDimension(player);
            BanEnforcement.enforcePlayer(player, true);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !BanRules.isPlayerWhitelisted(player)) {
            BanEnforcement.enforcePlayer(player, true);
        }
    }

    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer player && !BanRules.isPlayerWhitelisted(player)) {
            BanEnforcement.enforcePlayer(player, true);
        }
    }

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !BanRules.isPlayerWhitelisted(player)) {
            BanEnforcement.enforcePlayer(player, true);
        }
    }

    @SubscribeEvent
    public static void onSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !BanRules.isPlayerWhitelisted(player)) {
            BanEnforcement.enforcePlayer(player, true);
        }
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !BanRules.isPlayerWhitelisted(player)) {
            BanEnforcement.enforcePlayer(player, true);
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
