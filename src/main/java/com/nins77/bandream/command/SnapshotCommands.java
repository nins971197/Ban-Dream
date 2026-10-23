package com.nins77.bandream.command;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.nins77.bandream.block.BakabanScanner;
import com.nins77.bandream.block.BakabanSnapshotStore;
import com.nins77.bandream.config.BanRules;
import com.nins77.bandream.item.BannedItemMarkerItem;
import com.nins77.bandream.item.ItemRedirectService;
import com.nins77.bandream.item.ItemSnapshotStore;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

final class SnapshotCommands {
    private static final int PAGE_SIZE = 10;

    private SnapshotCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> node() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("snapshot")
            .executes(context -> help(context.getSource()));
        root.then(Commands.literal("list")
            .then(Commands.literal("item")
                .executes(context -> listItems(context.getSource(), 1))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                    .executes(context -> listItems(context.getSource(),
                        IntegerArgumentType.getInteger(context, "page")))))
            .then(Commands.literal("block")
                .executes(context -> listBlocks(context.getSource(), 1))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                    .executes(context -> listBlocks(context.getSource(),
                        IntegerArgumentType.getInteger(context, "page"))))));
        root.then(Commands.literal("inspect")
            .then(Commands.literal("item")
                .then(Commands.argument("uuid", StringArgumentType.word())
                    .executes(context -> inspectItem(context.getSource(), context))))
            .then(Commands.literal("block")
                .then(Commands.argument("uuid", StringArgumentType.word())
                    .executes(context -> inspectBlock(context.getSource(), context)))));
        root.then(Commands.literal("delete")
            .then(Commands.literal("item")
                .then(Commands.argument("uuid", StringArgumentType.word())
                    .executes(context -> deleteItem(context.getSource(), context))))
            .then(Commands.literal("block")
                .then(Commands.argument("uuid", StringArgumentType.word())
                    .executes(context -> deleteBlock(context.getSource(), context)))));
        root.then(Commands.literal("restore")
            .then(Commands.literal("item")
                .then(Commands.argument("uuid", StringArgumentType.word())
                    .executes(context -> restoreItem(context.getSource(), context))))
            .then(Commands.literal("block")
                .then(Commands.argument("uuid", StringArgumentType.word())
                    .executes(context -> restoreBlock(context.getSource(), context)))));
        root.then(Commands.literal("orphaned").executes(context -> orphaned(context.getSource())));
        return root;
    }

    private static int listItems(CommandSourceStack source, int page) {
        ServerLevel level = source.getServer().overworld();
        ItemSnapshotStore store = ItemSnapshotStore.get(level);
        List<UUID> ids = store.ids();
        int currentPage = clampPage(page, ids.size());
        header(source, "物品快照", ids.size(), currentPage, ids.size());
        source.sendSystemMessage(Component.literal("读取失败: "
            + store.loadFailures() + " | 写入失败: " + store.writeFailures()
            + " | 删除失败: " + store.deleteFailures())
            .withStyle(ChatFormatting.DARK_GRAY));
        for (UUID id : page(ids, currentPage)) {
            ItemStack stack = store.get(id);
            source.sendSystemMessage(Component.literal(id + " | "
                + BanRules.itemId(stack.getItem()) + " x" + stack.getCount())
                .withStyle(ChatFormatting.GRAY));
        }
        return ids.size();
    }

    private static int listBlocks(CommandSourceStack source, int page) {
        ServerLevel level = source.getServer().overworld();
        BakabanSnapshotStore store = BakabanSnapshotStore.get(level);
        List<UUID> ids = store.ids();
        int currentPage = clampPage(page, ids.size());
        header(source, "方块快照", ids.size(), currentPage, ids.size());
        source.sendSystemMessage(Component.literal("读取失败: "
            + store.loadFailures() + " | 写入失败: " + store.writeFailures()
            + " | 删除失败: " + store.deleteFailures())
            .withStyle(ChatFormatting.DARK_GRAY));
        for (UUID id : page(ids, currentPage)) {
            BakabanSnapshotStore.StoredBlock stored = store.get(id);
            source.sendSystemMessage(Component.literal(id + " | "
                + BanRules.blockId(stored.state().getBlock()))
                .withStyle(ChatFormatting.GRAY));
        }
        return ids.size();
    }

    private static int inspectItem(CommandSourceStack source, CommandContext<CommandSourceStack> context) {
        UUID id = uuid(context);
        if (id == null) {
            return 0;
        }
        ItemStack stack = ItemSnapshotStore.get(source.getServer().overworld()).get(id);
        if (stack.isEmpty()) {
            source.sendFailure(Component.literal("没有该物品快照"));
            return 0;
        }
        source.sendSystemMessage(Component.literal("物品快照 " + id).withStyle(ChatFormatting.GOLD));
        source.sendSystemMessage(Component.literal("物品: " + BanRules.itemId(stack.getItem())
            + " x" + stack.getCount() + " | " + stack.getHoverName().getString())
            .withStyle(ChatFormatting.GRAY));
        return 1;
    }

    private static int inspectBlock(CommandSourceStack source, CommandContext<CommandSourceStack> context) {
        UUID id = uuid(context);
        if (id == null) {
            return 0;
        }
        BakabanSnapshotStore.StoredBlock stored =
            BakabanSnapshotStore.get(source.getServer().overworld()).get(id);
        if (stored == null) {
            source.sendFailure(Component.literal("没有该方块快照"));
            return 0;
        }
        source.sendSystemMessage(Component.literal("方块快照 " + id).withStyle(ChatFormatting.GOLD));
        source.sendSystemMessage(Component.literal("方块: "
            + BanRules.blockId(stored.state().getBlock())
            + " | 方块实体: " + (stored.blockEntity() == null ? "无" : "有"))
            .withStyle(ChatFormatting.GRAY));
        return 1;
    }

    private static int deleteItem(CommandSourceStack source, CommandContext<CommandSourceStack> context) {
        UUID id = uuid(context);
        if (id == null) {
            return 0;
        }
        ItemSnapshotStore store = ItemSnapshotStore.get(source.getServer().overworld());
        boolean existed = store.exists(id);
        boolean removed = store.delete(id);
        source.sendSystemMessage(Component.literal(removed
            ? "已删除物品快照 " + id
            : existed ? "删除物品快照失败，请检查文件权限" : "没有该物品快照")
            .withStyle(removed || !existed
                ? ChatFormatting.YELLOW : ChatFormatting.RED));
        return removed ? 1 : 0;
    }

    private static int deleteBlock(CommandSourceStack source, CommandContext<CommandSourceStack> context) {
        UUID id = uuid(context);
        if (id == null) {
            return 0;
        }
        BakabanSnapshotStore store = BakabanSnapshotStore.get(source.getServer().overworld());
        boolean existed = store.exists(id);
        boolean removed = store.delete(id);
        source.sendSystemMessage(Component.literal(removed
            ? "已删除方块快照 " + id
            : existed ? "删除方块快照失败，请检查文件权限" : "没有该方块快照")
            .withStyle(removed || !existed
                ? ChatFormatting.YELLOW : ChatFormatting.RED));
        return removed ? 1 : 0;
    }

    private static int restoreItem(CommandSourceStack source, CommandContext<CommandSourceStack> context) {
        UUID id = uuid(context);
        if (id == null) {
            return 0;
        }
        MinecraftServer server = source.getServer();
        ServerLevel overworld = server.overworld();
        if (ItemSnapshotStore.get(overworld).get(id).isEmpty()) {
            source.sendFailure(Component.literal("没有该物品快照"));
            return 0;
        }
        for (ServerLevel level : server.getAllLevels()) {
            List<Entity> entities = new ArrayList<>();
            level.getAllEntities().forEach(entities::add);
            for (Entity entity : entities) {
                if (entity instanceof ItemEntity itemEntity
                    && id.equals(markerId(itemEntity.getItem()))) {
                    ItemStack restored = ItemRedirectService.restore(
                        itemEntity.getItem(), level, level.dimension().location(), false);
                    if (restored != null) {
                        itemEntity.setItem(restored);
                        itemEntity.lifespan = 6000;
                        itemEntity.setInvulnerable(false);
                        source.sendSystemMessage(Component.literal("已恢复物品快照 " + id)
                            .withStyle(ChatFormatting.GREEN));
                        return 1;
                    }
                }
            }
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (restorePlayerStack(player, id)) {
                source.sendSystemMessage(Component.literal("已恢复玩家物品快照 " + id)
                    .withStyle(ChatFormatting.GREEN));
                return 1;
            }
        }
        source.sendFailure(Component.literal("未找到该快照对应的物品或方块，且不能凭空恢复"));
        return 0;
    }

    private static int restoreBlock(CommandSourceStack source, CommandContext<CommandSourceStack> context) {
        UUID id = uuid(context);
        if (id == null) {
            return 0;
        }
        if (BakabanSnapshotStore.get(source.getServer().overworld()).get(id) == null) {
            source.sendFailure(Component.literal("没有该方块快照"));
            return 0;
        }
        if (BakabanScanner.restoreById(source.getServer(), id)) {
            source.sendSystemMessage(Component.literal("已恢复方块快照 " + id)
                .withStyle(ChatFormatting.GREEN));
            return 1;
        }
        source.sendFailure(Component.literal("未找到该快照对应的已加载 bakaban，无法凭空恢复"));
        return 0;
    }

    private static boolean restorePlayerStack(ServerPlayer player, UUID id) {
        if (restoreStacks(player.getInventory().items, player, id)
            || restoreStacks(player.getInventory().armor, player, id)
            || restoreStacks(player.getInventory().offhand, player, id)) {
            return true;
        }
        if (id.equals(markerId(player.containerMenu.getCarried()))) {
            ItemStack restored = ItemRedirectService.restore(
                player.containerMenu.getCarried(), (ServerLevel) player.level(),
                player.level().dimension().location(), false);
            if (restored != null) {
                player.containerMenu.setCarried(restored);
                return true;
            }
        }
        for (Slot slot : player.containerMenu.slots) {
            for (int index = 0; index < slot.container.getContainerSize(); index++) {
                ItemStack stack = slot.container.getItem(index);
                if (!id.equals(markerId(stack))) {
                    continue;
                }
                ItemStack restored = ItemRedirectService.restore(
                    stack, (ServerLevel) player.level(),
                    player.level().dimension().location(), false);
                if (restored != null) {
                    slot.container.setItem(index, restored);
                    slot.container.setChanged();
                    player.containerMenu.broadcastChanges();
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean restoreStacks(
        NonNullList<ItemStack> stacks,
        ServerPlayer player,
        UUID id
    ) {
        for (int index = 0; index < stacks.size(); index++) {
            ItemStack stack = stacks.get(index);
            if (!id.equals(markerId(stack))) {
                continue;
            }
            ItemStack restored = ItemRedirectService.restore(
                stack, (ServerLevel) player.level(),
                player.level().dimension().location(), false);
            if (restored != null) {
                stacks.set(index, restored);
                player.getInventory().setChanged();
                return true;
            }
        }
        return false;
    }

    private static UUID markerId(ItemStack stack) {
        return BannedItemMarkerItem.snapshotId(stack);
    }
    private static int orphaned(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        Set<UUID> itemIds = new HashSet<>();
        Set<UUID> blockIds = BakabanScanner.referencedSnapshotIds(server);
        for (ServerLevel level : server.getAllLevels()) {
            List<Entity> entities = new ArrayList<>();
            level.getAllEntities().forEach(entities::add);
            for (Entity entity : entities) {
                if (entity instanceof ItemEntity itemEntity) {
                    addMarker(itemIds, itemEntity.getItem());
                }
            }
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            addMarkers(itemIds, player.getInventory().items);
            addMarkers(itemIds, player.getInventory().armor);
            addMarkers(itemIds, player.getInventory().offhand);
            addMarker(itemIds, player.containerMenu.getCarried());
            for (Slot slot : player.containerMenu.slots) {
                for (int index = 0; index < slot.container.getContainerSize(); index++) {
                    addMarker(itemIds, slot.container.getItem(index));
                }
            }
        }
        reportOrphans(source, "物品", new HashSet<>(ItemSnapshotStore.get(server.overworld()).ids()), itemIds);
        reportOrphans(source, "方块", new HashSet<>(BakabanSnapshotStore.get(server.overworld()).ids()), blockIds);
        source.sendSystemMessage(Component.literal("这是当前加载范围的疑似列表，不代表全量审计；确认无效后再使用 snapshot delete")
            .withStyle(ChatFormatting.DARK_GRAY));
        return 1;
    }

    private static void reportOrphans(CommandSourceStack source, String label,
        Set<UUID> snapshots, Set<UUID> referenced) {
        List<UUID> orphans = snapshots.stream().filter(id -> !referenced.contains(id))
            .sorted((left, right) -> left.toString().compareTo(right.toString())).toList();
        source.sendSystemMessage(Component.literal(label + "疑似孤立快照: " + orphans.size())
            .withStyle(orphans.isEmpty() ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        for (UUID id : orphans.stream().limit(PAGE_SIZE).toList()) {
            source.sendSystemMessage(Component.literal(id.toString()).withStyle(ChatFormatting.GRAY));
        }
    }

    private static void addMarkers(Set<UUID> ids, NonNullList<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            addMarker(ids, stack);
        }
    }

    private static void addMarker(Set<UUID> ids, ItemStack stack) {
        if (ItemRedirectService.isMarker(stack)) {
            ids.add(BannedItemMarkerItem.snapshotId(stack));
        }
    }

    private static void header(CommandSourceStack source, String label, int total, int page, int pageCount) {
        source.sendSystemMessage(Component.literal("==== Ban Dream · " + label + " (" + total + ") ====")
            .withStyle(ChatFormatting.GOLD));
        source.sendSystemMessage(Component.literal("第 " + page + " / " + pages(pageCount) + " 页")
            .withStyle(ChatFormatting.DARK_GRAY));
    }

    private static List<UUID> page(List<UUID> ids, int page) {
        long start = (long) (page - 1) * PAGE_SIZE;
        if (start >= ids.size()) {
            return List.of();
        }
        int from = (int) start;
        int to = Math.min(ids.size(), from + PAGE_SIZE);
        return ids.subList(from, to);
    }

    private static int pages(int size) {
        return Math.max(1, (size + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private static int clampPage(int page, int size) {
        return Math.min(page, pages(size));
    }

    private static UUID uuid(CommandContext<CommandSourceStack> context) {
        try {
            return UUID.fromString(StringArgumentType.getString(context, "uuid"));
        } catch (IllegalArgumentException exception) {
            context.getSource().sendFailure(Component.literal("UUID 格式无效"));
            return null;
        }
    }

    private static int help(CommandSourceStack source) {
        source.sendSystemMessage(Component.literal("==== Ban Dream · 快照审计 ====")
            .withStyle(ChatFormatting.GOLD));
        source.sendSystemMessage(Component.literal("/bbban snapshot list item|block [页码]")
            .withStyle(ChatFormatting.GRAY));
        source.sendSystemMessage(Component.literal("/bbban snapshot inspect item|block <UUID>")
            .withStyle(ChatFormatting.GRAY));
        source.sendSystemMessage(Component.literal("/bbban snapshot delete item|block <UUID>")
            .withStyle(ChatFormatting.GRAY));
        source.sendSystemMessage(Component.literal("/bbban snapshot restore item|block <UUID>")
            .withStyle(ChatFormatting.GRAY));
        source.sendSystemMessage(Component.literal("/bbban snapshot orphaned")
            .withStyle(ChatFormatting.GRAY));
        return 1;
    }
}
