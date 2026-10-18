package com.nins77.bandream.config;

import java.util.Arrays;
import java.util.Optional;

public enum BanType {
    ITEM("item", true),
    ENTITY("entity", true),
    STRUCTURE("structure", true),
    ENCHANTMENT("enchantment", true),
    DIMENSION("dimension", false),
    EFFECT("effect", true),
    NBT("nbt", true);

    private final String id;
    private final boolean scoped;

    BanType(String id, boolean scoped) {
        this.id = id;
        this.scoped = scoped;
    }

    public String id() {
        return id;
    }

    public boolean supportsScope() {
        return scoped;
    }

    public static Optional<BanType> byId(String value) {
        return Arrays.stream(values())
            .filter(type -> type.id.equalsIgnoreCase(value))
            .findFirst();
    }
}
