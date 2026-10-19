package com.nins77.bandream.enforce;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.nins77.bandream.config.BanType;
import com.nins77.bandream.item.BanItems;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class BanStickEvents {
    private static final Map<UUID, Integer> LAST_ENTITY_CLICK = new ConcurrentHashMap<>();

    private BanStickEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide
            || !isBanStick(player.getMainHandItem())) {
            return;
        }
        event.setCanceled(true);
        if (!player.hasPermissions(Commands.LEVEL_ADMINS)) {
            BanEnforcement.notify(player, "使用封禁棒需要管理员权限");
            return;
        }
        if (event.getTarget() instanceof Player) {
            BanEnforcement.notify(player, "封禁棒不能用于玩家");
            return;
        }
        if (!(event.getTarget() instanceof LivingEntity living)) {
            return;
        }
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
        if (type == null) {
            BanEnforcement.notify(player, "无法读取该生物的类型");
            return;
        }
        BanActionHandler.openEntityOptions(player, type.toString(), living.getDisplayName().getString());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide()
            || event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START
            || !(event.getEntity() instanceof ServerPlayer player)
            || !isBanStick(player.getMainHandItem())
            || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        if (!player.hasPermissions(Commands.LEVEL_ADMINS)) {
            BanEnforcement.notify(player, "使用封禁棒需要管理员权限");
            return;
        }
        BlockState state = level.getBlockState(event.getPos());
        if (state.isAir()) {
            return;
        }

        if (player.isShiftKeyDown() && state.is(Blocks.BARREL)
            && level.getBlockEntity(event.getPos()) instanceof BarrelBlockEntity barrel) {
            BanActionHandler.openItemOptions(player, collectItemIds(barrel));
            return;
        }

        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (blockId == null) {
            BanEnforcement.notify(player, "无法读取该方块的类型");
            return;
        }
        BanActionHandler.openBlockOptions(player, blockId.toString(), state.getBlock().getName().getString());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()
            || event.getHand() != InteractionHand.MAIN_HAND
            || !(event.getEntity() instanceof ServerPlayer player)
            || !isBanStick(player.getMainHandItem())
            || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        if (!player.hasPermissions(Commands.LEVEL_ADMINS)) {
            BanEnforcement.notify(player, "使用封禁棒需要管理员权限");
            return;
        }
        BlockState state = level.getBlockState(event.getPos());
        if (state.isAir()) {
            return;
        }

        if (player.isShiftKeyDown() && state.is(Blocks.BARREL)
            && level.getBlockEntity(event.getPos()) instanceof BarrelBlockEntity barrel) {
            BanActionHandler.unbanItems(player, collectItemIds(barrel));
            return;
        }

        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (blockId == null) {
            BanEnforcement.notify(player, "无法读取该方块的类型");
            return;
        }
        BanActionHandler.openUnbanConfirm(player, BanType.BLOCK, blockId.toString(),
            state.getBlock().getName().getString());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickEntity(PlayerInteractEvent.EntityInteract event) {
        handleEntityRightClick(event.getLevel(), event.getHand(), event.getEntity(),
            event.getTarget(), event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickEntitySpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        handleEntityRightClick(event.getLevel(), event.getHand(), event.getEntity(),
            event.getTarget(), event);
    }

    public static void clearPlayer(ServerPlayer player) {
        LAST_ENTITY_CLICK.remove(player.getUUID());
    }

    private static void handleEntityRightClick(
        net.minecraft.world.level.Level level,
        InteractionHand hand,
        Player source,
        Entity target,
        PlayerInteractEvent event
    ) {
        if (level.isClientSide()
            || hand != InteractionHand.MAIN_HAND
            || !(source instanceof ServerPlayer player)
            || !isBanStick(player.getMainHandItem())) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        if (!player.hasPermissions(Commands.LEVEL_ADMINS)) {
            BanEnforcement.notify(player, "使用封禁棒需要管理员权限");
            return;
        }
        if (target instanceof Player) {
            BanEnforcement.notify(player, "封禁棒不能用于玩家");
            return;
        }
        if (!(target instanceof LivingEntity living)) {
            return;
        }
        Integer previous = LAST_ENTITY_CLICK.put(player.getUUID(), player.tickCount);
        if (previous != null && previous == player.tickCount) {
            return;
        }
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
        if (type == null) {
            BanEnforcement.notify(player, "无法读取该生物的类型");
            return;
        }
        BanActionHandler.openUnbanConfirm(player, BanType.ENTITY, type.toString(),
            living.getDisplayName().getString());
    }

    private static List<String> collectItemIds(BarrelBlockEntity barrel) {
        Set<ResourceLocation> itemIds = new LinkedHashSet<>();
        for (int slot = 0; slot < barrel.getContainerSize(); slot++) {
            ItemStack stack = barrel.getItem(slot);
            if (!stack.isEmpty()) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (id != null) {
                    itemIds.add(id);
                }
            }
        }
        return itemIds.stream().map(ResourceLocation::toString).toList();
    }

    private static boolean isBanStick(ItemStack stack) {
        return !stack.isEmpty() && stack.is(BanItems.BAN_STICK.get());
    }
}
