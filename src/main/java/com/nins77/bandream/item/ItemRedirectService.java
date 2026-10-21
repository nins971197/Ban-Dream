package com.nins77.bandream.item;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.UUID;

import com.nins77.bandream.config.BanRules;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class ItemRedirectService {
    private ItemRedirectService() {
    }

    public static ItemStack redirect(ItemStack source, ServerLevel level) {
        if (source.isEmpty() || source.is(BanItems.BAN_999.get())
            || !BanRules.isItemBanned(source, level.dimension().location())) {
            return null;
        }
        ItemSnapshotStore store = ItemSnapshotStore.get(level);
        UUID id = store.put(source);
        return BannedItemMarkerItem.withSnapshot(id);
    }

    public static void discardSnapshot(ItemStack marker, ServerLevel level) {
        UUID id = BannedItemMarkerItem.snapshotId(marker);
        if (id != null) {
            ItemSnapshotStore.get(level).remove(id);
        }
    }

    public static boolean restorePlayer(ServerPlayer player, boolean bypassBans) {
        ServerLevel level = (ServerLevel) player.level();
        ItemSnapshotStore store = ItemSnapshotStore.get(level);
        ResourceLocation dimension = player.level().dimension().location();
        boolean changed = restoreList(player.getInventory().items, store, dimension, bypassBans);
        changed |= restoreList(player.getInventory().armor, store, dimension, bypassBans);
        changed |= restoreList(player.getInventory().offhand, store, dimension, bypassBans);

        ItemStack carried = player.containerMenu.getCarried();
        ItemStack restored = restore(carried, store, dimension, bypassBans);
        if (restored != null) {
            player.containerMenu.setCarried(restored);
            player.containerMenu.broadcastChanges();
            changed = true;
        }
        if (changed) {
            player.getInventory().setChanged();
        }
        return changed;
    }

    public static boolean isMarker(ItemStack stack) {
        return !stack.isEmpty()
            && stack.is(BanItems.BAN_999.get())
            && BannedItemMarkerItem.snapshotId(stack) != null;
    }

    public static boolean redirectPlayer(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        boolean changed = redirectList(player.getInventory().items, level);
        changed |= redirectList(player.getInventory().armor, level);
        changed |= redirectList(player.getInventory().offhand, level);

        ItemStack carried = player.containerMenu.getCarried();
        ItemStack marker = redirect(carried, level);
        if (marker != null) {
            player.containerMenu.setCarried(marker);
            player.containerMenu.broadcastChanges();
            changed = true;
        }
        if (changed) {
            player.getInventory().setChanged();
        }
        return changed;
    }

    public static boolean handleItemEntity(ItemEntity itemEntity, ServerLevel level, boolean redirectBans) {
        ItemStack stack = itemEntity.getItem();
        if (isMarker(stack)) {
            itemEntity.setUnlimitedLifetime();
            itemEntity.setInvulnerable(true);
            ItemStack restored = restore(stack, level, level.dimension().location(), false);
            if (restored != null) {
                itemEntity.setItem(restored);
                return true;
            }
            return false;
        }
        if (!redirectBans) {
            return false;
        }
        ItemStack marker = redirect(stack, level);
        if (marker == null) {
            return false;
        }
        itemEntity.setItem(marker);
        itemEntity.setUnlimitedLifetime();
        itemEntity.setInvulnerable(true);
        return true;
    }

    public static ItemStack restore(ItemStack stack, ServerLevel level, ResourceLocation dimension,
        boolean bypassBans) {
        return restore(stack, ItemSnapshotStore.get(level), dimension, bypassBans);
    }

    public static boolean enforceMenu(
        ServerPlayer player,
        AbstractContainerMenu menu,
        boolean redirectBans
    ) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        boolean bypassBans = BanRules.isPlayerWhitelisted(player);
        ResourceLocation dimension = level.dimension().location();
        ItemSnapshotStore store = ItemSnapshotStore.get(level);
        Set<Container> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        boolean changed = false;
        for (Slot slot : menu.slots) {
            if (!slot.allowModification(player) || !visited.add(slot.container)) {
                continue;
            }
            Container container = slot.container;
            boolean containerChanged = false;
            for (int index = 0; index < container.getContainerSize(); index++) {
                ItemStack stack = container.getItem(index);
                if (stack.isEmpty()) {
                    continue;
                }
                ItemStack restored = restore(stack, store, dimension, bypassBans);
                if (restored != null) {
                    container.setItem(index, restored);
                    stack = restored;
                    containerChanged = true;
                }
                if (redirectBans && !bypassBans) {
                    ItemStack marker = redirect(stack, level);
                    if (marker != null) {
                        container.setItem(index, marker);
                        containerChanged = true;
                    }
                }
            }
            if (containerChanged) {
                container.setChanged();
                changed = true;
            }
        }
        if (changed) {
            menu.broadcastChanges();
        }
        return changed;
    }

    private static boolean restoreList(
        NonNullList<ItemStack> stacks,
        ItemSnapshotStore store,
        ResourceLocation dimension,
        boolean bypassBans
    ) {
        boolean changed = false;
        for (int index = 0; index < stacks.size(); index++) {
            ItemStack restored = restore(stacks.get(index), store, dimension, bypassBans);
            if (restored != null) {
                stacks.set(index, restored);
                changed = true;
            }
        }
        return changed;
    }

    private static boolean redirectList(NonNullList<ItemStack> stacks, ServerLevel level) {
        boolean changed = false;
        for (int index = 0; index < stacks.size(); index++) {
            ItemStack marker = redirect(stacks.get(index), level);
            if (marker != null) {
                stacks.set(index, marker);
                changed = true;
            }
        }
        return changed;
    }

    private static ItemStack restore(ItemStack stack, ItemSnapshotStore store, ResourceLocation dimension,
        boolean bypassBans) {
        if (!isMarker(stack)) {
            return null;
        }
        UUID id = BannedItemMarkerItem.snapshotId(stack);
        if (!store.contains(id)) {
            return null;
        }
        ItemStack original = store.get(id);
        if (original.isEmpty() || (!bypassBans && BanRules.isItemBanned(original, dimension))) {
            return null;
        }
        ItemStack restored = store.remove(id);
        return restored.isEmpty() ? null : restored;
    }
}
