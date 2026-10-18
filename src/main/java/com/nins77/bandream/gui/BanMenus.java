package com.nins77.bandream.gui;

import com.nins77.bandream.BanDream;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class BanMenus {
    private static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(ForgeRegistries.MENU_TYPES, BanDream.MOD_ID);

    public static final RegistryObject<MenuType<BanListMenu>> BAN_LIST =
        MENUS.register("ban_list", () -> IForgeMenuType.create(
            (windowId, inventory, data) -> new BanListMenu(windowId, inventory)));

    private BanMenus() {
    }

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
