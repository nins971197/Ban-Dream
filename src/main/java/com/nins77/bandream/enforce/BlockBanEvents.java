package com.nins77.bandream.enforce;

import java.util.Optional;

import com.nins77.bandream.config.BanRules;
import com.nins77.bandream.config.BlockBanMode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class BlockBanEvents {
    private BlockBanEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)
            || BanRules.isPlayerWhitelisted(player)) {
            return;
        }
        BlockState state = event.getLevel().getBlockState(event.getPos());
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (id != null && blockMode(id, player).isPresent()) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)
            || BanRules.isPlayerWhitelisted(player)) {
            return;
        }
        BlockState state = event.getLevel().getBlockState(event.getPos());
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (id != null && blockMode(id, player).orElse(null) == BlockBanMode.FULL) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || BanRules.isPlayerWhitelisted(player)) {
            return;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(event.getState().getBlock());
        if (id != null && blockMode(id, player).orElse(null) == BlockBanMode.FULL) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof Player player && BanRules.isPlayerWhitelisted(player)) {
            return;
        }
        Entity entity = event.getEntity();
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(event.getPlacedBlock().getBlock());
        if (entity != null && id != null
            && blockMode(id, entity).orElse(null) == BlockBanMode.FULL) {
            event.setCanceled(true);
        }
    }

    private static Optional<BlockBanMode> blockMode(ResourceLocation id, Entity entity) {
        return BanRules.blockBanMode(id, entity.level().dimension().location());
    }
}
