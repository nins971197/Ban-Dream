package com.nins77.bandream.client;

import java.util.List;

import net.minecraftforge.fml.ModList;

public final class BanClientHooks {
    private BanClientHooks() {
    }

    public static void applyJeiSync(List<String> hiddenItems) {
        if (ModList.get().isLoaded("jei")) {
            applyToJei(hiddenItems);
        }
    }

    private static void applyToJei(List<String> hiddenItems) {
        BanDreamJeiPlugin.acceptHiddenItems(hiddenItems);
        BanDreamJeiPlugin.refresh();
    }
}
