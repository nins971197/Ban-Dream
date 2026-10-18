package com.nins77.bandream.client;

import com.nins77.bandream.gui.BanListMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public final class BanListScreen extends AbstractContainerScreen<BanListMenu> {
    private static final int PANEL = 0xEE151A20;
    private static final int PANEL_INNER = 0xEE202832;
    private static final int BORDER = 0xFF475569;
    private static final int SLOT = 0xFF111820;
    private static final int SLOT_BORDER = 0xFF64748B;

    public BanListScreen(BanListMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.inventoryLabelY = 109;
        this.titleLabelY = 6;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, PANEL);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, PANEL_INNER);
        graphics.renderOutline(x, y, imageWidth, imageHeight, BORDER);

        for (Slot slot : menu.slots) {
            int sx = x + slot.x;
            int sy = y + slot.y;
            graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, SLOT_BORDER);
            graphics.fill(sx, sy, sx + 16, sy + 16, SLOT);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, titleLabelY, 0xF8FAFC, false);
        Component page = Component.translatable("bandream.gui.page", menu.page() + 1, menu.totalPages());
        graphics.drawString(font, page, imageWidth - 8 - font.width(page), 6, 0x94A3B8, false);
        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0xCBD5E1, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
