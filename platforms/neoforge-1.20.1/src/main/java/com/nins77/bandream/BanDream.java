package com.nins77.bandream;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(BanDream.MOD_ID)
public final class BanDream {
    public static final String MOD_ID = "bandream";
    public static final String MOD_NAME = "Ban Dream";
    public static final String VERSION = "1.2.0";
    private static final Logger LOGGER = LogUtils.getLogger();

    public BanDream() {
        LOGGER.info("Ban Dream NeoForge 1.20.1 development environment loaded");
    }
}
