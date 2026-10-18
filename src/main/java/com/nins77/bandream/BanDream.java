package com.nins77.bandream;

import com.nins77.bandream.config.BanRules;
import com.nins77.bandream.command.BanCommands;
import com.nins77.bandream.enforce.BanEvents;
import com.nins77.bandream.enforce.BanStickEvents;
import com.nins77.bandream.gui.BanMenus;
import com.nins77.bandream.item.BanItems;
import com.nins77.bandream.network.BanNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(BanDream.MOD_ID)
public final class BanDream {
    public static final String MOD_ID = "bandream";
    public static final String MOD_NAME = "Ban Dream";

    public BanDream() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        BanMenus.register(modBus);
        BanItems.register(modBus);
        BanNetwork.register();
        MinecraftForge.EVENT_BUS.register(BanEvents.class);
        MinecraftForge.EVENT_BUS.register(BanStickEvents.class);
        MinecraftForge.EVENT_BUS.addListener(BanCommands::register);
        modBus.addListener(BanItems::addCreative);
        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                BanRules.load();
            } catch (Exception ignored) {
            }
        });
    }
}
