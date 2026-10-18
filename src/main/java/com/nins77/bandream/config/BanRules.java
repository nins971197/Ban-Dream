package com.nins77.bandream.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParseException;
import com.mojang.brigadier.StringReader;
import com.nins77.bandream.BanDream;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.loading.FMLPaths;

public final class BanRules {
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .registerTypeAdapter(BanType.class, (JsonDeserializer<BanType>) (json, type, context) ->
            BanType.byId(json.getAsString()).orElse(null))
        .registerTypeAdapter(BanScope.class, (JsonDeserializer<BanScope>) (json, type, context) -> {
            try {
                return BanScope.valueOf(json.getAsString().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        })
        .registerTypeAdapter(BanTrigger.class, (JsonDeserializer<BanTrigger>) (json, type, context) -> {
            try {
                return BanTrigger.valueOf(json.getAsString().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        })
        .create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("bandream").resolve("bans.json");
    private static final Comparator<BanEntry> ORDER = Comparator
        .comparing((BanEntry entry) -> entry.type.ordinal())
        .thenComparing(entry -> entry.id)
        .thenComparing(entry -> entry.dimension == null ? "" : entry.dimension)
        .thenComparing(entry -> entry.path == null ? "" : entry.path);

    private static volatile RuleIndex banIndex = RuleIndex.empty();
    private static volatile List<PlayerWhitelistEntry> playerWhitelist = List.of();
    private static volatile Set<UUID> whitelistedPlayerIds = Set.of();
    private static volatile Set<String> whitelistedPlayerNames = Set.of();
    private static volatile Set<String> hiddenJeiItems = Set.of();
    private static volatile boolean hasInventoryItemRules;
    private static volatile boolean hasUseItemRules;
    private static volatile boolean hasEnchantmentRules;
    private static volatile boolean hasEffectRules;
    private static volatile boolean hasEntityRules;
    private static volatile boolean hasNbtRules;
    private static final Map<String, Optional<NbtPathArgument.NbtPath>> PATH_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Optional<Tag>> VALUE_CACHE = new ConcurrentHashMap<>();

    private BanRules() {
    }

    public static synchronized void load() throws IOException {
        Files.createDirectories(CONFIG_PATH.getParent());
        if (Files.notExists(CONFIG_PATH)) {
            writeConfig(List.of(), List.of());
        }

        String json = Files.readString(CONFIG_PATH, StandardCharsets.UTF_8);
        BanConfigFile file;
        try {
            file = GSON.fromJson(json, BanConfigFile.class);
        } catch (JsonParseException exception) {
            throw new IOException("Invalid Ban Dream config: " + exception.getMessage(), exception);
        }
        if (file == null) {
            file = new BanConfigFile();
        }

        List<BanEntry> bans = normalizeBans(file.bans);
        List<PlayerWhitelistEntry> players = normalizePlayers(file.playerWhitelist);
        setRules(bans, players);
        BanDream.LOGGER.info("Loaded {} bans and {} player whitelist entries from {}",
            bans.size(), players.size(), CONFIG_PATH);
    }

    public static synchronized boolean ban(BanEntry entry) throws IOException {
        BanEntry normalized = normalizedCopy(entry);
        boolean replaced = banIndex.entries().stream().anyMatch(normalized::sameKey);
        saveBans(withReplacement(banIndex.entries(), List.of(normalized)));
        return replaced;
    }

    public static synchronized int banAll(List<BanEntry> additions) throws IOException {
        if (additions == null || additions.isEmpty()) {
            return 0;
        }
        List<BanEntry> normalized = additions.stream().map(BanRules::normalizedCopy).toList();
        saveBans(withReplacement(banIndex.entries(), normalized));
        return normalized.size();
    }

    public static synchronized int unban(java.util.function.Predicate<BanEntry> predicate) throws IOException {
        List<BanEntry> next = new ArrayList<>(banIndex.entries());
        int before = next.size();
        next.removeIf(predicate);
        int removed = before - next.size();
        if (removed > 0) {
            saveBans(next);
        }
        return removed;
    }

    public static int unban(BanType type, String id, BanScope scope, String dimension) throws IOException {
        return unban(entry -> {
            if (entry.type != type || !entry.id.equals(id)) {
                return false;
            }
            if (scope == null) {
                return true;
            }
            if (scope == BanScope.GLOBAL) {
                return entry.isGlobal();
            }
            return entry.scope == BanScope.DIMENSION && java.util.Objects.equals(entry.dimension, dimension);
        });
    }

    public static int unbanNbt(String id, String path, String value) throws IOException {
        return unban(entry -> entry.type == BanType.NBT && entry.id.equals(id)
            && (path == null || path.equals(entry.path))
            && (value == null || value.equals(entry.value)));
    }

    public static synchronized boolean addPlayerToWhitelist(String name, UUID uuid) throws IOException {
        PlayerWhitelistEntry entry = new PlayerWhitelistEntry(name, uuid);
        if (!entry.normalize()) {
            throw new IllegalArgumentException("Invalid player whitelist entry");
        }
        List<PlayerWhitelistEntry> next = new ArrayList<>(playerWhitelist);
        boolean replaced = next.removeIf(existing -> existing.sameIdentity(entry));
        next.add(entry);
        savePlayerWhitelist(next);
        return replaced;
    }

    public static synchronized int removePlayerFromWhitelist(String key) throws IOException {
        List<PlayerWhitelistEntry> next = new ArrayList<>(playerWhitelist);
        int before = next.size();
        next.removeIf(entry -> entry.matchesKey(key));
        int removed = before - next.size();
        if (removed > 0) {
            savePlayerWhitelist(next);
        }
        return removed;
    }

    public static boolean isPlayerWhitelisted(Player player) {
        if (player == null) {
            return false;
        }
        if (whitelistedPlayerIds.contains(player.getUUID())) {
            return true;
        }
        return whitelistedPlayerNames.contains(player.getGameProfile().getName().toLowerCase(Locale.ROOT));
    }

    public static List<PlayerWhitelistEntry> playerWhitelistEntries() {
        return playerWhitelist;
    }

    private static void saveBans(List<BanEntry> bans) throws IOException {
        List<BanEntry> sorted = new ArrayList<>(bans);
        sorted.sort(ORDER);
        writeConfig(sorted, playerWhitelist);
        setRules(sorted, playerWhitelist);
    }

    private static void savePlayerWhitelist(List<PlayerWhitelistEntry> players) throws IOException {
        List<PlayerWhitelistEntry> sorted = new ArrayList<>(players);
        sorted.sort(Comparator.comparing(PlayerWhitelistEntry::nameKey)
            .thenComparing(entry -> entry.uuid == null ? "" : entry.uuid));
        writeConfig(banIndex.entries(), sorted);
        setRules(banIndex.entries(), sorted);
    }

    private static List<BanEntry> withReplacement(List<BanEntry> existing, List<BanEntry> additions) {
        List<BanEntry> next = new ArrayList<>(existing);
        for (BanEntry addition : additions) {
            next.removeIf(rule -> rule.sameKey(addition));
            next.add(addition);
        }
        next.sort(ORDER);
        return next;
    }

    private static BanEntry normalizedCopy(BanEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("Missing rule entry");
        }
        BanEntry copy = entry.copy();
        if (!copy.normalize()) {
            throw new IllegalArgumentException("Invalid rule entry");
        }
        return copy;
    }

    private static List<BanEntry> normalizeBans(List<BanEntry> source) {
        if (source == null) {
            return List.of();
        }
        List<BanEntry> result = new ArrayList<>();
        for (BanEntry entry : source) {
            if (entry == null || !entry.normalize()) {
                BanDream.LOGGER.warn("Skipping invalid Ban Dream ban entry: {}", entry);
                continue;
            }
            if (result.stream().noneMatch(entry::sameKey)) {
                result.add(entry);
            }
        }
        result.sort(ORDER);
        return result;
    }

    private static List<PlayerWhitelistEntry> normalizePlayers(List<PlayerWhitelistEntry> source) {
        if (source == null) {
            return List.of();
        }
        List<PlayerWhitelistEntry> result = new ArrayList<>();
        for (PlayerWhitelistEntry entry : source) {
            if (entry == null || !entry.normalize()) {
                BanDream.LOGGER.warn("Skipping invalid player whitelist entry: {}", entry);
                continue;
            }
            if (result.stream().noneMatch(entry::sameIdentity)) {
                result.add(entry);
            }
        }
        result.sort(Comparator.comparing(PlayerWhitelistEntry::nameKey)
            .thenComparing(entry -> entry.uuid == null ? "" : entry.uuid));
        return result;
    }

    private static void setRules(List<BanEntry> bans, List<PlayerWhitelistEntry> players) {
        banIndex = RuleIndex.of(bans);
        playerWhitelist = List.copyOf(players);

        Set<UUID> ids = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (PlayerWhitelistEntry entry : players) {
            if (entry.uuid != null) {
                try {
                    ids.add(UUID.fromString(entry.uuid));
                } catch (IllegalArgumentException ignored) {
                    // normalize() already rejects malformed UUIDs.
                }
            }
            if (entry.name != null && !entry.name.isBlank()) {
                names.add(entry.nameKey());
            }
        }
        whitelistedPlayerIds = Set.copyOf(ids);
        whitelistedPlayerNames = Set.copyOf(names);

        Set<String> hidden = new HashSet<>();
        for (BanEntry entry : bans) {
            if (entry.type == BanType.ITEM && Boolean.FALSE.equals(entry.jeiVisible)) {
                hidden.add(entry.id);
            }
        }
        hiddenJeiItems = Set.copyOf(hidden);
        hasInventoryItemRules = bans.stream().anyMatch(entry ->
            (entry.type == BanType.ITEM || entry.type == BanType.NBT) && entry.trigger == BanTrigger.INVENTORY);
        hasUseItemRules = bans.stream().anyMatch(entry ->
            (entry.type == BanType.ITEM || entry.type == BanType.NBT) && entry.trigger == BanTrigger.USE);
        hasEnchantmentRules = bans.stream().anyMatch(entry -> entry.type == BanType.ENCHANTMENT);
        hasEffectRules = bans.stream().anyMatch(entry -> entry.type == BanType.EFFECT);
        hasEntityRules = bans.stream().anyMatch(entry -> entry.type == BanType.ENTITY);
        hasNbtRules = bans.stream().anyMatch(entry -> entry.type == BanType.NBT);
    }

    private static void writeConfig(List<BanEntry> bans, List<PlayerWhitelistEntry> players) throws IOException {
        Files.createDirectories(CONFIG_PATH.getParent());
        BanConfigFile file = new BanConfigFile();
        file.bans = new ArrayList<>(bans);
        file.playerWhitelist = new ArrayList<>(players);
        String json = GSON.toJson(file) + System.lineSeparator();
        Path temporary = CONFIG_PATH.resolveSibling(CONFIG_PATH.getFileName() + ".tmp");
        Files.writeString(temporary, json, StandardCharsets.UTF_8);
        try {
            Files.move(temporary, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static List<BanEntry> entries() {
        return banIndex.entries();
    }

    public static List<BanEntry> entries(BanType type) {
        return banIndex.entries().stream().filter(entry -> entry.type == type).toList();
    }

    public static int count(BanType type) {
        return (int) banIndex.entries().stream().filter(entry -> entry.type == type).count();
    }

    public static int count() {
        return banIndex.entries().size();
    }

    public static Optional<BanEntry> findItem(ItemStack stack, ResourceLocation dimension, BanTrigger trigger) {
        if (stack.isEmpty() || !hasItemRules(trigger)) {
            return Optional.empty();
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String id = itemId.toString();

        for (BanEntry entry : banIndex.candidates(BanType.ITEM, id)) {
            if (entry.matchesScope(dimension) && entry.trigger == trigger) {
                return Optional.of(entry);
            }
        }
        if (hasNbtRules) {
            List<BanEntry> nbtCandidates = new ArrayList<>(banIndex.candidates(BanType.NBT, id));
            nbtCandidates.addAll(banIndex.candidates(BanType.NBT, "*"));
            for (BanEntry entry : nbtCandidates) {
                if (entry.matchesScope(dimension) && entry.trigger == trigger
                    && matchesNbtEntry(entry, stack, itemId)) {
                    return Optional.of(entry);
                }
            }
        }
        return Optional.empty();
    }

    private static boolean matchesNbtEntry(BanEntry entry, ItemStack stack, ResourceLocation itemId) {
        if (!"*".equals(entry.id) && !entry.id.equals(itemId.toString())) {
            return false;
        }
        CompoundTag root = stack.getTag();
        if (root == null || root.isEmpty()) {
            return false;
        }
        if ("*".equals(entry.path)) {
            return true;
        }

        Optional<NbtPathArgument.NbtPath> parsedPath = PATH_CACHE.computeIfAbsent(entry.path, BanRules::parsePath);
        if (parsedPath.isEmpty()) {
            return false;
        }
        List<Tag> matches;
        try {
            matches = parsedPath.get().get(root);
        } catch (Exception ignored) {
            return false;
        }
        if (matches.isEmpty()) {
            return false;
        }
        if (entry.value == null || entry.value.isBlank() || "*".equals(entry.value)) {
            return true;
        }
        Optional<Tag> wanted = VALUE_CACHE.computeIfAbsent(entry.value, value -> {
            try {
                return Optional.of(TagParser.parseTag(value));
            } catch (Exception ignored) {
                return Optional.empty();
            }
        });
        return matches.stream().anyMatch(tag -> wanted.map(value -> tag.equals(value))
            .orElseGet(() -> tag.getAsString().equals(entry.value)));
    }

    private static Optional<NbtPathArgument.NbtPath> parsePath(String value) {
        try {
            return Optional.of(new NbtPathArgument().parse(new StringReader(value)));
        } catch (Exception ignored) {
            BanDream.LOGGER.warn("Invalid NBT path in Ban Dream config: {}", value);
            return Optional.empty();
        }
    }

    public static boolean isEntityBanned(ResourceLocation entityId, ResourceLocation dimension) {
        return banIndex.matches(BanType.ENTITY, entityId.toString(), dimension);
    }

    public static boolean isStructureBanned(ResourceLocation structureId, ResourceLocation dimension) {
        return banIndex.matches(BanType.STRUCTURE, structureId.toString(), dimension);
    }

    public static boolean isEnchantmentBanned(Enchantment enchantment, ResourceLocation dimension) {
        ResourceLocation id = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
        return id != null && isEnchantmentBanned(id.toString(), dimension);
    }

    public static boolean isEnchantmentBanned(String id, ResourceLocation dimension) {
        return banIndex.matches(BanType.ENCHANTMENT, id, dimension);
    }

    public static boolean isEffectBanned(MobEffect effect, ResourceLocation dimension) {
        ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect);
        return id != null && isEffectBanned(id.toString(), dimension);
    }

    public static boolean isEffectBanned(String id, ResourceLocation dimension) {
        return banIndex.matches(BanType.EFFECT, id, dimension);
    }

    public static boolean isDimensionBanned(ResourceLocation dimension) {
        return banIndex.matches(BanType.DIMENSION, dimension.toString(), dimension);
    }

    public static boolean isDimensionBanned(Level level) {
        return isDimensionBanned(level.dimension().location());
    }

    public static boolean hasItemRules(BanTrigger trigger) {
        return trigger == BanTrigger.USE ? hasUseItemRules : hasInventoryItemRules;
    }

    public static boolean hasEnchantmentRules() {
        return hasEnchantmentRules;
    }

    public static boolean hasEffectRules() {
        return hasEffectRules;
    }

    public static boolean hasEntityRules() {
        return hasEntityRules;
    }

    public static Set<String> hiddenJeiItems() {
        return hiddenJeiItems;
    }

    public static Path configPath() {
        return CONFIG_PATH;
    }

    public static List<ItemGroup> itemGroups() {
        Map<String, List<BanEntry>> grouped = new TreeMap<>(banIndex.byId(BanType.ITEM));
        return grouped.entrySet().stream()
            .map(pair -> new ItemGroup(pair.getKey(), List.copyOf(pair.getValue())))
            .toList();
    }

    public record ItemGroup(String id, List<BanEntry> entries) {
    }

    private static final class BanConfigFile {
        int version = 1;
        String _comment = "Ban Dream rules. playerWhitelist players bypass item removal, enchantment/effect cleanup, use restrictions and dimension bans. World generation/entity bans still apply.";
        List<BanEntry> bans = new ArrayList<>();
        List<PlayerWhitelistEntry> playerWhitelist = new ArrayList<>();
    }
}
