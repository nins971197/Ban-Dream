package com.nins77.bandream.config;

import java.util.Objects;

import net.minecraft.resources.ResourceLocation;

public final class BanEntry {
    public BanType type = BanType.ITEM;
    public String id = "";
    public BanScope scope = BanScope.GLOBAL;
    public String dimension;
    public Boolean jeiVisible = true;
    public BanTrigger trigger = BanTrigger.INVENTORY;
    public String path;
    public String value;

    public BanEntry() {
    }

    public BanEntry(BanType type, String id, BanScope scope, String dimension) {
        this.type = type;
        this.id = id;
        this.scope = scope;
        this.dimension = dimension;
    }

    public BanEntry copy() {
        BanEntry out = new BanEntry(type, id, scope, dimension);
        out.jeiVisible = jeiVisible;
        out.trigger = trigger;
        out.path = path;
        out.value = value;
        return out;
    }

    public boolean normalize() {
        if (type == null || id == null || id.isBlank()) {
            return false;
        }
        id = id.trim();
        if (!(type == BanType.NBT && "*".equals(id))) {
            ResourceLocation parsed = ResourceLocation.tryParse(id);
            if (parsed == null) {
                return false;
            }
            id = parsed.toString();
        }

        if (scope == null) {
            scope = BanScope.GLOBAL;
        }
        if (!type.supportsScope()) {
            scope = BanScope.GLOBAL;
            dimension = null;
        } else if (scope == BanScope.DIMENSION) {
            ResourceLocation parsedDimension = ResourceLocation.tryParse(dimension == null ? "" : dimension);
            if (parsedDimension == null) {
                return false;
            } else {
                dimension = parsedDimension.toString();
            }
        } else {
            dimension = null;
        }

        if (trigger == null) {
            trigger = BanTrigger.INVENTORY;
        }
        if (type == BanType.DIMENSION) {
            jeiVisible = null;
            trigger = BanTrigger.INVENTORY;
            path = null;
            value = null;
        } else if (type == BanType.BLOCK) {
            jeiVisible = null;
            trigger = null;
            path = null;
            value = null;
        } else if (type == BanType.NBT) {
            if (path == null || path.isBlank()) {
                return false;
            }
            path = path.trim();
            if (path.startsWith("tag.")) {
                path = path.substring(4);
            }
            if (path.startsWith(".")) {
                path = path.substring(1);
            }
            if (path.isBlank()) {
                return false;
            }
            if (value == null || value.isBlank()) {
                value = "*";
            }
            jeiVisible = null;
        } else {
            path = null;
            value = null;
            if (type != BanType.ITEM) {
                jeiVisible = null;
            } else if (jeiVisible == null) {
                jeiVisible = true;
            }
        }
        if (type != BanType.ITEM && type != BanType.NBT) {
            trigger = null;
        }
        return true;
    }

    public boolean isGlobal() {
        return scope != BanScope.DIMENSION;
    }

    public boolean matchesScope(ResourceLocation currentDimension) {
        return isGlobal() || Objects.equals(dimension, currentDimension.toString());
    }

    public boolean matchesScope(String currentDimension) {
        return isGlobal() || Objects.equals(dimension, currentDimension);
    }

    public boolean sameKey(BanEntry other) {
        if (type != other.type || !id.equals(other.id)
            || scope != other.scope || !Objects.equals(dimension, other.dimension)) {
            return false;
        }
        if (type == BanType.NBT) {
            return Objects.equals(path, other.path) && Objects.equals(value, other.value);
        }
        return true;
    }

    public String nbtDescription() {
        if (type != BanType.NBT) {
            return "";
        }
        return path + "=" + value;
    }
}
