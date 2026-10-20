package com.nins77.bandream.enforce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import com.nins77.bandream.config.BanRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.AttachCapabilitiesEvent;

public final class BlockBanSupport {
    private static final Set<BlockEntity> LOADED_BLOCK_ENTITIES = Collections.synchronizedSet(
        Collections.newSetFromMap(new WeakHashMap<>()));

    private BlockBanSupport() {
    }

    public static boolean blocksItemIo(BlockEntity blockEntity) {
        if (!BanRules.hasBlockRules() || blockEntity == null || blockEntity.getLevel() == null
            || blockEntity.getLevel().isClientSide() || blockEntity.isRemoved()) {
            return false;
        }
        if (!BanRules.isBlockBanned(blockEntity.getBlockState().getBlock(),
            blockEntity.getLevel().dimension().location())) {
            return false;
        }
        LOADED_BLOCK_ENTITIES.add(blockEntity);
        return true;
    }

    public static boolean blocksItemIoAt(Level level, BlockPos pos) {
        if (!BanRules.hasBlockRules() || level.isClientSide()) {
            return false;
        }
        String id = BanRules.blockId(level.getBlockState(pos).getBlock());
        return BanRules.isBlockBanned(id, level.dimension().location());
    }

    public static boolean shouldSkipTick(Level level, BlockPos pos) {
        return !level.isClientSide()
            && BanRules.hasBlockRules()
            && BanRules.isBlockBanned(level.getBlockState(pos).getBlock(),
                level.dimension().location());
    }

    public static void trackCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
        BlockEntity blockEntity = event.getObject();
        if (blockEntity.getLevel() == null || !blockEntity.getLevel().isClientSide()) {
            LOADED_BLOCK_ENTITIES.add(blockEntity);
        }
    }

    public static void refreshItemCapabilities() {
        ArrayList<BlockEntity> loaded;
        synchronized (LOADED_BLOCK_ENTITIES) {
            loaded = new ArrayList<>(LOADED_BLOCK_ENTITIES);
        }
        for (BlockEntity blockEntity : loaded) {
            if (blockEntity.isRemoved() || blockEntity.getLevel() == null || blockEntity.getLevel().isClientSide()) {
                LOADED_BLOCK_ENTITIES.remove(blockEntity);
                continue;
            }
            try {
                blockEntity.invalidateCaps();
                blockEntity.reviveCaps();
            } catch (RuntimeException ignored) {
                LOADED_BLOCK_ENTITIES.remove(blockEntity);
            }
        }
    }

}
