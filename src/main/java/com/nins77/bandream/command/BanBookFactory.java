package com.nins77.bandream.command;

import java.util.ArrayList;
import java.util.List;

import com.nins77.bandream.config.BanEntry;
import com.nins77.bandream.config.BanType;
import com.nins77.bandream.text.BanText;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.WrittenBookItem;

public final class BanBookFactory {
    private static final int LINES_PER_PAGE = 13;
    private static final int LINE_WIDTH = 24;
    private static final int PAGES_PER_BOOK = WrittenBookItem.MAX_PAGES;
    private static final int HEADER_LINES = 6;
    private static final int LINES_PER_BOOK = LINES_PER_PAGE * PAGES_PER_BOOK - HEADER_LINES;

    private BanBookFactory() {
    }

    public static List<ItemStack> createVolumes(BanType type, List<BanEntry> entries) {
        List<String> body = new ArrayList<>();
        for (BanEntry entry : entries) {
            body.addAll(BanText.wrappedLines(entry, LINE_WIDTH));
        }
        if (body.isEmpty()) {
            body.add("没有封禁记录");
        }

        int volumeCount = Math.max(1, (body.size() + LINES_PER_BOOK - 1) / LINES_PER_BOOK);
        List<ItemStack> books = new ArrayList<>(volumeCount);
        for (int volume = 0; volume < volumeCount; volume++) {
            int start = volume * LINES_PER_BOOK;
            int end = Math.min(body.size(), start + LINES_PER_BOOK);
            List<String> lines = new ArrayList<>();
            lines.add("Ban Dream");
            lines.add(BanText.typeLabel(type) + "封禁清单");
            lines.add("共 " + entries.size() + " 项");
            if (volumeCount > 1) {
                lines.add("第 " + (volume + 1) + " / " + volumeCount + " 卷");
            }
            lines.add("");
            lines.addAll(body.subList(start, end));
            books.add(createBook(type, volume + 1, volumeCount, lines));
        }
        return books;
    }

    private static ItemStack createBook(BanType type, int volume, int volumeCount, List<String> lines) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = book.getOrCreateTag();
        String title = "Ban Dream - " + BanText.typeLabel(type);
        if (volumeCount > 1) {
            title += " " + volume + "/" + volumeCount;
        }
        tag.putString(WrittenBookItem.TAG_TITLE, title);
        tag.putString(WrittenBookItem.TAG_AUTHOR, "nins77");
        tag.putInt(WrittenBookItem.TAG_GENERATION, 0);

        ListTag pages = new ListTag();
        for (int start = 0; start < lines.size(); start += LINES_PER_PAGE) {
            int end = Math.min(lines.size(), start + LINES_PER_PAGE);
            String page = String.join("\n", lines.subList(start, end));
            pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(page))));
        }
        tag.put(WrittenBookItem.TAG_PAGES, pages);
        tag.putBoolean(WrittenBookItem.TAG_RESOLVED, true);
        book.setHoverName(Component.literal(title));
        return book;
    }
}
