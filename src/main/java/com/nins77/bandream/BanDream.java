package com.nins77.bandream;

import com.nins77.bandream.config.BanRules;
import com.nins77.bandream.block.BakabanEvents;
import com.nins77.bandream.block.BanBlocks;
import com.nins77.bandream.command.BanCommands;
import com.nins77.bandream.compat.CuriosEvents;
import com.nins77.bandream.enforce.BanEvents;
import com.nins77.bandream.enforce.BanStickEvents;
import com.nins77.bandream.enforce.BlockBanEvents;
import com.nins77.bandream.enforce.BlockBanSupport;
import com.nins77.bandream.gui.BanMenus;
import com.nins77.bandream.item.BanItems;
import com.nins77.bandream.network.BanNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(BanDream.MOD_ID)
public final class BanDream {
    public static final String MOD_ID = "bandream";
    public static final String MOD_NAME = "Ban Dream";
    private static final Logger LOGGER = LoggerFactory.getLogger(BanDream.class);

    public BanDream() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        BanMenus.register(modBus);
        BanBlocks.register(modBus);
        BanItems.register(modBus);
        BanNetwork.register();
        MinecraftForge.EVENT_BUS.register(BanEvents.class);
        MinecraftForge.EVENT_BUS.register(BakabanEvents.class);
        MinecraftForge.EVENT_BUS.register(BanStickEvents.class);
        MinecraftForge.EVENT_BUS.register(BlockBanEvents.class);
        if (ModList.get().isLoaded("curios")) {
            MinecraftForge.EVENT_BUS.register(CuriosEvents.class);
        }
        MinecraftForge.EVENT_BUS.addListener(BanCommands::register);
        MinecraftForge.EVENT_BUS.addGenericListener(BlockEntity.class, BlockBanSupport::trackCapabilities);
        modBus.addListener(BanItems::addCreative);
        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                BanRules.load();
            } catch (Exception exception) {
                LOGGER.error("Failed to load Ban Dream rules from {}", BanRules.configPath(), exception);
            }
        });
    }
}
