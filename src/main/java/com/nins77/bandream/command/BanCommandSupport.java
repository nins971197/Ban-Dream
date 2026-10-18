package com.nins77.bandream.command;

import java.util.List;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.nins77.bandream.config.BanType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

final class BanCommandSupport {
    static final DynamicCommandExceptionType UNKNOWN_ID = new DynamicCommandExceptionType(
        id -> Component.literal("找不到对应的注册项: " + id));

    static final SuggestionProvider<CommandSourceStack> ITEM_IDS =
        (context, builder) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ITEM.keySet(), builder);
    static final SuggestionProvider<CommandSourceStack> ENTITY_IDS =
        (context, builder) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.keySet(), builder);
    static final SuggestionProvider<CommandSourceStack> ENCHANTMENT_IDS =
        (context, builder) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENCHANTMENT.keySet(), builder);
    static final SuggestionProvider<CommandSourceStack> EFFECT_IDS =
        (context, builder) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.MOB_EFFECT.keySet(), builder);
    static final SuggestionProvider<CommandSourceStack> STRUCTURE_IDS =
        (context, builder) -> SharedSuggestionProvider.suggestResource(
            context.getSource().getServer().registryAccess().registryOrThrow(Registries.STRUCTURE).keySet(), builder);
    static final SuggestionProvider<CommandSourceStack> DIMENSION_IDS =
        (context, builder) -> SharedSuggestionProvider.suggestResource(
            context.getSource().levels().stream().map(key -> key.location()), builder);
    static final SuggestionProvider<CommandSourceStack> TRIGGERS =
        (context, builder) -> SharedSuggestionProvider.suggest(List.of("inventory", "use"), builder);
    static final SuggestionProvider<CommandSourceStack> JEI_MODES =
        (context, builder) -> SharedSuggestionProvider.suggest(List.of("show", "hide"), builder);

    private BanCommandSupport() {
    }

    static void requireKnown(CommandSourceStack source, BanType type, String id) throws CommandSyntaxException {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            throw UNKNOWN_ID.create(id);
        }
        boolean known = switch (type) {
            case ITEM, NBT -> BuiltInRegistries.ITEM.containsKey(location);
            case ENTITY -> BuiltInRegistries.ENTITY_TYPE.containsKey(location);
            case ENCHANTMENT -> BuiltInRegistries.ENCHANTMENT.containsKey(location);
            case EFFECT -> BuiltInRegistries.MOB_EFFECT.containsKey(location);
            case STRUCTURE -> source.getServer().registryAccess()
                .registryOrThrow(Registries.STRUCTURE).containsKey(location);
            case DIMENSION -> source.levels().stream().anyMatch(key -> key.location().equals(location));
        };
        if (!known) {
            throw UNKNOWN_ID.create(id);
        }
    }
}
