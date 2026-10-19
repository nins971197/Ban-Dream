package com.nins77.bandream.command;

import java.util.ArrayList;
import java.util.List;

import com.nins77.bandream.config.BanEntry;
import com.nins77.bandream.config.BanScope;
import com.nins77.bandream.config.BanTrigger;
import com.nins77.bandream.config.BanType;
import com.nins77.bandream.config.BlockBanMode;

public final class BanText {
    private BanText() {
    }

    public static String typeLabel(BanType type) {
        return switch (type) {
            case ITEM -> "物品";
            case ENTITY -> "生物";
            case STRUCTURE -> "结构";
            case ENCHANTMENT -> "附魔";
            case DIMENSION -> "维度";
            case EFFECT -> "Buff";
            case BLOCK -> "方块";
            case NBT -> "NBT";
        };
    }

    public static String scope(BanEntry entry) {
        if (entry.type == BanType.DIMENSION) {
            return "全局";
        }
        if (entry.scope == BanScope.DIMENSION) {
            return entry.dimension;
        }
        return "全局";
    }

    public static String trigger(BanEntry entry) {
        return entry.trigger == BanTrigger.USE ? "使用/交互时删除" : "进入背包时删除";
    }

    public static String line(BanEntry entry) {
        StringBuilder out = new StringBuilder(entry.id);
        if (entry.type == BanType.NBT) {
            out.append(" | NBT=").append(entry.nbtDescription());
        }
        out.append(" | 范围=").append(scope(entry));
        if (entry.type != BanType.DIMENSION) {
            if (entry.type == BanType.ITEM || entry.type == BanType.NBT) {
                out.append(" | ").append(trigger(entry));
            }
            if (entry.type == BanType.ITEM) {
                out.append(" | JEI=").append(Boolean.FALSE.equals(entry.jeiVisible) ? "隐藏" : "显示");
            }
            if (entry.type == BanType.BLOCK) {
                out.append(" | 模式=")
                    .append(entry.blockMode == BlockBanMode.INTERACT ? "禁止交互" : "完全禁止");
            }
        }
        return out.toString();
    }

    public static List<String> wrappedLines(BanEntry entry, int width) {
        String value = line(entry);
        List<String> lines = new ArrayList<>();
        while (value.length() > width) {
            int split = value.lastIndexOf(' ', width);
            if (split < width / 2) {
                split = width;
            }
            lines.add(value.substring(0, split));
            value = value.substring(split).stripLeading();
        }
        if (!value.isBlank()) {
            lines.add(value);
        }
        return lines;
    }
}
