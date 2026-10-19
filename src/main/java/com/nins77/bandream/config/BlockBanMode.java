package com.nins77.bandream.config;

import java.util.Arrays;
import java.util.Optional;

public enum BlockBanMode {
    INTERACT("interact"),
    FULL("full");

    private final String id;

    BlockBanMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Optional<BlockBanMode> byId(String value) {
        return Arrays.stream(values())
            .filter(mode -> mode.id.equalsIgnoreCase(value))
            .findFirst();
    }
}
