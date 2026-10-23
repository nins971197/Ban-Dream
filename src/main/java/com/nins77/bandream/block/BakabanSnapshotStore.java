package com.nins77.bandream.block;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.nins77.bandream.storage.SnapshotFiles;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

public final class BakabanSnapshotStore extends SavedData {
    private static final String DATA_NAME = "bandream_bakaban_snapshots";
    private static final int FORMAT_VERSION = 2;
    private final Path directory;
    private final ServerLevel level;
    private final Set<UUID> ids = new HashSet<>();
    private int loadFailures;
    private int writeFailures;
    private int deleteFailures;

    public BakabanSnapshotStore(Path directory, ServerLevel level) {
        this.directory = directory;
        this.level = level;
    }

    public static BakabanSnapshotStore get(ServerLevel level) {
        ServerLevel storageLevel = level.getServer().overworld();
        Path directory = SnapshotFiles.directory(level, "block_snapshots");
        return storageLevel.getDataStorage().computeIfAbsent(
            tag -> load(tag, directory, storageLevel),
            () -> new BakabanSnapshotStore(directory, storageLevel),
            DATA_NAME);
    }

    public UUID put(BlockState state, CompoundTag blockEntity) {
        UUID id = UUID.randomUUID();
        CompoundTag entry = new CompoundTag();
        entry.putInt("version", FORMAT_VERSION);
        entry.putUUID("id", id);
        entry.put("state", NbtUtils.writeBlockState(state));
        if (blockEntity != null) {
            entry.put("entity", blockEntity.copy());
        }
        if (!SnapshotFiles.write(directory, id, entry)) {
            writeFailures++;
            return null;
        }
        ids.add(id);
        setDirty();
        return id;
    }

    public StoredBlock get(UUID id) {
        if (id == null || !ids.contains(id)) {
            return null;
        }
        try {
            CompoundTag entry = SnapshotFiles.read(directory, id);
            if (entry == null || !entry.contains("state", Tag.TAG_COMPOUND)) {
                loadFailures++;
                return null;
            }
            BlockState state = NbtUtils.readBlockState(
                level.holderLookup(Registries.BLOCK), entry.getCompound("state"));
            if (state.isAir()) {
                loadFailures++;
                return null;
            }
            CompoundTag blockEntity = entry.contains("entity", Tag.TAG_COMPOUND)
                ? entry.getCompound("entity").copy()
                : null;
            return new StoredBlock(state, blockEntity);
        } catch (IOException | RuntimeException exception) {
            loadFailures++;
            return null;
        }
    }

    public boolean exists(UUID id) {
        return id != null && ids.contains(id) && SnapshotFiles.exists(directory, id);
    }

    public StoredBlock remove(UUID id) {
        StoredBlock stored = get(id);
        if (stored == null || !delete(id)) {
            return null;
        }
        return stored;
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

    public Map<UUID, StoredBlock> entries() {
        Map<UUID, StoredBlock> snapshot = new HashMap<>();
        for (UUID id : ids) {
            StoredBlock stored = get(id);
            if (stored != null) {
                snapshot.put(id, stored);
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
        tag.put("blocks", list);
        return tag;
    }

    private static BakabanSnapshotStore load(
        CompoundTag tag,
        Path directory,
        ServerLevel level
    ) {
        BakabanSnapshotStore data = new BakabanSnapshotStore(directory, level);
        SnapshotFiles.ScanResult scan = SnapshotFiles.scan(directory);
        data.loadFailures += scan.failures();
        Set<UUID> ids = new HashSet<>(scan.ids());
        ListTag list = tag.getList("blocks", Tag.TAG_COMPOUND);
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

    public record StoredBlock(BlockState state, CompoundTag blockEntity) {
    }
}
