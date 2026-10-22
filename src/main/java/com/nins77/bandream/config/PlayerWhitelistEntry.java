package com.nins77.bandream.config;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class PlayerWhitelistEntry {
    public String name;
    public String uuid;

    public PlayerWhitelistEntry() {
    }

    public PlayerWhitelistEntry(String name, UUID uuid) {
        this.name = name;
        this.uuid = uuid == null ? null : uuid.toString();
    }

    public boolean normalize() {
        if (name != null) {
            name = name.trim();
        }
        if (uuid != null) {
            try {
                uuid = UUID.fromString(uuid.trim()).toString();
            } catch (IllegalArgumentException ignored) {
                return false;
            }
        }
        return name != null && !name.isBlank() || uuid != null;
    }

    public boolean matchesKey(String value) {
        String key = value.trim();
        if (uuid != null && uuid.equalsIgnoreCase(key)) {
            return true;
        }
        return name != null && name.equalsIgnoreCase(key);
    }

    public boolean sameIdentity(PlayerWhitelistEntry other) {
        if (uuid != null && other.uuid != null) {
            return uuid.equals(other.uuid);
        }
        return name != null && other.name != null && name.equalsIgnoreCase(other.name);
    }

    public String nameKey() {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }

    public String display() {
        if (name != null && uuid != null) {
            return name + " (" + uuid + ")";
        }
        if (name != null) {
            return name;
        }
        return Objects.requireNonNullElse(uuid, "unknown");
    }
}
