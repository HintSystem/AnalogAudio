package com.palm1.analogaudio.client.gui;

import com.palm1.analogaudio.AnalogAudio;
import com.palm1.analogaudio.inventory.CassetteBagMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import com.palm1.analogaudio.registry.ModItems;

public class CassetteBagScreen extends AbstractContainerScreen<CassetteBagMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/cassette_bag.png");

    public CassetteBagScreen(CassetteBagMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 140;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        if (slot.getItem().isEmpty() && slot.index < 18) {
            renderGhostItem(guiGraphics, ModItems.CASSETTE_TAPE.get().getDefaultInstance(), slot.x, slot.y,
                    this.hoveredSlot == slot);
        }
        super.renderSlot(guiGraphics, slot);
    }

    private void renderGhostItem(GuiGraphics guiGraphics, ItemStack stack, int x, int y, boolean hovered) {
        float alpha = hovered ? 0.6f : 0.3f;
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, alpha);
        guiGraphics.renderItem(stack, x, y);
        if (hovered) {
            guiGraphics.fill(RenderType.guiGhostRecipeOverlay(), x, y, x + 16, y + 16, 0x30FFFFFF);
        }
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
}
