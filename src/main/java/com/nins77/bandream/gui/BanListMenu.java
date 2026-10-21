package com.nins77.bandream.gui;

import java.util.ArrayList;
import java.util.List;

import com.nins77.bandream.action.PendingActionManager;
import com.nins77.bandream.config.BanEntry;
import com.nins77.bandream.config.BanRules;
import com.nins77.bandream.config.BanType;
import com.nins77.bandream.network.BanNetwork;
import com.nins77.bandream.network.ChoiceLayout;
import com.nins77.bandream.text.BanText;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.network.NetworkHooks;

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
    private final BanType type;
    private final List<BanRules.RuleGroup> groups;
    private int page;
    private int totalPages = 1;

    public BanListMenu(int windowId, Inventory playerInventory, BanType type) {
        super(BanMenus.BAN_LIST.get(), windowId);
        this.type = listType(type);
        this.groups = BanRules.groups(this.type);
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

    public static void open(ServerPlayer player, BanType type) {
        BanType listType = listType(type);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (windowId, inventory, ignored) -> new BanListMenu(windowId, inventory, listType),
            Component.translatable(titleKey(listType))), buffer -> buffer.writeEnum(listType));
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
        if (groups.isEmpty()) {
            container.setItem(0, emptyStack());
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

    private ItemStack stackFor(BanRules.RuleGroup group, int index) {
        return type == BanType.BLOCK ? blockStack(group, index) : itemStack(group, index);
    }

    private ItemStack itemStack(BanRules.RuleGroup group, int index) {
        ResourceLocation id = ResourceLocation.tryParse(group.id());
        Item item = id == null ? null : BuiltInRegistries.ITEM.get(id);
        boolean air = item == Items.AIR;
        boolean missing = item == null || (item == Items.AIR && !"minecraft:air".equals(group.id()));
        Item icon = missing || air ? Items.BARRIER : item;
        ItemStack stack = new ItemStack(icon);
        stack.setCount(1);
        if (missing || air) {
            stack.setHoverName(Component.literal(group.id())
                .withStyle(missing ? ChatFormatting.RED : ChatFormatting.GRAY));
        }

        List<Component> lore = new ArrayList<>();
        if (missing) {
            lore.add(Component.literal("物品未加载（可能缺少对应模组）").withStyle(ChatFormatting.DARK_RED));
        }
        lore.add(Component.literal("ID: " + group.id()).withStyle(ChatFormatting.DARK_GRAY));
        for (BanEntry entry : group.entries()) {
            lore.add(Component.literal("范围: " + BanText.scopeLabel(entry)).withStyle(ChatFormatting.GRAY));
            lore.add(Component.literal("方式: " + BanText.trigger(entry)).withStyle(ChatFormatting.GRAY));
            if (Boolean.FALSE.equals(entry.jeiVisible)) {
                lore.add(Component.literal("JEI: 隐藏").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        lore.add(Component.literal("状态: 已封存为 999ban").withStyle(ChatFormatting.GRAY));
        lore.add(Component.literal("左键：管理封禁方式 / 解封").withStyle(ChatFormatting.AQUA));
        lore.add(Component.literal("编号 #" + (index + 1)).withStyle(ChatFormatting.DARK_GRAY));
        setLore(stack, lore);
        return stack;
    }

    private ItemStack blockStack(BanRules.RuleGroup group, int index) {
        ResourceLocation id = ResourceLocation.tryParse(group.id());
        Block block = id == null ? null : BuiltInRegistries.BLOCK.get(id);
        boolean missing = block == null || (block == Blocks.AIR && !"minecraft:air".equals(group.id()));
        Item icon = missing ? Items.BARRIER : block.asItem();
        boolean noItem = !missing && icon == Items.AIR;
        ItemStack stack = new ItemStack(missing || noItem ? Items.BARRIER : icon);
        stack.setCount(1);
        if (missing) {
            stack.setHoverName(Component.literal(group.id()).withStyle(ChatFormatting.RED));
        } else if (noItem) {
            stack.setHoverName(block.getName().copy().withStyle(ChatFormatting.AQUA));
        }

        List<Component> lore = new ArrayList<>();
        if (missing) {
            lore.add(Component.literal("方块未加载（可能缺少对应模组）").withStyle(ChatFormatting.DARK_RED));
        } else if (noItem) {
            lore.add(Component.literal("该方块没有物品形式，使用屏障图标").withStyle(ChatFormatting.DARK_GRAY));
        }
        lore.add(Component.literal("ID: " + group.id()).withStyle(ChatFormatting.DARK_GRAY));
        for (BanEntry entry : group.entries()) {
            lore.add(Component.literal("范围: " + BanText.scopeLabel(entry)).withStyle(ChatFormatting.GRAY));
        }
        lore.add(Component.literal("状态: 已重定向为 bakaban").withStyle(ChatFormatting.GRAY));
        lore.add(Component.literal("左键：管理封禁 / 解封").withStyle(ChatFormatting.AQUA));
        lore.add(Component.literal("编号 #" + (index + 1)).withStyle(ChatFormatting.DARK_GRAY));
        setLore(stack, lore);
        return stack;
    }

    private ItemStack emptyStack() {
        ItemStack stack = new ItemStack(Items.BARRIER);
        stack.setHoverName(Component.translatable("bandream.gui.empty." + type.id())
            .withStyle(ChatFormatting.GRAY));
        return stack;
    }

    private static void setLore(ItemStack stack, List<Component> lore) {
        ListTag loreTag = new ListTag();
        for (Component line : lore) {
            loreTag.add(StringTag.valueOf(Component.Serializer.toJson(line)));
        }
        stack.getOrCreateTagElement("display").put("Lore", loreTag);
    }

    private ItemStack navStack(Item item, Component name, int targetPage) {
        ItemStack stack = new ItemStack(item);
        stack.setHoverName(name);
        setLore(stack, List.of(Component.translatable("bandream.gui.page", targetPage, totalPages)
            .withStyle(ChatFormatting.GRAY)));
        return stack;
    }

    private static BanType listType(BanType type) {
        return type == BanType.BLOCK ? BanType.BLOCK : BanType.ITEM;
    }

    private static String titleKey(BanType type) {
        return "bandream.gui.title." + type.id();
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
            if (slotId < CONTENT_SLOTS && button == 0 && clickType == ClickType.PICKUP
                && player instanceof ServerPlayer serverPlayer) {
                int groupIndex = page * CONTENT_SLOTS + slotId;
                if (groupIndex < groups.size()) {
                    openEntryChoice(serverPlayer, groups.get(groupIndex));
                }
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    private void openEntryChoice(ServerPlayer player, BanRules.RuleGroup group) {
        if (!player.hasPermissions(Commands.LEVEL_ADMINS)) {
            return;
        }
        List<String> options = type == BanType.BLOCK
            ? List.of("禁止（全局）", "解封")
            : List.of("禁止（全局）", "解封");
        player.closeContainer();
        int actionId = PendingActionManager.offer(
            player, PendingActionManager.Kind.LIST_ENTRY, type, List.of(group.id()));
        BanNetwork.sendChoice(player, actionId, "管理" + BanText.typeLabel(type),
            group.id(), ChoiceLayout.LEFT, options);
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
