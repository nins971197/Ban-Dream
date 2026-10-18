package com.nins77.bandream.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;

/**
 * Immutable lookup index for ban or whitelist entries.
 */
final class RuleIndex {
    private static final RuleIndex EMPTY = new RuleIndex(List.of(), Map.of());

    private final List<BanEntry> entries;
    private final Map<BanType, Map<String, List<BanEntry>>> byTypeAndId;

    private RuleIndex(List<BanEntry> entries, Map<BanType, Map<String, List<BanEntry>>> byTypeAndId) {
        this.entries = entries;
        this.byTypeAndId = byTypeAndId;
    }

    static RuleIndex empty() {
        return EMPTY;
    }

    static RuleIndex of(List<BanEntry> source) {
        Map<BanType, Map<String, List<BanEntry>>> indexed = new EnumMap<>(BanType.class);
        for (BanType type : BanType.values()) {
            Map<String, List<BanEntry>> grouped = new HashMap<>();
            for (BanEntry entry : source) {
                if (entry.type == type) {
                    grouped.computeIfAbsent(entry.id, ignored -> new ArrayList<>()).add(entry);
                }
            }
            if (!grouped.isEmpty()) {
                Map<String, List<BanEntry>> immutable = new HashMap<>();
                grouped.forEach((id, rules) -> immutable.put(id, List.copyOf(rules)));
                indexed.put(type, Map.copyOf(immutable));
            }
        }
        return new RuleIndex(List.copyOf(source), Collections.unmodifiableMap(indexed));
    }

    List<BanEntry> entries() {
        return entries;
    }

    Map<String, List<BanEntry>> byId(BanType type) {
        return byTypeAndId.getOrDefault(type, Map.of());
    }

    List<BanEntry> candidates(BanType type, String id) {
        return byId(type).getOrDefault(id, List.of());
    }

    boolean matches(BanType type, String id, ResourceLocation dimension) {
        for (BanEntry entry : candidates(type, id)) {
            if (entry.matchesScope(dimension)) {
                return true;
            }
        }
        return false;
    }

    boolean hasGlobal(BanType type, String id) {
        for (BanEntry entry : candidates(type, id)) {
            if (entry.isGlobal()) {
                return true;
            }
        }
        return false;
    }
}
