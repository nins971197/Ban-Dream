package com.nins77.bandream.item;

import java.util.UUID;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class BannedItemMarkerItem extends Item {
    private static final String SNAPSHOT_TAG = "SnapshotId";

    public BannedItemMarkerItem(Properties properties) {
        super(properties);
    }

    public static ItemStack withSnapshot(UUID snapshotId) {
        ItemStack stack = new ItemStack(BanItems.BAN_999.get());
        stack.getOrCreateTag().putUUID(SNAPSHOT_TAG, snapshotId);
        return stack;
    }

    public static UUID snapshotId(ItemStack stack) {
        return stack.hasTag() && stack.getTag().hasUUID(SNAPSHOT_TAG)
            ? stack.getTag().getUUID(SNAPSHOT_TAG)
            : null;
    }
}
