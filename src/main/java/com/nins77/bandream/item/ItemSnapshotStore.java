package com.nins77.bandream.item;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

public final class ItemSnapshotStore extends SavedData {
    private static final String DATA_NAME = "bandream_item_snapshots";
    private final Map<UUID, ItemStack> items = new HashMap<>();

    public static ItemSnapshotStore get(ServerLevel level) {
        ServerLevel storageLevel = level.getServer().overworld();
        return storageLevel.getDataStorage().computeIfAbsent(
            ItemSnapshotStore::load,
            ItemSnapshotStore::new,
            DATA_NAME);
    }

    public UUID put(ItemStack stack) {
        UUID id = UUID.randomUUID();
        items.put(id, stack.copy());
        setDirty();
        return id;
    }

    public ItemStack get(UUID id) {
        ItemStack stack = items.get(id);
        return stack == null ? ItemStack.EMPTY : stack.copy();
    }

    public boolean contains(UUID id) {
        return items.containsKey(id);
    }

    public ItemStack remove(UUID id) {
        ItemStack removed = items.remove(id);
        if (removed != null) {
            setDirty();
        }
        return removed == null ? ItemStack.EMPTY : removed.copy();
    }

    public boolean hasSnapshots() {
        return !items.isEmpty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag entries = new ListTag();
        for (Map.Entry<UUID, ItemStack> entry : items.entrySet()) {
            CompoundTag item = new CompoundTag();
            item.putUUID("id", entry.getKey());
            item.put("stack", entry.getValue().save(new CompoundTag()));
            entries.add(item);
        }
        tag.put("items", entries);
        return tag;
    }

    private static ItemSnapshotStore load(CompoundTag tag) {
        ItemSnapshotStore data = new ItemSnapshotStore();
        ListTag entries = tag.getList("items", Tag.TAG_COMPOUND);
        for (int index = 0; index < entries.size(); index++) {
            CompoundTag entry = entries.getCompound(index);
            if (!entry.hasUUID("id") || !entry.contains("stack", Tag.TAG_COMPOUND)) {
                continue;
            }
            try {
                ItemStack stack = ItemStack.of(entry.getCompound("stack"));
                if (!stack.isEmpty()) {
                    data.items.put(entry.getUUID("id"), stack);
                }
            } catch (RuntimeException ignored) {
            }
        }
        return data;
    }
}
