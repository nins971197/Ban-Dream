package com.nins77.bandream.client;

import java.util.List;

import com.nins77.bandream.network.BanNetwork;
import com.nins77.bandream.network.ChoiceLayout;
import com.nins77.bandream.network.OpenChoicePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class BanChoiceScreen extends Screen {
    private static final int PANEL_BACKGROUND = 0xF01A202A;
    private static final int PANEL_BORDER = 0xFF64748B;
    private static final int TITLE_COLOR = 0xF8FAFC;
    private static final int DETAIL_COLOR = 0xAAB4C0;

    private final int actionId;
    private final String title;
    private final String detail;
    private final ChoiceLayout layout;
    private final List<String> options;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private boolean responded;

    public BanChoiceScreen(OpenChoicePacket packet) {
        super(Component.literal("Ban Dream"));
        this.actionId = packet.actionId();
        this.title = packet.title();
        this.detail = packet.detail();
        this.layout = packet.layout();
        this.options = List.copyOf(packet.options());
    }

    @Override
    protected void init() {
        super.init();
        panelWidth = layout == ChoiceLayout.BOTTOM_RIGHT ? 220 : 200;
        panelHeight = 78 + options.size() * 24;
        panelX = layout == ChoiceLayout.BOTTOM_RIGHT ? Math.max(8, width - panelWidth - 8) : 8;
        panelY = layout == ChoiceLayout.BOTTOM_RIGHT
            ? Math.max(8, height - panelHeight - 8)
            : Math.max(8, (height - panelHeight) / 2);

        int buttonY = panelY + 50;
        for (int index = 0; index < options.size(); index++) {
            int option = index;
            addRenderableWidget(Button.builder(
                Component.literal(options.get(index)),
                button -> choose(option))
                .bounds(panelX + 8, buttonY + index * 24, panelWidth - 16, 20)
                .build());
        }
        addRenderableWidget(Button.builder(
            Component.translatable("gui.cancel"),
            button -> onClose())
            .bounds(panelX + 8, buttonY + options.size() * 24 + 4, panelWidth - 16, 20)
            .build());
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        graphics.fill(0, 0, width, height, 0x66000000);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL_BACKGROUND);
        graphics.renderOutline(panelX, panelY, panelWidth, panelHeight, PANEL_BORDER);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(font, font.plainSubstrByWidth(title, panelWidth - 16),
            panelX + 8, panelY + 8, TITLE_COLOR, false);
        if (!detail.isBlank()) {
            graphics.drawString(font, font.plainSubstrByWidth(detail, panelWidth - 16),
                panelX + 8, panelY + 24, DETAIL_COLOR, false);
        }
    }

    @Override
    public void onClose() {
        if (!responded) {
            sendAction(-1);
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void choose(int option) {
        responded = true;
        sendAction(option);
        onClose();
    }

    private void sendAction(int choice) {
        if (Minecraft.getInstance().getConnection() != null) {
            BanNetwork.sendAction(actionId, choice);
        }
    }
}
