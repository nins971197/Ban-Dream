package com.nins77.bandream.item;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.nins77.bandream.config.BanRules;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class ItemRedirectService {
    private static final int DEFAULT_ITEM_LIFESPAN = 6000;
    private static final ThreadLocal<Boolean> INTERNAL_REDIRECT = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> WRITE_FAILED = ThreadLocal.withInitial(() -> false);
    private static final Map<UUID, Long> LAST_WRITE_WARNING = new ConcurrentHashMap<>();

    private ItemRedirectService() {
    }

    public static ItemStack redirect(ItemStack source, ServerLevel level) {
        if (source.isEmpty() || source.is(BanItems.BAN_999.get())
            || !BanRules.isItemBanned(source, level.dimension().location())) {
            return null;
        }
        ItemSnapshotStore store = ItemSnapshotStore.get(level);
        UUID id = store.put(source);
        if (id == null) {
            WRITE_FAILED.set(true);
            return null;
        }
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
            && !BannedItemMarkerItem.hasMissingSnapshot(stack)
            && BannedItemMarkerItem.snapshotId(stack) != null;
    }

    public static boolean beginInternalRedirect() {
        if (INTERNAL_REDIRECT.get()) {
            return false;
        }
        INTERNAL_REDIRECT.set(true);
        return true;
    }

    public static void endInternalRedirect() {
        INTERNAL_REDIRECT.remove();
    }

    public static void clearWriteFailure() {
        WRITE_FAILED.set(false);
    }

    public static boolean consumeWriteFailure() {
        boolean failed = WRITE_FAILED.get();
        WRITE_FAILED.remove();
        return failed;
    }

    public static boolean redirectPlayer(ServerPlayer player) {
        clearWriteFailure();
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
        if (consumeWriteFailure()) {
            notifyWriteFailure(player);
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
                itemEntity.lifespan = DEFAULT_ITEM_LIFESPAN;
                itemEntity.setInvulnerable(false);
                return true;
            }
            ItemStack missing = missingMarker(stack, level);
            if (missing != null) {
                itemEntity.setItem(missing);
                itemEntity.lifespan = DEFAULT_ITEM_LIFESPAN;
                itemEntity.setInvulnerable(false);
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
        clearWriteFailure();
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
                ItemStack missing = missingMarker(stack, store);
                if (missing != null) {
                    container.setItem(index, missing);
                    containerChanged = true;
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
        if (consumeWriteFailure()) {
            notifyWriteFailure(player);
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
            ItemStack stack = stacks.get(index);
            ItemStack missing = missingMarker(stack, store);
            if (missing != null) {
                stacks.set(index, missing);
                changed = true;
                continue;
            }
            ItemStack restored = restore(stack, store, dimension, bypassBans);
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
        return store.delete(id) ? original : null;
    }

    private static ItemStack missingMarker(ItemStack stack, ServerLevel level) {
        return missingMarker(stack, ItemSnapshotStore.get(level));
    }

    private static ItemStack missingMarker(ItemStack stack, ItemSnapshotStore store) {
        if (!isMarker(stack)) {
            return null;
        }
        UUID id = BannedItemMarkerItem.snapshotId(stack);
        return id != null && !store.contains(id)
            ? BannedItemMarkerItem.withMissingSnapshot(id)
            : null;
    }

    private static void notifyWriteFailure(ServerPlayer player) {
        long now = System.currentTimeMillis();
        Long previous = LAST_WRITE_WARNING.put(player.getUUID(), now);
        if (previous == null || now - previous >= 5000L) {
            player.displayClientMessage(Component.literal("封存写入失败，请检查磁盘空间和权限")
                .withStyle(ChatFormatting.RED), true);
        }
    }
}
