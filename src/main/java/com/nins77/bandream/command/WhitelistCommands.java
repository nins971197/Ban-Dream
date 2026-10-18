package com.nins77.bandream.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.nins77.bandream.config.BanRules;
import com.nins77.bandream.config.PlayerWhitelistEntry;
import com.nins77.bandream.enforce.BanEnforcement;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

final class WhitelistCommands {
    private WhitelistCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> node() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("whitelist")
            .executes(context -> help(context.getSource()));
        root.then(Commands.literal("add").then(Commands.argument("player", StringArgumentType.word())
            .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                context.getSource().getOnlinePlayerNames(), builder))
            .executes(WhitelistCommands::add)));
        root.then(Commands.literal("remove").then(Commands.argument("player", StringArgumentType.word())
            .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                removalSuggestions(context.getSource()), builder))
            .executes(WhitelistCommands::remove)));
        root.then(Commands.literal("list").executes(context -> list(context.getSource())));
        return root;
    }

    private static int add(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String input = StringArgumentType.getString(context, "player");
        Optional<UUID> directUuid = parseUuid(input);

        try {
            boolean replaced;
            String display;
            if (directUuid.isPresent()) {
                UUID uuid = directUuid.get();
                replaced = BanRules.addPlayerToWhitelist(null, uuid);
                display = uuid.toString();
            } else {
                GameProfile profile = findProfile(source, input);
                replaced = BanRules.addPlayerToWhitelist(
                    profile != null ? profile.getName() : input,
                    profile != null ? profile.getId() : null);
                display = profile != null ? profile.getName() : input;
            }
            BanEnforcement.afterRulesChanged(source.getServer(), false);
            source.sendSuccess(() -> Component.literal(
                (replaced ? "已更新玩家白名单: " : "已添加玩家白名单: ") + display)
                .withStyle(ChatFormatting.GREEN), true);
            return 1;
        } catch (Exception exception) {
            throw new SimpleCommandExceptionType(
                Component.literal("写入玩家白名单失败: " + exception.getMessage())).create();
        }
    }

    private static int remove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String input = StringArgumentType.getString(context, "player");
        try {
            int removed = BanRules.removePlayerFromWhitelist(input);
            if (removed == 0) {
                source.sendFailure(Component.literal("玩家白名单中没有: " + input));
                return 0;
            }
            BanEnforcement.afterRulesChanged(source.getServer(), false);
            source.sendSuccess(() -> Component.literal(
                "已移除 " + removed + " 条玩家白名单记录: " + input)
                .withStyle(ChatFormatting.GREEN), true);
            return removed;
        } catch (Exception exception) {
            throw new SimpleCommandExceptionType(
                Component.literal("写入玩家白名单失败: " + exception.getMessage())).create();
        }
    }

    private static int list(CommandSourceStack source) {
        List<PlayerWhitelistEntry> entries = BanRules.playerWhitelistEntries();
        source.sendSystemMessage(Component.literal(
            "==== Ban Dream · 玩家白名单 (" + entries.size() + ") ====")
            .withStyle(ChatFormatting.GOLD));
        if (entries.isEmpty()) {
            source.sendSystemMessage(Component.literal("没有玩家白名单记录")
                .withStyle(ChatFormatting.GRAY));
        } else {
            for (PlayerWhitelistEntry entry : entries) {
                source.sendSystemMessage(Component.literal(entry.display())
                    .withStyle(ChatFormatting.GRAY));
            }
        }
        return Math.max(1, entries.size());
    }

    private static int help(CommandSourceStack source) {
        source.sendSystemMessage(Component.literal("==== Ban Dream · 玩家白名单 ====")
            .withStyle(ChatFormatting.GOLD));
        source.sendSystemMessage(Component.literal("/bbban whitelist add <玩家名|UUID>")
            .withStyle(ChatFormatting.GRAY));
        source.sendSystemMessage(Component.literal("/bbban whitelist remove <玩家名|UUID>")
            .withStyle(ChatFormatting.GRAY));
        source.sendSystemMessage(Component.literal("/bbban whitelist list")
            .withStyle(ChatFormatting.GRAY));
        return 1;
    }

    private static List<String> removalSuggestions(CommandSourceStack source) {
        List<String> suggestions = new ArrayList<>(source.getOnlinePlayerNames());
        for (PlayerWhitelistEntry entry : BanRules.playerWhitelistEntries()) {
            if (entry.name != null) {
                suggestions.add(entry.name);
            }
            if (entry.uuid != null) {
                suggestions.add(entry.uuid);
            }
        }
        return suggestions.stream().distinct().toList();
    }

    private static GameProfile findProfile(CommandSourceStack source, String name) {
        ServerPlayer online = source.getServer().getPlayerList().getPlayerByName(name);
        if (online != null) {
            return online.getGameProfile();
        }
        return source.getServer().getProfileCache().get(name).orElse(null);
    }

    private static Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
