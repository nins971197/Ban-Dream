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
    private static final ThreadLocal<Long> LAST_WRITE_NANOS = ThreadLocal.withInitial(() -> 0L);

    private SnapshotFiles() {
    }

    public static Path directory(ServerLevel level, String name) {
        return level.getServer().getWorldPath(LevelResource.ROOT)
            .resolve("bandream")
            .resolve(name);
    }

    public static boolean write(Path directory, UUID id, CompoundTag tag) {
        long started = System.nanoTime();
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
            LAST_WRITE_NANOS.set(System.nanoTime() - started);
            return true;
        } catch (IOException exception) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException ignored) {
            }
            LAST_WRITE_NANOS.set(System.nanoTime() - started);
            return false;
        }
    }

    public static long consumeLastWriteNanos() {
        long elapsed = LAST_WRITE_NANOS.get();
        LAST_WRITE_NANOS.remove();
        return elapsed;
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

    public static ScanResult scan(Path directory) {
        Set<UUID> ids = new HashSet<>();
        int failures = 0;
        if (!Files.isDirectory(directory)) {
            return new ScanResult(ids, failures);
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.nbt")) {
            for (Path path : stream) {
                String file = path.getFileName().toString();
                try {
                    ids.add(UUID.fromString(file.substring(0, file.length() - 4)));
                } catch (IllegalArgumentException ignored) {
                    failures++;
                }
            }
        } catch (IOException ignored) {
            failures++;
        }
        return new ScanResult(ids, failures);
    }

    public record ScanResult(Set<UUID> ids, int failures) {
    }
}
