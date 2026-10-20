package com.nins77.bandream.config;

import java.util.Arrays;
import java.util.Optional;

public enum BanType {
    ITEM("item", false),
    ENTITY("entity", true),
    STRUCTURE("structure", false),
    ENCHANTMENT("enchantment", false),
    DIMENSION("dimension", false),
    EFFECT("effect", false),
    BLOCK("block", false),
    NBT("nbt", false);

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
