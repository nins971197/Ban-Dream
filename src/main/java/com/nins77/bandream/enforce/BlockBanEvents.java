package com.nins77.bandream.enforce;

import java.util.Optional;

import com.nins77.bandream.config.BanRules;
import com.nins77.bandream.config.BlockBanMode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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
        if (!BanRules.hasBlockRules() || event.getLevel().isClientSide()
            || !(event.getEntity() instanceof ServerPlayer player)
            || BanRules.isPlayerWhitelisted(player)) {
            return;
        }
        BlockState state = event.getLevel().getBlockState(event.getPos());
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (id != null) {
            Optional<BlockBanMode> mode = blockMode(id, player);
            if (mode.isEmpty()) {
                return;
            }
            if (mode.get() == BlockBanMode.FULL) {
                event.getLevel().removeBlock(event.getPos(), false);
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!BanRules.hasBlockRules() || event.getLevel().isClientSide()
            || !(event.getEntity() instanceof ServerPlayer player)
            || BanRules.isPlayerWhitelisted(player)) {
            return;
        }
        BlockState state = event.getLevel().getBlockState(event.getPos());
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (id != null && blockMode(id, player).orElse(null) == BlockBanMode.FULL) {
            event.getLevel().removeBlock(event.getPos(), false);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!BanRules.hasBlockRules() || !(event.getPlayer() instanceof ServerPlayer player)
            || BanRules.isPlayerWhitelisted(player)) {
            return;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(event.getState().getBlock());
        if (id != null && blockMode(id, player).orElse(null) == BlockBanMode.FULL) {
            event.getLevel().removeBlock(event.getPos(), false);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!BanRules.hasBlockRules() || event.getLevel().isClientSide()
            || !(event.getLevel() instanceof Level level)) {
            return;
        }
        if (event.getEntity() instanceof Player player && BanRules.isPlayerWhitelisted(player)) {
            return;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(event.getPlacedBlock().getBlock());
        if (id != null && BanRules.blockBanMode(id, level.dimension().location()).orElse(null) == BlockBanMode.FULL) {
            event.setCanceled(true);
        }
    }

    private static Optional<BlockBanMode> blockMode(ResourceLocation id, ServerPlayer player) {
        return BanRules.blockBanMode(id, player.level().dimension().location());
    }
}
