package com.nins77.bandream.block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

public final class BakabanSavedData extends SavedData {
    private static final String DATA_NAME = "bandream_bakaban";
    private final Map<BlockPos, StoredBlock> blocks = new HashMap<>();
    private final Map<Long, Set<BlockPos>> chunkIndex = new HashMap<>();

    public static BakabanSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            tag -> load(tag, level),
            BakabanSavedData::new,
            DATA_NAME);
    }

    private static BakabanSavedData load(CompoundTag tag, ServerLevel level) {
        BakabanSavedData data = new BakabanSavedData();
        ListTag entries = tag.getList("blocks", Tag.TAG_COMPOUND);
        for (int index = 0; index < entries.size(); index++) {
            CompoundTag entry = entries.getCompound(index);
            if (!entry.contains("pos", Tag.TAG_COMPOUND) || !entry.contains("state", Tag.TAG_COMPOUND)) {
                continue;
            }
            try {
                BlockPos pos = NbtUtils.readBlockPos(entry.getCompound("pos"));
                BlockState state = NbtUtils.readBlockState(
                    level.holderLookup(Registries.BLOCK), entry.getCompound("state"));
                if (state.isAir()) {
                    continue;
                }
                CompoundTag blockEntity = entry.contains("entity", Tag.TAG_COMPOUND)
                    ? entry.getCompound("entity").copy()
                    : null;
                data.putInternal(pos, state, blockEntity);
            } catch (RuntimeException ignored) {
            }
        }
        return data;
    }

    public StoredBlock get(BlockPos pos) {
        return blocks.get(pos);
    }

    public void put(BlockPos pos, BlockState state, CompoundTag blockEntity) {
        putInternal(pos, state, blockEntity);
        setDirty();
    }

    public StoredBlock remove(BlockPos pos) {
        StoredBlock removed = blocks.remove(pos);
        if (removed != null) {
            removeIndex(pos);
            setDirty();
        }
        return removed;
    }

    public List<BlockPos> positions() {
        return new ArrayList<>(blocks.keySet());
    }

    public List<BlockPos> positionsInChunk(ChunkPos chunk) {
        Set<BlockPos> positions = chunkIndex.get(chunk.toLong());
        return positions == null ? List.of() : new ArrayList<>(positions);
    }

    public boolean hasEntriesInChunk(ChunkPos chunk) {
        Set<BlockPos> positions = chunkIndex.get(chunk.toLong());
        return positions != null && !positions.isEmpty();
    }

    public boolean isEmpty() {
        return blocks.isEmpty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag entries = new ListTag();
        for (Map.Entry<BlockPos, StoredBlock> entry : blocks.entrySet()) {
            CompoundTag item = new CompoundTag();
            item.put("pos", NbtUtils.writeBlockPos(entry.getKey()));
            item.put("state", NbtUtils.writeBlockState(entry.getValue().state()));
            if (entry.getValue().blockEntity() != null) {
                item.put("entity", entry.getValue().blockEntity().copy());
            }
            entries.add(item);
        }
        tag.put("blocks", entries);
        return tag;
    }

    private void putInternal(BlockPos pos, BlockState state, CompoundTag blockEntity) {
        BlockPos key = pos.immutable();
        removeIndex(key);
        blocks.put(key, new StoredBlock(state, blockEntity == null ? null : blockEntity.copy()));
        chunkIndex.computeIfAbsent(chunkKey(key), ignored -> new HashSet<>()).add(key);
    }

    private void removeIndex(BlockPos pos) {
        Set<BlockPos> positions = chunkIndex.get(chunkKey(pos));
        if (positions == null) {
            return;
        }
        positions.remove(pos);
        if (positions.isEmpty()) {
            chunkIndex.remove(chunkKey(pos));
        }
    }

    private static long chunkKey(BlockPos pos) {
        return ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
    }

    public record StoredBlock(BlockState state, CompoundTag blockEntity) {
    }
}
