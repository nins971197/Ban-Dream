package com.nins77.bandream.block;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class BakabanEvents {
    private BakabanEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)
            || event.getEntity().level().isClientSide()
            || !level.getBlockState(event.getPos()).is(BanBlocks.BAKABAN.get())
            || !BakabanManager.hasSnapshot(level, event.getPos())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        if (event.getEntity() instanceof ServerPlayer player) {
            BakabanManager.restore(level, event.getPos(), player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
            || !(event.getLevel() instanceof ServerLevel level)
            || event.getEntity().level().isClientSide()
            || !level.getBlockState(event.getPos()).is(BanBlocks.BAKABAN.get())
            || !BakabanManager.hasSnapshot(level, event.getPos())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getEntity() instanceof ServerPlayer player) {
            BakabanManager.restore(level, event.getPos(), player);
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ChunkAccess chunk = event.getChunk();
            BakabanManager.onChunkLoad(level, chunk.getPos());
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ChunkAccess chunk = event.getChunk();
            BakabanManager.onChunkUnload(level, chunk.getPos());
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            BakabanManager.tick(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        BakabanManager.clear();
    }
}
