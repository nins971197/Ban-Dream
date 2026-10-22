package com.nins77.bandream.enforce;

import com.nins77.bandream.block.BakabanService;
import com.nins77.bandream.config.BanRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Block;
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
            || !(event.getLevel() instanceof ServerLevel level)
            || BanRules.isPlayerWhitelisted(player)) {
            return;
        }
        BlockState state = level.getBlockState(event.getPos());
        if (BanRules.isBlockBanned(state.getBlock(), level.dimension().location())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!BanRules.hasBlockRules() || event.getLevel().isClientSide()
            || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (BanRules.isBlockBanned(event.getPlacedBlock().getBlock(),
            level.dimension().location())) {
            if (!BakabanService.redirect(level, event.getPos(), Block.UPDATE_CLIENTS)
                && event.getEntity() instanceof ServerPlayer player) {
                BanEnforcement.notify(player, "方块封存写入失败，请检查磁盘空间和权限");
            }
        }
    }

}
