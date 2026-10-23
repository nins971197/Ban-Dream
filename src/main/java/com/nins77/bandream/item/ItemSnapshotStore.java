package com.nins77.bandream.item;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.nins77.bandream.storage.SnapshotFiles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

public final class ItemSnapshotStore extends SavedData {
    private static final String DATA_NAME = "bandream_item_snapshots";
    private static final int FORMAT_VERSION = 2;
    private final Path directory;
    private final Set<UUID> ids = new HashSet<>();
    private int loadFailures;
    private int writeFailures;
    private int deleteFailures;

    public ItemSnapshotStore(Path directory) {
        this.directory = directory;
    }

    public static ItemSnapshotStore get(ServerLevel level) {
        ServerLevel storageLevel = level.getServer().overworld();
        Path directory = SnapshotFiles.directory(level, "item_snapshots");
        return storageLevel.getDataStorage().computeIfAbsent(
            tag -> load(tag, directory),
            () -> new ItemSnapshotStore(directory),
            DATA_NAME);
    }

    public UUID put(ItemStack stack) {
        UUID id = UUID.randomUUID();
        CompoundTag entry = new CompoundTag();
        entry.putInt("version", FORMAT_VERSION);
        entry.putUUID("id", id);
        entry.put("stack", stack.copy().save(new CompoundTag()));
        if (!SnapshotFiles.write(directory, id, entry)) {
            writeFailures++;
            return null;
        }
        ids.add(id);
        setDirty();
        return id;
    }

    public ItemStack get(UUID id) {
        if (id == null || !ids.contains(id)) {
            return ItemStack.EMPTY;
        }
        try {
            CompoundTag entry = SnapshotFiles.read(directory, id);
            if (entry == null || !entry.contains("stack", Tag.TAG_COMPOUND)) {
                loadFailures++;
                return ItemStack.EMPTY;
            }
            return ItemStack.of(entry.getCompound("stack"));
        } catch (IOException | RuntimeException exception) {
            loadFailures++;
            return ItemStack.EMPTY;
        }
    }

    public boolean contains(UUID id) {
        if (id == null || !ids.contains(id)) {
            return false;
        }
        if (SnapshotFiles.exists(directory, id)) {
            return true;
        }
        ids.remove(id);
        loadFailures++;
        setDirty();
        return false;
    }

    public boolean exists(UUID id) {
        return id != null && ids.contains(id) && SnapshotFiles.exists(directory, id);
    }

    public ItemStack remove(UUID id) {
        ItemStack stack = get(id);
        if (stack.isEmpty() || !delete(id)) {
            return ItemStack.EMPTY;
        }
        return stack;
    }

    public boolean delete(UUID id) {
        if (id == null) {
            return false;
        }
        try {
            if (!SnapshotFiles.exists(directory, id)) {
                ids.remove(id);
                return false;
            }
            boolean deleted = SnapshotFiles.delete(directory, id);
            if (deleted) {
                ids.remove(id);
                setDirty();
            } else {
                deleteFailures++;
            }
            return deleted;
        } catch (IOException exception) {
            deleteFailures++;
            return false;
        }
    }

    public boolean hasSnapshots() {
        return !ids.isEmpty();
    }

    public int size() {
        return ids.size();
    }

    public int loadFailures() {
        return loadFailures;
    }

    public int writeFailures() {
        return writeFailures;
    }

    public int deleteFailures() {
        return deleteFailures;
    }

    public java.util.List<UUID> ids() {
        return ids.stream()
            .sorted((left, right) -> left.toString().compareTo(right.toString()))
            .toList();
    }

    public Map<UUID, ItemStack> entries() {
        Map<UUID, ItemStack> snapshot = new HashMap<>();
        for (UUID id : ids) {
            ItemStack stack = get(id);
            if (!stack.isEmpty()) {
                snapshot.put(id, stack);
            }
        }
        return Map.copyOf(snapshot);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("version", FORMAT_VERSION);
        ListTag list = new ListTag();
        for (UUID id : ids) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("id", id);
            list.add(entry);
        }
        tag.put("items", list);
        return tag;
    }

    private static ItemSnapshotStore load(CompoundTag tag, Path directory) {
        ItemSnapshotStore data = new ItemSnapshotStore(directory);
        SnapshotFiles.ScanResult scan = SnapshotFiles.scan(directory);
        data.loadFailures += scan.failures();
        Set<UUID> ids = new HashSet<>(scan.ids());
        ListTag list = tag.getList("items", Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag entry = list.getCompound(index);
            if (entry.hasUUID("id")) {
                ids.add(entry.getUUID("id"));
            } else {
                data.loadFailures++;
            }
        }
        ids.removeIf(id -> !SnapshotFiles.exists(directory, id));
        data.ids.addAll(ids);
        return data;
    }
}
