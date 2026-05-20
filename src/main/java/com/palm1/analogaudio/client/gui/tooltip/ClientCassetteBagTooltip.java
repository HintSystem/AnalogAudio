package com.palm1.analogaudio.client.gui.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.util.Mth;

import java.util.List;

public class ClientCassetteBagTooltip implements ClientTooltipComponent {
    private static final int SLOT_MARGIN = 4;
    private static final int SLOT_SIZE = 24;
    private static final int GRID_WIDTH = 96;
    private static final int PROGRESSBAR_HEIGHT = 13;
    private static final int PROGRESSBAR_WIDTH = 96;
    private static final int PROGRESSBAR_BORDER = 1;
    private static final int PROGRESSBAR_FILL_MAX = 94;
    private static final int PROGRESSBAR_MARGIN_Y = 4;
    private final ItemContainerContents contents;

    public ClientCassetteBagTooltip(final CassetteBagTooltip tooltip) {
        this.contents = tooltip.contents();
    }

    @Override
    public int getHeight() {
        return itemGridHeight() /* + PROGRESSBAR_HEIGHT + PROGRESSBAR_MARGIN_Y*2 */;
    }

    @Override
    public int getWidth(final Font font) {
        return gridSizeX() * SLOT_SIZE;
    }

    private int itemGridHeight() {
        return this.gridSizeY() * SLOT_SIZE;
    }

    private int gridSizeX() {
        return 4;
    }

    private int gridSizeY() {
        return Mth.positiveCeilDiv(contents.getSlots(), gridSizeX());
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        List<ItemStack> shownItems = getShownItems();

        int slotNumber = 0;
        for (int rowNumber = 0; rowNumber < gridSizeY(); rowNumber++) {
            for (int columnNumber = 0; columnNumber < gridSizeX(); columnNumber++) {
                if (slotNumber < shownItems.size()) {
                    renderSlot(slotNumber++,
                        x + columnNumber * SLOT_SIZE,
                        y + rowNumber * SLOT_SIZE,
                        shownItems, graphics);
                }
            }
        }

//        this.extractSelectedItemTooltip(font, graphics, x, y, w);
//        extractProgressbar(x, y + this.itemGridHeight() + 4, font, graphics);
    }

    private List<ItemStack> getShownItems() {
        List<ItemStack> items = this.contents.stream().toList();
        int lastNonEmpty = 0;
        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).isEmpty()) lastNonEmpty = i + 1;
        }

        return items.subList(0, lastNonEmpty);
    }

    private void renderSlot(
        final int slotNumber,
        final int drawX,
        final int drawY,
        final List<ItemStack> shownItems,
        final GuiGraphics graphics
    ) {
//        boolean hasHighlight = false;
//        if (hasHighlight) {
//            graphics.blitSprite(SLOT_HIGHLIGHT_BACK_SPRITE, drawX, drawY, SLOT_SIZE, SLOT_SIZE);
//        } else {
//            graphics.blitSprite(SLOT_BACKGROUND_SPRITE, drawX, drawY, SLOT_SIZE, SLOT_SIZE);
//        }

        ItemStack item = shownItems.get(slotNumber);
        graphics.renderFakeItem(item, drawX + SLOT_MARGIN, drawY + SLOT_MARGIN);

//        if (hasHighlight) {
//            graphics.blitSprite(SLOT_HIGHLIGHT_FRONT_SPRITE, drawX, drawY, SLOT_SIZE, SLOT_SIZE);
//        }
    }

//    private void extractSelectedItemTooltip(final Font font, final GuiGraphics graphics, final int x, final int y, final int w) {
//        ItemStack itemStack = this.contents.getSelectedItem();
//        if (itemStack != null) {
//            Component selectedItemName = itemStack.getStyledHoverName();
//            int textWidth = font.width(selectedItemName.getVisualOrderText());
//            int centerTooltip = x + w / 2 - 12;
//            ClientTooltipComponent selectedItemNameTooltip = ClientTooltipComponent.create(selectedItemName.getVisualOrderText());
//            graphics.tooltip(
//                font,
//                List.of(selectedItemNameTooltip),
//                centerTooltip - textWidth / 2,
//                y - 15,
//                DefaultTooltipPositioner.INSTANCE,
//                (Identifier)itemStack.get(DataComponents.TOOLTIP_STYLE)
//            );
//        }
//    }
//
//    private static void extractProgressbar(final int x, final int y, final Font font, final GuiGraphics graphics, final Fraction weight) {
//        graphics.blitSprite(getProgressBarTexture(weight), x + 1, y, getProgressBarFill(weight), 13);
//        graphics.blitSprite(PROGRESSBAR_BORDER_SPRITE, x, y, GRID_WIDTH, 13);
//        Component progressBarFillText = getProgressBarFillText(weight);
//        if (progressBarFillText != null) {
//            graphics.centeredText(font, progressBarFillText, x + 48, y + 3, -1);
//        }
//    }
//
//    private static int getEmptyBundleDescriptionTextHeight(final Font font) {
//        return font.split(BUNDLE_EMPTY_DESCRIPTION, GRID_WIDTH).size() * 9;
//    }
//
//    private static int getProgressBarFill(final Fraction weight) {
//        return Mth.clamp(Mth.mulAndTruncate(weight, 94), 0, 94);
//    }
//
//    private static Identifier getProgressBarTexture(final Fraction weight) {
//        return weight.compareTo(Fraction.ONE) >= 0 ? PROGRESSBAR_FULL_SPRITE : PROGRESSBAR_FILL_SPRITE;
//    }
//
//    private static @Nullable Component getProgressBarFillText(final Fraction weight) {
//        if (weight.compareTo(Fraction.ZERO) == 0) {
//            return BUNDLE_EMPTY_TEXT;
//        } else {
//            return weight.compareTo(Fraction.ONE) >= 0 ? BUNDLE_FULL_TEXT : null;
//        }
//    }
}
