package com.nins77.bandream.enforce;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.nins77.bandream.action.PendingBanManager;
import com.nins77.bandream.config.BanEntry;
import com.nins77.bandream.config.BanScope;
import com.nins77.bandream.config.BanTrigger;
import com.nins77.bandream.config.BanType;
import com.nins77.bandream.item.BanItems;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class BanStickEvents {
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
        if (event.getTarget() instanceof LivingEntity living) {
            ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
            if (type == null) {
                BanEnforcement.notify(player, "无法读取该生物的类型");
                return;
            }
            BanEntry entry = new BanEntry(BanType.ENTITY, type.toString(), BanScope.GLOBAL, null);
            PendingBanManager.offer(player, List.of(entry),
                "目标生物: " + type + "（" + living.getDisplayName().getString() + "）");
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide()
            || event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START
            || !(event.getEntity() instanceof ServerPlayer player)
            || !player.isShiftKeyDown()
            || !isBanStick(player.getMainHandItem())
            || !(event.getLevel() instanceof ServerLevel level)
            || !level.getBlockState(event.getPos()).is(Blocks.BARREL)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        if (!player.hasPermissions(Commands.LEVEL_ADMINS)) {
            BanEnforcement.notify(player, "使用封禁棒需要管理员权限");
            return;
        }
        if (!(level.getBlockEntity(event.getPos()) instanceof BarrelBlockEntity barrel)) {
            return;
        }

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
        if (itemIds.isEmpty()) {
            BanEnforcement.notify(player, "这个木桶是空的");
            return;
        }

        List<BanEntry> entries = new ArrayList<>(itemIds.size());
        for (ResourceLocation id : itemIds) {
            BanEntry entry = new BanEntry(BanType.ITEM, id.toString(), BanScope.GLOBAL, null);
            entry.trigger = BanTrigger.INVENTORY;
            entry.jeiVisible = true;
            entries.add(entry);
        }
        String preview = itemIds.stream().limit(6).map(ResourceLocation::toString)
            .reduce((left, right) -> left + ", " + right).orElse("");
        if (itemIds.size() > 6) {
            preview += " 等";
        }
        PendingBanManager.offer(player, entries,
            "木桶内检测到 " + itemIds.size() + " 种物品: " + preview);
    }

    private static boolean isBanStick(ItemStack stack) {
        return !stack.isEmpty() && stack.is(BanItems.BAN_STICK.get());
    }
}
