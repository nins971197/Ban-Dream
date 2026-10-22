package com.nins77.bandream.block;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
    private final Map<UUID, StoredBlock> blocks = new HashMap<>();
    private int loadFailures;

    public static BakabanSnapshotStore get(ServerLevel level) {
        ServerLevel storageLevel = level.getServer().overworld();
        return storageLevel.getDataStorage().computeIfAbsent(
            tag -> load(tag, storageLevel),
            BakabanSnapshotStore::new,
            DATA_NAME);
    }

    public UUID put(BlockState state, CompoundTag blockEntity) {
        UUID id = UUID.randomUUID();
        blocks.put(id, new StoredBlock(state, blockEntity == null ? null : blockEntity.copy()));
        setDirty();
        return id;
    }

    public StoredBlock get(UUID id) {
        return blocks.get(id);
    }

    public StoredBlock remove(UUID id) {
        StoredBlock removed = blocks.remove(id);
        if (removed != null) {
            setDirty();
        }
        return removed;
    }

    public boolean hasSnapshots() {
        return !blocks.isEmpty();
    }

    public int size() {
        return blocks.size();
    }

    public Map<UUID, StoredBlock> entries() {
        return Map.copyOf(blocks);
    }

    public int loadFailures() {
        return loadFailures;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag entries = new ListTag();
        for (Map.Entry<UUID, StoredBlock> entry : blocks.entrySet()) {
            CompoundTag item = new CompoundTag();
            item.putUUID("id", entry.getKey());
            item.put("state", NbtUtils.writeBlockState(entry.getValue().state()));
            if (entry.getValue().blockEntity() != null) {
                item.put("entity", entry.getValue().blockEntity().copy());
            }
            entries.add(item);
        }
        tag.put("blocks", entries);
        return tag;
    }

    private static BakabanSnapshotStore load(CompoundTag tag, ServerLevel level) {
        BakabanSnapshotStore data = new BakabanSnapshotStore();
        ListTag entries = tag.getList("blocks", Tag.TAG_COMPOUND);
        for (int index = 0; index < entries.size(); index++) {
            CompoundTag entry = entries.getCompound(index);
            if (!entry.hasUUID("id") || !entry.contains("state", Tag.TAG_COMPOUND)) {
                data.loadFailures++;
                continue;
            }
            try {
                UUID id = entry.getUUID("id");
                BlockState state = NbtUtils.readBlockState(
                    level.holderLookup(Registries.BLOCK), entry.getCompound("state"));
                if (state.isAir()) {
                    continue;
                }
                CompoundTag blockEntity = entry.contains("entity", Tag.TAG_COMPOUND)
                    ? entry.getCompound("entity").copy()
                    : null;
                data.blocks.put(id, new StoredBlock(state, blockEntity));
            } catch (RuntimeException exception) {
                data.loadFailures++;
            }
        }
        return data;
    }

    public record StoredBlock(BlockState state, CompoundTag blockEntity) {
    }
}
