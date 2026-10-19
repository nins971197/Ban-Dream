package com.nins77.bandream.enforce;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.nins77.bandream.block.BakabanManager;
import com.nins77.bandream.compat.CuriosCompat;
import com.nins77.bandream.config.BanEntry;
import com.nins77.bandream.config.BanRules;
import com.nins77.bandream.config.BanTrigger;
import com.nins77.bandream.network.BanNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;

public final class BanEnforcement {
    private static final Map<UUID, Integer> LAST_DIMENSION_WARNING = new ConcurrentHashMap<>();

    private BanEnforcement() {
    }

    public static void enforcePlayer(ServerPlayer player, boolean fullCheck) {
        if (player.level().isClientSide || BanRules.isPlayerWhitelisted(player)) {
            return;
        }
        ResourceLocation dimension = player.level().dimension().location();
        boolean scanInventoryItems = BanRules.hasItemRules(BanTrigger.INVENTORY);
        boolean scanCuriosItems = scanInventoryItems || BanRules.hasItemRules(BanTrigger.USE);
        boolean scanEnchantments = fullCheck && BanRules.hasEnchantmentRules();
        boolean changed = false;

        if (scanInventoryItems || scanEnchantments) {
            changed |= scanCompartment(
                player, player.getInventory().items, dimension, scanInventoryItems, scanEnchantments);
            changed |= scanCompartment(
                player, player.getInventory().armor, dimension, scanInventoryItems, scanEnchantments);
            changed |= scanCompartment(
                player, player.getInventory().offhand, dimension, scanInventoryItems, scanEnchantments);
        }

        ItemStack carried = player.containerMenu.getCarried();
        if (scanInventoryItems && !carried.isEmpty()) {
            Optional<BanEntry> banned = BanRules.findItem(carried, dimension, BanTrigger.INVENTORY);
            if (banned.isPresent()) {
                player.containerMenu.setCarried(ItemStack.EMPTY);
                changed = true;
            }
        }

        if (fullCheck && (scanCuriosItems || scanEnchantments) && ModList.get().isLoaded("curios")) {
            changed |= CuriosCompat.enforce(player, dimension, scanCuriosItems, scanEnchantments);
        }

        if (fullCheck && BanRules.hasEffectRules() && !player.getActiveEffects().isEmpty()) {
            for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects())) {
                if (BanRules.isEffectBanned(effect.getEffect(), dimension)) {
                    player.removeEffect(effect.getEffect());
                    changed = true;
                    notify(player, "已移除封禁效果: " + effect.getEffect().getDisplayName().getString());
                }
            }
        }

        if (changed) {
            player.getInventory().setChanged();
        }
    }

    private static boolean scanCompartment(
        ServerPlayer player,
        NonNullList<ItemStack> compartment,
        ResourceLocation dimension,
        boolean scanItems,
        boolean scanEnchantments
    ) {
        boolean changed = false;
        for (int index = 0; index < compartment.size(); index++) {
            ItemStack stack = compartment.get(index);
            if (stack.isEmpty()) {
                continue;
            }
            if (scanItems && BanRules.findItem(stack, dimension, BanTrigger.INVENTORY).isPresent()) {
                String name = stack.getHoverName().getString();
                compartment.set(index, ItemStack.EMPTY);
                changed = true;
                notify(player, "已移除封禁物品: " + name);
                continue;
            }
            if (scanEnchantments && stripBannedEnchantments(player, stack, dimension)) {
                changed = true;
                notify(player, "已移除封禁附魔: " + stack.getHoverName().getString());
            }
        }
        return changed;
    }

    public static boolean stripBannedEnchantments(ServerPlayer player, ItemStack stack, ResourceLocation dimension) {
        if (BanRules.isPlayerWhitelisted(player) || !BanRules.hasEnchantmentRules()
            || stack.isEmpty() || !stack.isEnchanted()) {
            return false;
        }
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
        boolean removed = enchantments.keySet().removeIf(enchantment -> BanRules.isEnchantmentBanned(enchantment, dimension));
        if (!removed) {
            return false;
        }
        EnchantmentHelper.setEnchantments(enchantments, stack);
        player.getInventory().setChanged();
        return true;
    }

    public static boolean enforceUse(ServerPlayer player, ItemStack held) {
        if (BanRules.isPlayerWhitelisted(player) || held.isEmpty()
            || (!BanRules.hasItemRules(BanTrigger.USE) && !BanRules.hasEnchantmentRules())) {
            return false;
        }
        ResourceLocation dimension = player.level().dimension().location();
        if (BanRules.hasItemRules(BanTrigger.USE)) {
            Optional<BanEntry> direct = BanRules.findItem(held, dimension, BanTrigger.USE);
            if (direct.isPresent()) {
                removeUseBannedItems(player, dimension);
                return true;
            }
        }
        if (stripBannedEnchantments(player, held, dimension)) {
            notify(player, "该物品携带封禁附魔，已阻止使用");
            return true;
        }
        return false;
    }

    public static int removeUseBannedItems(ServerPlayer player, ResourceLocation dimension) {
        if (BanRules.isPlayerWhitelisted(player) || !BanRules.hasItemRules(BanTrigger.USE)) {
            return 0;
        }
        int removed = 0;
        String lastName = "";
        for (NonNullList<ItemStack> compartment : List.of(
            player.getInventory().items,
            player.getInventory().armor,
            player.getInventory().offhand)) {
            for (int index = 0; index < compartment.size(); index++) {
                ItemStack stack = compartment.get(index);
                if (!stack.isEmpty() && BanRules.findItem(stack, dimension, BanTrigger.USE).isPresent()) {
                    lastName = stack.getHoverName().getString();
                    compartment.set(index, ItemStack.EMPTY);
                    removed++;
                }
            }
        }
        ItemStack carried = player.containerMenu.getCarried();
        if (!carried.isEmpty() && BanRules.findItem(carried, dimension, BanTrigger.USE).isPresent()) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
            removed++;
        }
        if (removed > 0) {
            player.getInventory().setChanged();
            notify(player, "已移除封禁物品: " + lastName);
        }
        return removed;
    }

    public static void enforceLivingEffects(LivingEntity entity) {
        if (entity.level().isClientSide || !BanRules.hasEffectRules()) {
            return;
        }
        if (entity.getActiveEffects().isEmpty()) {
            return;
        }
        if (entity instanceof Player player && BanRules.isPlayerWhitelisted(player)) {
            return;
        }
        ResourceLocation dimension = entity.level().dimension().location();
        for (MobEffectInstance effect : new ArrayList<>(entity.getActiveEffects())) {
            if (BanRules.isEffectBanned(effect.getEffect(), dimension)) {
                entity.removeEffect(effect.getEffect());
            }
        }
    }

    public static void escapeBannedDimension(ServerPlayer player) {
        if (BanRules.isPlayerWhitelisted(player)) {
            return;
        }
        ResourceLocation current = player.level().dimension().location();
        if (!BanRules.isDimensionBanned(current)) {
            return;
        }
        ServerLevel target = chooseFallback(player.server, current);
        if (target == null) {
            Integer lastWarning = LAST_DIMENSION_WARNING.get(player.getUUID());
            if (lastWarning == null || player.tickCount - lastWarning >= 100) {
                LAST_DIMENSION_WARNING.put(player.getUUID(), player.tickCount);
                notify(player, "当前维度已被封禁，且没有可用的安全维度");
            }
            return;
        }
        LAST_DIMENSION_WARNING.remove(player.getUUID());
        BlockPos spawn = target.getSharedSpawnPos();
        player.teleportTo(target, spawn.getX() + 0.5D, spawn.getY() + 1.0D, spawn.getZ() + 0.5D,
            player.getYRot(), player.getXRot());
        player.fallDistance = 0.0F;
        notify(player, "维度 " + current + " 已被封禁，已传送至 " + target.dimension().location());
    }

    private static ServerLevel chooseFallback(MinecraftServer server, ResourceLocation banned) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld != null && !overworld.dimension().location().equals(banned)
            && !BanRules.isDimensionBanned(overworld.dimension().location())) {
            return overworld;
        }
        for (ServerLevel level : server.getAllLevels()) {
            ResourceLocation id = level.dimension().location();
            if (!id.equals(banned) && !BanRules.isDimensionBanned(id)) {
                return level;
            }
        }
        return null;
    }

    public static void afterRulesChanged(MinecraftServer server) {
        afterRulesChanged(server, true, true);
    }

    public static void afterRulesChanged(MinecraftServer server, boolean purgeEntities) {
        afterRulesChanged(server, purgeEntities, false);
    }

    public static void afterRulesChanged(
        MinecraftServer server,
        boolean purgeEntities,
        boolean refreshBlockCapabilities
    ) {
        if (refreshBlockCapabilities) {
            BlockBanSupport.refreshItemCapabilities();
            BakabanManager.refresh(server);
        }
        BanNetwork.syncAll();
        boolean entityRules = purgeEntities && BanRules.hasEntityRules();
        boolean effectRules = BanRules.hasEffectRules();
        if (entityRules || effectRules) {
            for (ServerLevel level : server.getAllLevels()) {
                List<Entity> entities = new ArrayList<>();
                level.getAllEntities().forEach(entities::add);
                for (Entity entity : entities) {
                    if (entity instanceof Player) {
                        continue;
                    }
                    if (entityRules && BanRules.isEntityBanned(
                        entity.getType(), level.dimension().location())) {
                        entity.discard();
                    } else if (effectRules && entity instanceof LivingEntity living) {
                        enforceLivingEffects(living);
                    }
                }
            }
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            escapeBannedDimension(player);
            enforcePlayer(player, true);
        }
    }

    public static void notify(ServerPlayer player, String text) {
        player.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.YELLOW), true);
    }

    public static void clearPlayerState(ServerPlayer player) {
        LAST_DIMENSION_WARNING.remove(player.getUUID());
    }
}
