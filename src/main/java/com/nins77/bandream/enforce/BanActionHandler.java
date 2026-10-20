package com.nins77.bandream.enforce;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.nins77.bandream.action.PendingActionManager;
import com.nins77.bandream.config.BanEntry;
import com.nins77.bandream.config.BanRules;
import com.nins77.bandream.config.BanScope;
import com.nins77.bandream.config.BanTrigger;
import com.nins77.bandream.config.BanType;
import com.nins77.bandream.gui.BanListMenu;
import com.nins77.bandream.network.BanNetwork;
import com.nins77.bandream.network.ChoiceLayout;
import com.nins77.bandream.text.BanText;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public final class BanActionHandler {
    private static final List<String> BLOCK_OPTIONS = List.of(
        "全局禁止");
    private static final List<String> ENTITY_OPTIONS = List.of(
        "全局禁止",
        "当前维度禁止");
    private static final List<String> ITEM_OPTIONS = List.of(
        "全局禁止");
    private static final List<String> CONFIRM_OPTIONS = List.of("确认解封");

    private BanActionHandler() {
    }

    public static void openBlockOptions(ServerPlayer player, String id, String displayName) {
        int actionId = PendingActionManager.offer(
            player, PendingActionManager.Kind.STICK_BLOCK, BanType.BLOCK, List.of(id));
        BanNetwork.sendChoice(player, actionId, "封禁方块",
            displayName + " (" + id + ")", ChoiceLayout.LEFT, BLOCK_OPTIONS);
    }

    public static void openEntityOptions(ServerPlayer player, String id, String displayName) {
        int actionId = PendingActionManager.offer(
            player, PendingActionManager.Kind.STICK_ENTITY, BanType.ENTITY, List.of(id));
        BanNetwork.sendChoice(player, actionId, "封禁生物",
            displayName + " (" + id + ")", ChoiceLayout.LEFT, ENTITY_OPTIONS);
    }

    public static void openItemOptions(ServerPlayer player, List<String> ids) {
        if (ids.isEmpty()) {
            BanEnforcement.notify(player, "这个木桶是空的");
            return;
        }
        int actionId = PendingActionManager.offer(
            player, PendingActionManager.Kind.STICK_ITEMS, BanType.ITEM, ids);
        BanNetwork.sendChoice(player, actionId, "封禁木桶物品",
            "共 " + ids.size() + " 种物品", ChoiceLayout.LEFT, ITEM_OPTIONS);
    }

    public static void openUnbanConfirm(ServerPlayer player, BanType type, String id, String displayName) {
        if (!BanRules.hasRule(type, id)) {
            BanEnforcement.notify(player, "没有匹配的封禁记录: " + BanText.typeLabel(type) + " " + id);
            return;
        }
        int actionId = PendingActionManager.offer(
            player, PendingActionManager.Kind.UNBAN, type, List.of(id));
        BanNetwork.sendChoice(player, actionId, "全局解封" + BanText.typeLabel(type),
            displayName + " (" + id + ")", ChoiceLayout.BOTTOM_RIGHT, CONFIRM_OPTIONS);
    }

    public static void applyChoice(ServerPlayer player, int actionId, int choice) {
        if (!player.hasPermissions(Commands.LEVEL_ADMINS)) {
            BanEnforcement.notify(player, "使用封禁棒需要管理员权限");
            return;
        }
        PendingActionManager.Action action = PendingActionManager.take(player, actionId);
        if (action == null) {
            BanEnforcement.notify(player, "操作已过期，请重新操作");
            return;
        }
        if (choice < 0) {
            if (action.kind() == PendingActionManager.Kind.LIST_ENTRY) {
                BanListMenu.open(player, action.type());
            }
            return;
        }

        boolean reopenList = action.kind() == PendingActionManager.Kind.LIST_ENTRY;
        try {
            switch (action.kind()) {
                case STICK_BLOCK -> applyStickBlock(player, action, choice);
                case STICK_ENTITY -> applyStickEntity(player, action, choice);
                case STICK_ITEMS -> applyStickItems(player, action, choice);
                case UNBAN -> applyUnban(player, action, choice);
                case LIST_ENTRY -> applyListEntry(player, action, choice);
            }
        } catch (Exception exception) {
            BanEnforcement.notify(player, "写入封禁配置失败: " + exception.getMessage());
        } finally {
            if (reopenList) {
                BanListMenu.open(player, action.type());
            }
        }
    }

    public static void unbanItems(ServerPlayer player, List<String> ids) {
        if (!player.hasPermissions(Commands.LEVEL_ADMINS)) {
            BanEnforcement.notify(player, "使用封禁棒需要管理员权限");
            return;
        }
        if (ids.isEmpty()) {
            BanEnforcement.notify(player, "这个木桶是空的");
            return;
        }
        Set<String> targets = new HashSet<>(ids);
        try {
            int removed = BanRules.unban(entry -> entry.type == BanType.ITEM && targets.contains(entry.id));
            BanEnforcement.afterRulesChanged(player.server, false, false);
            BanEnforcement.notify(player, removed == 0
                ? "木桶内物品没有封禁记录"
                : "已全局解封木桶内 " + ids.size() + " 种物品，移除 " + removed + " 条规则");
        } catch (Exception exception) {
            BanEnforcement.notify(player, "写入封禁配置失败: " + exception.getMessage());
        }
    }

    private static void applyStickBlock(
        ServerPlayer player,
        PendingActionManager.Action action,
        int choice
    ) throws IOException {
        if (choice != 0 || action.ids().isEmpty()) {
            BanEnforcement.notify(player, "无效的封禁选项");
            return;
        }
        BanScope scope = BanScope.GLOBAL;
        String id = action.ids().get(0);
        BanRules.ban(blockEntry(id, scope, dimension(player, scope)));
        BanEnforcement.afterRulesChanged(player.server, false, true);
        BanEnforcement.notify(player, "已封禁方块 " + id + "：" + scopeText(scope));
    }

    private static void applyStickEntity(
        ServerPlayer player,
        PendingActionManager.Action action,
        int choice
    ) throws IOException {
        if (choice > 1 || action.ids().isEmpty()) {
            BanEnforcement.notify(player, "无效的封禁选项");
            return;
        }
        BanScope scope = choice == 0 ? BanScope.GLOBAL : BanScope.DIMENSION;
        String id = action.ids().get(0);
        BanRules.ban(entityEntry(id, scope, dimension(player, scope)));
        BanEnforcement.afterRulesChanged(player.server, true, false);
        BanEnforcement.notify(player, "已封禁生物 " + id + "：" + scopeText(scope));
    }

    private static void applyStickItems(
        ServerPlayer player,
        PendingActionManager.Action action,
        int choice
    ) throws IOException {
        if (choice != 0 || action.ids().isEmpty()) {
            BanEnforcement.notify(player, "无效的封禁选项");
            return;
        }
        BanScope scope = BanScope.GLOBAL;
        String dimension = dimension(player, scope);
        List<BanEntry> entries = new ArrayList<>(action.ids().size());
        for (String id : action.ids()) {
            BanEntry entry = itemEntry(id, scope, dimension, BanTrigger.INVENTORY);
            entry.jeiVisible = !BanRules.hiddenJeiItems().contains(id);
            entries.add(entry);
        }
        BanRules.banAll(entries);
        BanEnforcement.afterRulesChanged(player.server, false, false);
        BanEnforcement.notify(player, "已封禁木桶内 " + entries.size() + " 种物品："
            + scopeText(scope));
    }

    private static void applyUnban(
        ServerPlayer player,
        PendingActionManager.Action action,
        int choice
    ) throws IOException {
        if (choice != 0 || action.ids().isEmpty()) {
            return;
        }
        String id = action.ids().get(0);
        int removed = BanRules.unban(action.type(), id, null, null);
        BanEnforcement.afterRulesChanged(player.server, false, action.type() == BanType.BLOCK);
        BanEnforcement.notify(player, removed == 0
            ? "没有匹配的封禁记录: " + BanText.typeLabel(action.type()) + " " + id
            : "已全局解封" + BanText.typeLabel(action.type()) + " " + id + "，移除 " + removed + " 条规则");
    }

    private static void applyListEntry(
        ServerPlayer player,
        PendingActionManager.Action action,
        int choice
    ) throws IOException {
        int maxChoice = 1;
        if (choice > maxChoice || action.ids().isEmpty()) {
            return;
        }
        String id = action.ids().get(0);
        if (action.type() == BanType.BLOCK) {
            if (choice == 1) {
                int removed = BanRules.unban(action.type(), id, null, null);
                BanEnforcement.afterRulesChanged(player.server, false, true);
                BanEnforcement.notify(player, removed == 0
                    ? "没有匹配的封禁记录"
                    : "已解除方块 " + id + "，移除 " + removed + " 条规则");
                return;
            }
            BanRules.ban(blockEntry(id, BanScope.GLOBAL, null));
            BanEnforcement.afterRulesChanged(player.server, false, true);
            BanEnforcement.notify(player, "已设置方块 " + id + "：全局禁止");
            return;
        }
        if (choice == 1) {
            int removed = BanRules.unban(action.type(), id, null, null);
            BanEnforcement.afterRulesChanged(player.server, false, action.type() == BanType.BLOCK);
            BanEnforcement.notify(player, removed == 0
                ? "没有匹配的封禁记录"
                : "已解除" + BanText.typeLabel(action.type()) + " " + id + "，移除 " + removed + " 条规则");
            return;
        }
        if (action.type() == BanType.ITEM) {
            BanEntry entry = itemEntry(id, BanScope.GLOBAL, null, BanTrigger.INVENTORY);
            entry.jeiVisible = !BanRules.hiddenJeiItems().contains(id);
            BanRules.ban(entry);
            BanEnforcement.afterRulesChanged(player.server, false, false);
            BanEnforcement.notify(player, "已设置物品 " + id + "：" + BanText.trigger(entry));
        }
    }

    private static BanEntry blockEntry(String id, BanScope scope, String dimension) {
        return new BanEntry(BanType.BLOCK, id, scope, dimension);
    }

    private static BanEntry entityEntry(String id, BanScope scope, String dimension) {
        return new BanEntry(BanType.ENTITY, id, scope, dimension);
    }

    private static BanEntry itemEntry(String id, BanScope scope, String dimension, BanTrigger trigger) {
        BanEntry entry = new BanEntry(BanType.ITEM, id, scope, dimension);
        entry.trigger = trigger;
        entry.jeiVisible = true;
        return entry;
    }

    private static String dimension(ServerPlayer player, BanScope scope) {
        return scope == BanScope.DIMENSION
            ? player.level().dimension().location().toString()
            : null;
    }

    private static String scopeText(BanScope scope) {
        return scope == BanScope.DIMENSION ? "当前维度" : "全局";
    }
}
