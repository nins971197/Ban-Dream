package com.nins77.bandream.storage;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

public final class SnapshotFiles {
    private SnapshotFiles() {
    }

    public static Path directory(ServerLevel level, String name) {
        return level.getServer().getWorldPath(LevelResource.ROOT)
            .resolve("bandream")
            .resolve(name);
    }

    public static boolean write(Path directory, UUID id, CompoundTag tag) {
        Path target = directory.resolve(id + ".nbt");
        Path temporary = directory.resolve(id + ".tmp");
        try {
            Files.createDirectories(directory);
            NbtIo.writeCompressed(tag, temporary.toFile());
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException exception) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException ignored) {
            }
            return false;
        }
    }

    public static CompoundTag read(Path directory, UUID id) throws IOException {
        Path file = directory.resolve(id + ".nbt");
        return Files.isRegularFile(file) ? NbtIo.readCompressed(file.toFile()) : null;
    }

    public static boolean delete(Path directory, UUID id) throws IOException {
        return Files.deleteIfExists(directory.resolve(id + ".nbt"));
    }

    public static boolean exists(Path directory, UUID id) {
        return Files.isRegularFile(directory.resolve(id + ".nbt"));
    }

    public static Set<UUID> list(Path directory) {
        Set<UUID> ids = new HashSet<>();
        if (!Files.isDirectory(directory)) {
            return ids;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.nbt")) {
            for (Path path : stream) {
                String file = path.getFileName().toString();
                try {
                    ids.add(UUID.fromString(file.substring(0, file.length() - 4)));
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (IOException ignored) {
        }
        return ids;
    }
}
