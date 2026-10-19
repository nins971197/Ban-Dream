package com.nins77.bandream.command;

import static com.nins77.bandream.command.BanCommandSupport.DIMENSION_IDS;
import static com.nins77.bandream.command.BanCommandSupport.EFFECT_IDS;
import static com.nins77.bandream.command.BanCommandSupport.ENCHANTMENT_IDS;
import static com.nins77.bandream.command.BanCommandSupport.ENTITY_IDS;
import static com.nins77.bandream.command.BanCommandSupport.BLOCK_IDS;
import static com.nins77.bandream.command.BanCommandSupport.BLOCK_MODES;
import static com.nins77.bandream.command.BanCommandSupport.ITEM_IDS;
import static com.nins77.bandream.command.BanCommandSupport.JEI_MODES;
import static com.nins77.bandream.command.BanCommandSupport.STRUCTURE_IDS;
import static com.nins77.bandream.command.BanCommandSupport.TRIGGERS;
import static com.nins77.bandream.command.BanCommandSupport.UNKNOWN_ID;
import static com.nins77.bandream.command.BanCommandSupport.requireKnown;

import java.util.List;
import java.util.Locale;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.nins77.bandream.action.PendingBanManager;
import com.nins77.bandream.config.BanEntry;
import com.nins77.bandream.config.BanRules;
import com.nins77.bandream.config.BanScope;
import com.nins77.bandream.config.BanTrigger;
import com.nins77.bandream.config.BanType;
import com.nins77.bandream.config.BlockBanMode;
import com.nins77.bandream.enforce.BanEnforcement;
import com.nins77.bandream.gui.BanListMenu;
import com.nins77.bandream.item.BanItems;
import com.nins77.bandream.text.BanText;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;

public final class BanCommands {
    private static final SimpleCommandExceptionType ERROR_NOT_PLAYER = new SimpleCommandExceptionType(
        Component.literal("该指令只能由玩家执行"));
    private static final SimpleCommandExceptionType ERROR_EMPTY_HAND = new SimpleCommandExceptionType(
        Component.literal("主手没有物品，无法执行手持物品封禁"));
    private BanCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("bbban")
            .requires(source -> source.hasPermission(Commands.LEVEL_ADMINS))
            .executes(BanCommands::help);
        root.then(banRoot());
        root.then(unbanRoot());
        root.then(listRoot());
        root.then(WhitelistCommands.node());
        root.then(Commands.literal("confirm").executes(BanCommands::confirmPending));
        root.then(Commands.literal("cancel").executes(BanCommands::cancelPending));
        root.then(Commands.literal("stick").executes(BanCommands::giveBanStick));
        root.then(Commands.literal("reload").executes(BanCommands::reload));
        event.getDispatcher().register(root);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> banRoot() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("ban");
        root.then(itemBan());
        root.then(simpleBan("entity", BanType.ENTITY, ENTITY_IDS));
        root.then(simpleBan("structure", BanType.STRUCTURE, STRUCTURE_IDS));
        root.then(simpleBan("enchantment", BanType.ENCHANTMENT, ENCHANTMENT_IDS));
        root.then(simpleBan("effect", BanType.EFFECT, EFFECT_IDS));
        root.then(blockBan());
        root.then(dimensionBan());
        root.then(nbtBan());
        return root;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> itemBan() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("item");
        addItemOptions(root, BanCommands::banHeldItem, BanScope.GLOBAL);
        root.then(itemGlobal(BanCommands::banHeldItem));
        root.then(itemDimension(BanCommands::banHeldItem));

        var id = Commands.argument("id", ResourceLocationArgument.id()).suggests(ITEM_IDS);
        addItemOptions(id, BanCommands::banItem, BanScope.GLOBAL);
        id.then(itemGlobal(BanCommands::banItem));
        id.then(itemDimension(BanCommands::banItem));
        root.then(id);
        return root;
    }

    private interface ItemBanCommand {
        int ban(CommandContext<CommandSourceStack> context, BanScope scope, boolean jeiVisible, BanTrigger trigger)
            throws CommandSyntaxException;
    }

    private static void addItemOptions(
        ArgumentBuilder<CommandSourceStack, ?> node,
        ItemBanCommand action,
        BanScope defaultScope
    ) {
        node.executes(context -> action.ban(context, defaultScope, true, BanTrigger.INVENTORY));
        node.then(Commands.literal("trigger").then(Commands.argument("triggerMode", StringArgumentType.word())
            .suggests(TRIGGERS)
            .executes(context -> action.ban(context, defaultScope, true,
                parseTrigger(StringArgumentType.getString(context, "triggerMode"))))));

        var jeiMode = Commands.argument("jeiMode", StringArgumentType.word())
            .suggests(JEI_MODES)
            .executes(context -> action.ban(context, defaultScope,
                parseJei(StringArgumentType.getString(context, "jeiMode")), BanTrigger.INVENTORY))
            .then(Commands.literal("trigger").then(Commands.argument("triggerMode", StringArgumentType.word())
                .suggests(TRIGGERS)
                .executes(context -> action.ban(context, defaultScope,
                    parseJei(StringArgumentType.getString(context, "jeiMode")),
                    parseTrigger(StringArgumentType.getString(context, "triggerMode"))))));
        node.then(Commands.literal("jei").then(jeiMode));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> itemGlobal(ItemBanCommand action) {
        LiteralArgumentBuilder<CommandSourceStack> global = Commands.literal("global");
        addItemOptions(global, action, BanScope.GLOBAL);
        return global;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> itemDimension(ItemBanCommand action) {
        var dimension = Commands.argument("dimension", ResourceLocationArgument.id()).suggests(DIMENSION_IDS);
        addItemOptions(dimension, action, BanScope.DIMENSION);
        return Commands.literal("in").then(dimension);
    }

    private static int banHeldItem(
        CommandContext<CommandSourceStack> context,
        BanScope scope,
        boolean jeiVisible,
        BanTrigger trigger
    ) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(context.getSource());
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            throw ERROR_EMPTY_HAND.create();
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(held.getItem());
        return applyItemBan(context, id, scope, jeiVisible, trigger);
    }

    private static int banItem(
        CommandContext<CommandSourceStack> context,
        BanScope scope,
        boolean jeiVisible,
        BanTrigger trigger
    ) throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(context, "id");
        requireKnown(context.getSource(), BanType.ITEM, id.toString());
        return applyItemBan(context, id, scope, jeiVisible, trigger);
    }

    private static int applyItemBan(
        CommandContext<CommandSourceStack> context,
        ResourceLocation id,
        BanScope scope,
        boolean jeiVisible,
        BanTrigger trigger
    ) throws CommandSyntaxException {
        if (id == null) {
            throw UNKNOWN_ID.create("unknown");
        }
        BanEntry entry = new BanEntry(BanType.ITEM, id.toString(), scope, dimension(context, scope));
        entry.jeiVisible = jeiVisible;
        entry.trigger = trigger;
        return applyBan(context.getSource(), entry);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> simpleBan(
        String literal,
        BanType type,
        SuggestionProvider<CommandSourceStack> suggestions
    ) {
        var id = Commands.argument("id", ResourceLocationArgument.id()).suggests(suggestions);
        id.executes(context -> banSimple(context, type, BanScope.GLOBAL));
        id.then(Commands.literal("global").executes(context -> banSimple(context, type, BanScope.GLOBAL)));
        var dimension = Commands.argument("dimension", ResourceLocationArgument.id()).suggests(DIMENSION_IDS)
            .executes(context -> banSimple(context, type, BanScope.DIMENSION));
        id.then(Commands.literal("in").then(dimension));
        return Commands.literal(literal).then(id);
    }

    private static int banSimple(
        CommandContext<CommandSourceStack> context,
        BanType type,
        BanScope scope
    ) throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(context, "id");
        requireKnown(context.getSource(), type, id.toString());
        BanEntry entry = new BanEntry(type, id.toString(), scope, dimension(context, scope));
        return applyBan(context.getSource(), entry);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> dimensionBan() {
        return Commands.literal("dimension").then(
            Commands.argument("id", ResourceLocationArgument.id())
                .suggests(DIMENSION_IDS)
                .executes(context -> {
                    ResourceLocation id = ResourceLocationArgument.getId(context, "id");
                    requireKnown(context.getSource(), BanType.DIMENSION, id.toString());
                    return applyBan(context.getSource(),
                        new BanEntry(BanType.DIMENSION, id.toString(), BanScope.GLOBAL, null));
                }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> blockBan() {
        var id = Commands.argument("id", ResourceLocationArgument.id()).suggests(BLOCK_IDS);
        id.executes(context -> banBlock(context, BanScope.GLOBAL, BlockBanMode.FULL));

        LiteralArgumentBuilder<CommandSourceStack> global = Commands.literal("global")
            .executes(context -> banBlock(context, BanScope.GLOBAL, BlockBanMode.FULL));
        global.then(blockModeNode(BanScope.GLOBAL));

        var dimension = Commands.argument("dimension", ResourceLocationArgument.id())
            .suggests(DIMENSION_IDS)
            .executes(context -> banBlock(context, BanScope.DIMENSION, BlockBanMode.FULL));
        dimension.then(blockModeNode(BanScope.DIMENSION));

        id.then(global);
        id.then(Commands.literal("in").then(dimension));
        id.then(blockModeNode(BanScope.GLOBAL));
        return Commands.literal("block").then(id);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> blockModeNode(BanScope scope) {
        return Commands.literal("mode").then(Commands.argument("blockMode", StringArgumentType.word())
            .suggests(BLOCK_MODES)
            .executes(context -> banBlock(context, scope,
                parseBlockMode(StringArgumentType.getString(context, "blockMode")))));
    }

    private static int banBlock(
        CommandContext<CommandSourceStack> context,
        BanScope scope,
        BlockBanMode mode
    ) throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(context, "id");
        requireKnown(context.getSource(), BanType.BLOCK, id.toString());
        BanEntry entry = new BanEntry(BanType.BLOCK, id.toString(), scope, dimension(context, scope));
        entry.blockMode = mode;
        return applyBan(context.getSource(), entry);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> nbtBan() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("nbt");
        root.then(nbtTarget("*", true));
        root.then(nbtTarget(null, false));
        return root;
    }

    private static ArgumentBuilder<CommandSourceStack, ?> nbtTarget(String fixedId, boolean wildcard) {
        if (wildcard) {
            return Commands.literal(fixedId).then(nbtPathNode(fixedId));
        }
        return Commands.argument("id", ResourceLocationArgument.id())
            .suggests(ITEM_IDS)
            .then(nbtPathNode(null));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> nbtPathNode(String fixedId) {
        var value = Commands.argument("value", StringArgumentType.string());
        addNbtScopes(value, fixedId);
        return Commands.argument("path", StringArgumentType.string()).then(value);
    }

    private static void addNbtScopes(ArgumentBuilder<CommandSourceStack, ?> valueNode, String fixedId) {
        valueNode.executes(context -> banNbt(context, fixedId, BanScope.GLOBAL, BanTrigger.INVENTORY));
        valueNode.then(nbtTriggerOnly(fixedId, BanScope.GLOBAL));
        var global = Commands.literal("global")
            .executes(context -> banNbt(context, fixedId, BanScope.GLOBAL, BanTrigger.INVENTORY));
        global.then(nbtTriggerOnly(fixedId, BanScope.GLOBAL));

        var dimension = Commands.argument("dimension", ResourceLocationArgument.id())
            .suggests(DIMENSION_IDS)
            .executes(context -> banNbt(context, fixedId, BanScope.DIMENSION, BanTrigger.INVENTORY));
        dimension.then(nbtTriggerOnly(fixedId, BanScope.DIMENSION));

        valueNode.then(global).then(Commands.literal("in").then(dimension));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> nbtTriggerOnly(String fixedId, BanScope scope) {
        return Commands.literal("trigger").then(Commands.argument("triggerMode", StringArgumentType.word())
            .suggests(TRIGGERS)
            .executes(context -> banNbt(context, fixedId, scope,
                parseTrigger(StringArgumentType.getString(context, "triggerMode")))));
    }

    private static int banNbt(
        CommandContext<CommandSourceStack> context,
        String fixedId,
        BanScope scope,
        BanTrigger trigger
    ) throws CommandSyntaxException {
        String id;
        if (fixedId != null) {
            id = fixedId;
        } else {
            id = ResourceLocationArgument.getId(context, "id").toString();
            requireKnown(context.getSource(), BanType.ITEM, id);
        }
        String path = StringArgumentType.getString(context, "path");
        String value = StringArgumentType.getString(context, "value");
        BanEntry entry = new BanEntry(BanType.NBT, id, scope, dimension(context, scope));
        entry.path = path;
        entry.value = value;
        entry.trigger = trigger;
        return applyBan(context.getSource(), entry);
    }

    private static int applyBan(CommandSourceStack source, BanEntry entry) throws CommandSyntaxException {
        try {
            boolean replaced = BanRules.ban(entry);
            BanEnforcement.afterRulesChanged(source.getServer(), entry.type == BanType.ENTITY,
                entry.type == BanType.BLOCK);
            source.sendSuccess(() -> Component.literal(
                (replaced ? "已更新封禁: " : "已封禁: ") + BanText.line(entry))
                .withStyle(ChatFormatting.GREEN), true);
            return Command.SINGLE_SUCCESS;
        } catch (Exception exception) {
            throw new SimpleCommandExceptionType(
                Component.literal("写入封禁配置失败: " + exception.getMessage())).create();
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> unbanRoot() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("unban");
        root.then(simpleUnban("item", BanType.ITEM, ITEM_IDS));
        root.then(simpleUnban("entity", BanType.ENTITY, ENTITY_IDS));
        root.then(simpleUnban("structure", BanType.STRUCTURE, STRUCTURE_IDS));
        root.then(simpleUnban("enchantment", BanType.ENCHANTMENT, ENCHANTMENT_IDS));
        root.then(simpleUnban("effect", BanType.EFFECT, EFFECT_IDS));
        root.then(simpleUnban("block", BanType.BLOCK, BLOCK_IDS));
        root.then(dimensionUnban());
        root.then(nbtUnban());
        return root;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> simpleUnban(
        String literal,
        BanType type,
        SuggestionProvider<CommandSourceStack> suggestions
    ) {
        var id = Commands.argument("id", ResourceLocationArgument.id())
            .suggests(suggestions)
            .executes(context -> unbanSimple(context, type, null, null));
        id.then(Commands.literal("global").executes(context -> unbanSimple(context, type, BanScope.GLOBAL, null)));
        id.then(Commands.literal("in").then(Commands.argument("dimension", ResourceLocationArgument.id())
            .suggests(DIMENSION_IDS)
            .executes(context -> unbanSimple(context, type, BanScope.DIMENSION,
                ResourceLocationArgument.getId(context, "dimension").toString()))));
        return Commands.literal(literal).then(id);
    }

    private static int unbanSimple(
        CommandContext<CommandSourceStack> context,
        BanType type,
        BanScope scope,
        String dimension
    ) throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(context, "id");
        try {
            int removed = BanRules.unban(type, id.toString(), scope, dimension);
            finishUnban(context.getSource(), removed, type, id.toString());
            return removed;
        } catch (Exception exception) {
            throw new SimpleCommandExceptionType(
                Component.literal("写入封禁配置失败: " + exception.getMessage())).create();
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> dimensionUnban() {
        return Commands.literal("dimension").then(Commands.argument("id", ResourceLocationArgument.id())
            .suggests(DIMENSION_IDS)
            .executes(context -> {
                ResourceLocation id = ResourceLocationArgument.getId(context, "id");
                try {
                    int removed = BanRules.unban(BanType.DIMENSION, id.toString(), null, null);
                    finishUnban(context.getSource(), removed, BanType.DIMENSION, id.toString());
                    return removed;
                } catch (Exception exception) {
                    throw new SimpleCommandExceptionType(
                        Component.literal("写入封禁配置失败: " + exception.getMessage())).create();
                }
            }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> nbtUnban() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("nbt");
        root.then(nbtUnbanWildcard("*"));
        var id = Commands.argument("id", ResourceLocationArgument.id()).suggests(ITEM_IDS)
            .executes(context -> unbanNbt(context, ResourceLocationArgument.getId(context, "id").toString(), null, null));
        var path = Commands.argument("path", StringArgumentType.string())
            .executes(context -> unbanNbt(context,
                ResourceLocationArgument.getId(context, "id").toString(),
                StringArgumentType.getString(context, "path"), null));
        path.then(Commands.literal("value").then(Commands.argument("value", StringArgumentType.string())
            .executes(context -> unbanNbt(context,
                ResourceLocationArgument.getId(context, "id").toString(),
                StringArgumentType.getString(context, "path"),
                StringArgumentType.getString(context, "value")))));
        id.then(Commands.literal("path").then(path));
        root.then(id);
        return root;
    }

    private static ArgumentBuilder<CommandSourceStack, ?> nbtUnbanWildcard(String itemId) {
        var base = Commands.literal(itemId)
            .executes(context -> unbanNbt(context, itemId, null, null));
        var path = Commands.argument("path", StringArgumentType.string())
            .executes(context -> unbanNbt(context, itemId,
                StringArgumentType.getString(context, "path"), null));
        path.then(Commands.literal("value").then(Commands.argument("value", StringArgumentType.string())
            .executes(context -> unbanNbt(context, itemId,
                StringArgumentType.getString(context, "path"),
                StringArgumentType.getString(context, "value")))));
        base.then(Commands.literal("path").then(path));
        return base;
    }

    private static int unbanNbt(
        CommandContext<CommandSourceStack> context,
        String id,
        String path,
        String value
    ) throws CommandSyntaxException {
        try {
            int removed = BanRules.unbanNbt(id, path, value);
            finishUnban(context.getSource(), removed, BanType.NBT, id);
            return removed;
        } catch (Exception exception) {
            throw new SimpleCommandExceptionType(
                Component.literal("写入封禁配置失败: " + exception.getMessage())).create();
        }
    }

    private static void finishUnban(CommandSourceStack source, int removed, BanType type, String id) {
        if (removed == 0) {
            source.sendFailure(Component.literal("没有匹配的封禁记录: " + BanText.typeLabel(type) + " " + id));
        } else {
            BanEnforcement.afterRulesChanged(source.getServer(), type == BanType.ENTITY,
                type == BanType.BLOCK);
            source.sendSuccess(() -> Component.literal(
                "已解除 " + removed + " 条封禁: " + BanText.typeLabel(type) + " " + id)
                .withStyle(ChatFormatting.GREEN), true);
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> listRoot() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("list");
        for (BanType type : BanType.values()) {
            var typeNode = Commands.literal(type.id())
                .executes(context -> list(context.getSource(), type, false))
                .then(Commands.literal("book").executes(context -> list(context.getSource(), type, true)));
            root.then(typeNode);
        }
        return root;
    }

    private static int list(CommandSourceStack source, BanType type, boolean book) throws CommandSyntaxException {
        List<BanEntry> entries = BanRules.entries(type);
        ServerPlayer player = source.getPlayer();
        if (!book && player != null && (type == BanType.ITEM || type == BanType.BLOCK)) {
            BanListMenu.open(player, type);
            source.sendSuccess(() -> Component.literal(
                "已打开封禁" + BanText.typeLabel(type) + "列表，共 " + entries.size() + " 条记录")
                .withStyle(ChatFormatting.AQUA), false);
            return Math.max(1, entries.size());
        }
        if (book) {
            ServerPlayer bookPlayer = requirePlayer(source);
            List<ItemStack> written = BanBookFactory.createVolumes(type, entries);
            for (ItemStack stack : written) {
                if (!bookPlayer.getInventory().add(stack)) {
                    bookPlayer.drop(stack, false);
                }
            }
            source.sendSuccess(() -> Component.literal(
                "已生成" + BanText.typeLabel(type) + "封禁清单书 "
                    + written.size() + " 本，共 " + entries.size() + " 条")
                .withStyle(ChatFormatting.GREEN), false);
            return Math.max(1, entries.size());
        }

        source.sendSystemMessage(Component.literal(
            "==== Ban Dream · " + BanText.typeLabel(type) + "封禁名单 (" + entries.size() + ") ====")
            .withStyle(ChatFormatting.GOLD));
        if (entries.isEmpty()) {
            source.sendSystemMessage(Component.literal("没有封禁记录").withStyle(ChatFormatting.GRAY));
        } else {
            for (BanEntry entry : entries) {
                source.sendSystemMessage(Component.literal(BanText.line(entry)).withStyle(ChatFormatting.GRAY));
            }
        }
        return Math.max(1, entries.size());
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            BanRules.load();
            BanEnforcement.afterRulesChanged(source.getServer());
            source.sendSuccess(() -> Component.literal(
                "已重载 " + BanRules.count() + " 条封禁规则: " + BanRules.configPath())
                .withStyle(ChatFormatting.GREEN), true);
            return BanRules.count();
        } catch (Exception exception) {
            source.sendFailure(Component.literal("重载失败: " + exception.getMessage()));
            return 0;
        }
    }

    private static int confirmPending(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = requirePlayer(source);
        PendingBanManager.PendingBan pending = PendingBanManager.get(player);
        if (pending == null) {
            source.sendFailure(Component.literal("没有待确认的封禁，或确认已超过 60 秒"));
            return 0;
        }
        try {
            int count = BanRules.banAll(pending.entries());
            PendingBanManager.clear(player);
            BanEnforcement.afterRulesChanged(source.getServer(),
                pending.entries().stream().anyMatch(entry -> entry.type == BanType.ENTITY),
                pending.entries().stream().anyMatch(entry -> entry.type == BanType.BLOCK));
            source.sendSuccess(() -> Component.literal(
                "已确认封禁：" + pending.summary() + "（写入 " + count + " 条规则）")
                .withStyle(ChatFormatting.GREEN), true);
            return count;
        } catch (Exception exception) {
            throw new SimpleCommandExceptionType(
                Component.literal("写入封禁配置失败: " + exception.getMessage())).create();
        }
    }

    private static int cancelPending(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(context.getSource());
        PendingBanManager.clear(player);
        context.getSource().sendSuccess(() -> Component.literal("已取消待确认的封禁")
            .withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int giveBanStick(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = requirePlayer(source);
        ItemStack stack = new ItemStack(BanItems.BAN_STICK.get());
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        source.sendSuccess(() -> Component.literal("已获得封禁棒")
            .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int help(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSystemMessage(Component.literal("==== Ban Dream /bbban ====").withStyle(ChatFormatting.GOLD));
        source.sendSystemMessage(Component.literal(
            "/bbban ban item [物品] [global|in <维度>] [jei show|hide] [trigger inventory|use]")
            .withStyle(ChatFormatting.GRAY));
        source.sendSystemMessage(Component.literal(
            "/bbban ban entity|structure|enchantment|effect <ID> [global|in <维度>]")
            .withStyle(ChatFormatting.GRAY));
        source.sendSystemMessage(Component.literal(
            "/bbban ban block <方块ID> [global|in <维度>] [mode interact|full]")
            .withStyle(ChatFormatting.GRAY));
        source.sendSystemMessage(Component.literal(
            "/bbban ban dimension <维度>").withStyle(ChatFormatting.GRAY));
        source.sendSystemMessage(Component.literal(
            "/bbban ban nbt <物品|*> <NBT路径> <SNBT值|*> [global|in <维度>] [trigger inventory|use]")
            .withStyle(ChatFormatting.GRAY));
        source.sendSystemMessage(Component.literal(
            "不写物品 ID 时封禁主手物品；不写范围时默认全局")
            .withStyle(ChatFormatting.DARK_GRAY));
        source.sendSystemMessage(Component.literal(
            "/bbban unban <类型> <ID> [global|in <维度>]").withStyle(ChatFormatting.GRAY));
        source.sendSystemMessage(Component.literal(
            "/bbban list <类型> [book]  — 物品/方块默认打开分页 GUI，book 生成成书")
            .withStyle(ChatFormatting.GRAY));
        source.sendSystemMessage(Component.literal(
            "/bbban whitelist add|remove|list <玩家>  — 豁免物品/效果/维度限制")
            .withStyle(ChatFormatting.GRAY));
        source.sendSystemMessage(Component.literal(
            "/bbban stick  — 获得封禁棒；左键生物，潜行左键木桶")
            .withStyle(ChatFormatting.GRAY));
        source.sendSystemMessage(Component.literal(
            "/bbban confirm  — 确认待处理封禁；/bbban cancel 取消")
            .withStyle(ChatFormatting.GRAY));
        source.sendSystemMessage(Component.literal(
            "/bbban reload").withStyle(ChatFormatting.GRAY));
        return 1;
    }

    private static String dimension(CommandContext<CommandSourceStack> context, BanScope scope) {
        if (scope != BanScope.DIMENSION) {
            return null;
        }
        return ResourceLocationArgument.getId(context, "dimension").toString();
    }

    private static boolean parseJei(String value) {
        return !"hide".equals(value.toLowerCase(Locale.ROOT));
    }

    private static BanTrigger parseTrigger(String value) {
        return "use".equals(value.toLowerCase(Locale.ROOT)) ? BanTrigger.USE : BanTrigger.INVENTORY;
    }

    private static BlockBanMode parseBlockMode(String value) {
        return BlockBanMode.byId(value).orElse(BlockBanMode.FULL);
    }

    private static ServerPlayer requirePlayer(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            throw ERROR_NOT_PLAYER.create();
        }
        return player;
    }

}
