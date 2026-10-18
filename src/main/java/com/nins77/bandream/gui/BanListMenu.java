package com.nins77.bandream.gui;

import java.util.ArrayList;
import java.util.List;

import com.nins77.bandream.config.BanEntry;
import com.nins77.bandream.config.BanRules;
import com.nins77.bandream.config.BanScope;
import com.nins77.bandream.config.BanTrigger;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

public final class BanListMenu extends AbstractContainerMenu {
    public static final int CONTAINER_SLOTS = 45;
    public static final int CONTENT_SLOTS = 36;
    public static final int PREVIOUS_SLOT = 36;
    public static final int CLOSE_SLOT = 40;
    public static final int NEXT_SLOT = 44;
    private static final int PLAYER_INVENTORY_Y = 120;
    private static final int HOTBAR_Y = 178;

    private final SimpleContainer container = new SimpleContainer(CONTAINER_SLOTS);
    private final ContainerData data = new SimpleContainerData(2);
    private final List<BanRules.ItemGroup> groups;
    private int page;
    private int totalPages = 1;

    public BanListMenu(int windowId, Inventory playerInventory) {
        super(BanMenus.BAN_LIST.get(), windowId);
        this.groups = BanRules.itemGroups();
        this.totalPages = Math.max(1, (groups.size() + CONTENT_SLOTS - 1) / CONTENT_SLOTS);

        for (int row = 0; row < 5; row++) {
            for (int column = 0; column < 9; column++) {
                int index = column + row * 9;
                this.addSlot(new ReadOnlySlot(container, index, 8 + column * 18, 18 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9,
                    8 + column * 18, PLAYER_INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, 8 + column * 18, HOTBAR_Y));
        }

        data.set(0, 0);
        data.set(1, totalPages);
        this.addDataSlots(data);
        refresh();
    }

    public static void open(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
            (windowId, inventory, ignored) -> new BanListMenu(windowId, inventory),
            Component.translatable("bandream.gui.title")));
    }

    private void refresh() {
        container.clearContent();
        int start = page * CONTENT_SLOTS;
        for (int index = 0; index < CONTENT_SLOTS; index++) {
            int groupIndex = start + index;
            if (groupIndex < groups.size()) {
                container.setItem(index, stackFor(groups.get(groupIndex), groupIndex));
            }
        }
        if (page > 0) {
            container.setItem(PREVIOUS_SLOT, navStack(Items.SPECTRAL_ARROW,
                Component.translatable("bandream.gui.previous").withStyle(ChatFormatting.AQUA),
                page));
        }
        container.setItem(CLOSE_SLOT, navStack(Items.BARRIER,
            Component.translatable("bandream.gui.close").withStyle(ChatFormatting.RED), page));
        if (page + 1 < totalPages) {
            container.setItem(NEXT_SLOT, navStack(Items.SPECTRAL_ARROW,
                Component.translatable("bandream.gui.next").withStyle(ChatFormatting.AQUA),
                page + 2));
        }
        data.set(0, page);
        data.set(1, totalPages);
        this.broadcastChanges();
    }

    private ItemStack stackFor(BanRules.ItemGroup group, int index) {
        ResourceLocation id = ResourceLocation.tryParse(group.id());
        Item item = id == null ? null : BuiltInRegistries.ITEM.get(id);
        boolean missing = item == null || item == Items.AIR && !"minecraft:air".equals(group.id());
        ItemStack stack = new ItemStack(missing ? Items.BARRIER : item);
        stack.setCount(1);
        if (missing) {
            stack.setHoverName(Component.literal(group.id()).withStyle(ChatFormatting.RED));
        }

        List<Component> lore = new ArrayList<>();
        if (missing) {
            lore.add(Component.literal("物品未加载（可能缺少对应模组）").withStyle(ChatFormatting.DARK_RED));
        }
        for (BanEntry entry : group.entries()) {
            String scope = entry.scope == BanScope.DIMENSION
                ? "维度 " + entry.dimension
                : "全局";
            String trigger = entry.trigger == BanTrigger.USE
                ? "使用/交互时删除"
                : "进入背包时删除";
            lore.add(Component.literal("范围: " + scope).withStyle(ChatFormatting.GRAY));
            lore.add(Component.literal("方式: " + trigger).withStyle(ChatFormatting.GRAY));
            if (Boolean.FALSE.equals(entry.jeiVisible)) {
                lore.add(Component.literal("JEI: 隐藏").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        lore.add(Component.literal("编号 #" + (index + 1)).withStyle(ChatFormatting.DARK_GRAY));
        ListTag loreTag = new ListTag();
        for (Component line : lore) {
            loreTag.add(StringTag.valueOf(Component.Serializer.toJson(line)));
        }
        stack.getOrCreateTagElement("display").put("Lore", loreTag);
        return stack;
    }

    private ItemStack navStack(Item item, Component name, int targetPage) {
        ItemStack stack = new ItemStack(item);
        stack.setHoverName(name);
        ListTag lore = new ListTag();
        lore.add(StringTag.valueOf(Component.Serializer.toJson(
            Component.literal("第 " + targetPage + " / " + totalPages + " 页")
                .withStyle(ChatFormatting.GRAY))));
        stack.getOrCreateTagElement("display").put("Lore", lore);
        return stack;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId == PREVIOUS_SLOT) {
            if (page > 0) {
                page--;
                refresh();
            }
            return;
        }
        if (slotId == NEXT_SLOT) {
            if (page + 1 < totalPages) {
                page++;
                refresh();
            }
            return;
        }
        if (slotId == CLOSE_SLOT) {
            player.closeContainer();
            return;
        }
        if (slotId >= 0 && slotId < CONTAINER_SLOTS) {
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public int page() {
        return data.get(0);
    }

    public int totalPages() {
        return Math.max(1, data.get(1));
    }

    private static final class ReadOnlySlot extends Slot {
        private ReadOnlySlot(net.minecraft.world.Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean allowModification(Player player) {
            return false;
        }
    }
}
