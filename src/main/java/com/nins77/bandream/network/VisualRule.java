package com.nins77.bandream.network;

import com.nins77.bandream.config.BanType;

public record VisualRule(
    BanType type,
    String id,
    String dimension,
    String path,
    String value
) {
    public boolean matchesDimension(String currentDimension) {
        return dimension == null || dimension.isEmpty() || dimension.equals(currentDimension);
    }
}
