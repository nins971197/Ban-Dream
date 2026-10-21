package com.nins77.bandream.item;

import com.nins77.bandream.BanDream;
import com.nins77.bandream.block.BanBlocks;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class BanItems {
    private static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, BanDream.MOD_ID);

    public static final RegistryObject<Item> BAN_STICK =
        ITEMS.register("ban_stick", BanStickItem::new);
    public static final RegistryObject<Item> BAN_999 =
        ITEMS.register("999ban", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> BAKABAN =
        ITEMS.register("bakaban",
            () -> new BakabanBlockItem(BanBlocks.BAKABAN.get(), new Item.Properties().stacksTo(1)));

    private BanItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(BAN_STICK.get());
            event.accept(BAN_999.get());
            event.accept(BAKABAN.get());
        }
    }
}
