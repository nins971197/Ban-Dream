package com.nins77.bandream.item;

import java.util.UUID;

import com.nins77.bandream.block.BakabanBlockEntity;
import com.nins77.bandream.block.BakabanScanner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ChunkPos;

public final class BakabanBlockItem extends BlockItem {
    private static final String SNAPSHOT_TAG = "SnapshotId";

    public BakabanBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    public static ItemStack withSnapshot(UUID snapshotId) {
        ItemStack stack = new ItemStack(BanItems.BAKABAN.get());
        stack.getOrCreateTag().putUUID(SNAPSHOT_TAG, snapshotId);
        return stack;
    }

    public static UUID snapshotId(ItemStack stack) {
        return stack.hasTag() && stack.getTag().hasUUID(SNAPSHOT_TAG)
            ? stack.getTag().getUUID(SNAPSHOT_TAG)
            : null;
    }

    @Override
    protected boolean updateCustomBlockEntityTag(
        BlockPos pos,
        Level level,
        Player player,
        ItemStack stack,
        BlockState state
    ) {
        boolean changed = super.updateCustomBlockEntityTag(pos, level, player, stack, state);
        UUID snapshotId = snapshotId(stack);
        if (snapshotId != null && level.getBlockEntity(pos) instanceof BakabanBlockEntity marker) {
            marker.setSnapshotId(snapshotId);
            if (level instanceof ServerLevel serverLevel) {
                BakabanScanner.onChunkLoad(serverLevel, new ChunkPos(pos));
            }
            return true;
        }
        return changed;
    }
}
