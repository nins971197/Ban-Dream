package com.nins77.bandream.item;

import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class BannedItemMarkerItem extends Item {
    private static final String SNAPSHOT_TAG = "SnapshotId";
    private static final String MISSING_TAG = "MissingSnapshot";

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

    public static ItemStack withMissingSnapshot(UUID snapshotId) {
        ItemStack stack = withSnapshot(snapshotId);
        stack.getTag().putBoolean(MISSING_TAG, true);
        stack.setHoverName(Component.translatable("item.bandream.999ban.missing"));
        return stack;
    }

    public static boolean hasMissingSnapshot(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(MISSING_TAG);
    }
}
