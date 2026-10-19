package com.nins77.bandream.client;

import com.nins77.bandream.network.OpenChoicePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public final class BanStickClient {
    private BanStickClient() {
    }

    public static void openChoice(OpenChoicePacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.screen instanceof AbstractContainerScreen<?>) {
            minecraft.player.closeContainer();
        }
        minecraft.setScreen(new BanChoiceScreen(packet));
    }
}
