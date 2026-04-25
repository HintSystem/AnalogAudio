package com.palm1.analogaudio.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import org.lwjgl.glfw.GLFW;

import com.palm1.analogaudio.AnalogAudio;
import com.palm1.analogaudio.item.CassetteData;
import com.palm1.analogaudio.inventory.CassetteDeckMenu;
import com.palm1.analogaudio.network.packet.EraseCassetteC2SPacket;
import com.palm1.analogaudio.network.packet.WriteCassetteC2SPacket;
import com.palm1.analogaudio.registry.ModDataComponents;
import com.palm1.analogaudio.registry.ModItems;
import com.palm1.analogaudio.registry.ModSounds;

import java.util.UUID;

public class CassetteDeckScreen extends AbstractContainerScreen<CassetteDeckMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/cassette_deck.png");

    private static final ResourceLocation WRITE_NORMAL = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/write.png");
    private static final ResourceLocation WRITE_HOVER = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/write_hover.png");
    private static final ResourceLocation WRITE_SELECTED = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/write_selected.png");

    private static final ResourceLocation ERASE_NORMAL = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/erase.png");
    private static final ResourceLocation ERASE_HOVER = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/erase_hover.png");
    private static final ResourceLocation ERASE_SELECTED = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/erase_selected.png");

    private static final ResourceLocation COLOR_NORMAL = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/color_button.png");
    private static final ResourceLocation COLOR_HOVER = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/color_button_hover.png");
    private static final ResourceLocation COLOR_SELECTED = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/color_button_selected.png");

    private static final ResourceLocation SLOT_NORMAL = ResourceLocation.fromNamespaceAndPath(
            AnalogAudio.MODID, "icons/cassette_slot");
    private static final ResourceLocation SLOT_HOVER = ResourceLocation.fromNamespaceAndPath(
            AnalogAudio.MODID, "icons/cassette_slot_hover");

    private EditBox urlBox;
    private EditBox nameBox;
    private int selectedColor = 0xFFFFFF;

    public enum StatusType {
        SUCCESS(0x55FF55),
        ERROR(0xFF5555),
        INFO(0xFFAA00),
        NONE(0xFFFFFF);

        public final int color;

        StatusType(int color) {
            this.color = color;
        }
    }

    public static Component statusMessage = Component.empty();
    public static StatusType statusType = StatusType.NONE;
    private int statusTimer = 0;

    public static void setStatus(Component message, StatusType type, int duration) {
        statusMessage = message;
        statusType = type;
        if (Minecraft.getInstance().screen instanceof CassetteDeckScreen screen) {
            screen.statusTimer = duration;
        }
    }

    public CassetteDeckScreen(CassetteDeckMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 172;
    }

    @Override
    protected void init() {
        super.init();

        this.urlBox = new EditBox(this.font, this.leftPos + 10, this.topPos + 26, 97, 10,
                Component.translatable("gui.analogaudio.cassette_deck.url")) {
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                int outlineColor = this.isHovered() ? 0xFF4A4441 : 0xFF352F2C;
                int bgColor = this.isHovered() ? 0xFF4E3B33 : 0xFF3E2723;

                guiGraphics.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.width + 1,
                        this.getY() + this.height + 1, outlineColor);
                guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height,
                        bgColor);

                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(this.getX(), this.getY() + 1.25f, 0);
                guiGraphics.pose().scale(0.85f, 0.85f, 1.0f);
                guiGraphics.pose().translate(-this.getX(), -this.getY(), 0);

                super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
                guiGraphics.pose().popPose();
            }
        };
        this.urlBox.setMaxLength(256);
        this.urlBox.setHint(Component.literal("https://.../audio.ogg"));
        this.urlBox.setBordered(false);
        this.addRenderableWidget(this.urlBox);

        this.nameBox = new EditBox(this.font, this.leftPos + 10, this.topPos + 48, 97, 10,
                Component.translatable("gui.analogaudio.cassette_deck.name")) {
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                int outlineColor = this.isHovered() ? 0xFF4A4441 : 0xFF352F2C;
                int bgColor = this.isHovered() ? 0xFF4E3B33 : 0xFF3E2723;

                guiGraphics.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.width + 1,
                        this.getY() + this.height + 1, outlineColor);
                guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height,
                        bgColor);

                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(this.getX(), this.getY() + 1.25f, 0);
                guiGraphics.pose().scale(0.85f, 0.85f, 1.0f);
                guiGraphics.pose().translate(-this.getX(), -this.getY(), 0);

                super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
                guiGraphics.pose().popPose();
            }
        };
        this.nameBox.setMaxLength(32);
        this.nameBox.setHint(Component.literal("Fear's Mixtape"));
        this.nameBox.setBordered(false);
        this.addRenderableWidget(this.nameBox);

        DyeColor[] orderedColors = {
                DyeColor.WHITE, DyeColor.LIGHT_GRAY,
                DyeColor.GRAY, DyeColor.BLACK,
                DyeColor.BROWN, DyeColor.RED,
                DyeColor.ORANGE, DyeColor.YELLOW,
                DyeColor.LIME, DyeColor.GREEN,
                DyeColor.CYAN, DyeColor.LIGHT_BLUE,
                DyeColor.BLUE, DyeColor.PURPLE,
                DyeColor.MAGENTA, DyeColor.PINK
        };

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                final DyeColor color = orderedColors[row * 4 + col];
                final int colorVal = 0xFF000000 | color.getFireworkColor();
                ImageButton colorBtn = new ImageButton(this.leftPos + 113 + (col * 13), this.topPos + 26 + (row * 14),
                        12, 13,
                        new WidgetSprites(COLOR_NORMAL, COLOR_NORMAL), button -> {
                            this.selectedColor = colorVal;
                            ItemStack stack = this.menu.getSlot(0).getItem();
                            if (!stack.isEmpty()
                                    && stack.is(ModItems.CASSETTE_TAPE.get())) {
                                CassetteData oldData = stack
                                        .get(ModDataComponents.CASSETTE_DATA.get());
                                String uuid = oldData != null ? oldData.uuid() : UUID.randomUUID().toString();
                                String url = oldData != null ? oldData.url() : "";
                                String name = oldData != null ? oldData.name() : "";
                                stack.set(ModDataComponents.CASSETTE_DATA.get(),
                                        new CassetteData(uuid, url, name, colorVal));
                            }
                        }) {
                    @Override
                    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                        int r = ((colorVal >> 16) & 0xFF);
                        int g = ((colorVal >> 8) & 0xFF);
                        int b = (colorVal & 0xFF);
                        guiGraphics.setColor(r / 255.0f, g / 255.0f, b / 255.0f, 1.0f);

                        ResourceLocation texture = COLOR_NORMAL;
                        if (selectedColor == colorVal) {
                            texture = COLOR_SELECTED;
                        } else if (this.isHovered()) {
                            texture = COLOR_HOVER;
                        }

                        guiGraphics.blit(texture, this.getX(), this.getY(), 0, 0, this.width, this.height, this.width,
                                this.height);
                        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                    }
                };
                this.addRenderableWidget(colorBtn);
            }
        }

        ImageButton writeBtn = new ImageButton(this.leftPos + 8, this.topPos + 67, 18, 18,
                new WidgetSprites(WRITE_NORMAL, WRITE_NORMAL), button -> {
                    if (this.menu.getSlot(0).hasItem()) {
                        PacketDistributor.sendToServer(
                                new WriteCassetteC2SPacket(this.urlBox.getValue(), this.nameBox.getValue(),
                                        this.selectedColor));
                        setStatus(Component.translatable("gui.analogaudio.cassette_deck.status.writing"),
                                StatusType.INFO, 60);
                    } else {
                        setStatus(Component.translatable("gui.analogaudio.cassette_deck.status.insert_cassette"),
                                StatusType.ERROR, 60);
                    }
                }) {
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                ResourceLocation texture = WRITE_NORMAL;
                if (this.isHovered()) {
                    if (GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(),
                            GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
                        texture = WRITE_SELECTED;
                    } else {
                        texture = WRITE_HOVER;
                    }
                }
                guiGraphics.blit(texture, this.getX(), this.getY(), 0, 0, this.width, this.height, this.width,
                        this.height);
            }

            @Override
            public void playDownSound(SoundManager handler) {
                handler.play(SimpleSoundInstance.forUI(ModSounds.WRITE.get(), 1.0f));
            }
        };
        writeBtn.setTooltip(Tooltip.create(Component.translatable("gui.analogaudio.cassette_deck.write")));
        this.addRenderableWidget(writeBtn);

        ImageButton eraseBtn = new ImageButton(this.leftPos + 26, this.topPos + 67, 18, 18,
                new WidgetSprites(ERASE_NORMAL, ERASE_NORMAL), button -> {
                    if (this.menu.getSlot(0).hasItem()) {
                        PacketDistributor.sendToServer(
                                new EraseCassetteC2SPacket());
                        setStatus(Component.translatable("gui.analogaudio.cassette_deck.status.erased"),
                                StatusType.SUCCESS, 60);
                    } else {
                        setStatus(Component.translatable("gui.analogaudio.cassette_deck.status.insert_cassette"),
                                StatusType.ERROR, 60);
                    }
                }) {
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                ResourceLocation texture = ERASE_NORMAL;
                if (this.isHovered()) {
                    if (GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(),
                            GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
                        texture = ERASE_SELECTED;
                    } else {
                        texture = ERASE_HOVER;
                    }
                }
                guiGraphics.blit(texture, this.getX(), this.getY(), 0, 0, this.width, this.height, this.width,
                        this.height);
            }

            @Override
            public void playDownSound(SoundManager handler) {
                handler.play(SimpleSoundInstance.forUI(ModSounds.ERASE.get(), 1.0f));
            }
        };
        eraseBtn.setTooltip(Tooltip.create(Component.translatable("gui.analogaudio.cassette_deck.erase")));
        this.addRenderableWidget(eraseBtn);
    }

    public void handleResult(int status) {
        switch (status) {
            case 0 -> setStatus(Component.translatable("gui.analogaudio.cassette_deck.status.write_success"),
                    StatusType.SUCCESS, 100);
            case 1 -> setStatus(Component.translatable("gui.analogaudio.cassette_deck.status.write_fail"),
                    StatusType.ERROR, 100);
            case 2 -> setStatus(Component.translatable("gui.analogaudio.cassette_deck.status.erase_success"),
                    StatusType.SUCCESS, 100);
            case 3 -> setStatus(Component.translatable("gui.analogaudio.cassette_deck.status.erase_fail"),
                    StatusType.ERROR, 100);
            case 4 -> setStatus(Component.translatable("gui.analogaudio.cassette_deck.status.invalid_url"),
                    StatusType.ERROR, 100);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (statusTimer > 0) {
            statusTimer--;
            if (statusTimer == 0) {
                statusMessage = Component.empty();
                statusType = StatusType.NONE;
            }
        }

        ItemStack stack = this.menu.getSlot(0).getItem();
        if (stack.isEmpty()) {
            selectedColor = 0xFFFFFFFF;
        } else if (stack.is(ModItems.CASSETTE_TAPE.get())) {
            CassetteData data = stack
                    .get(ModDataComponents.CASSETTE_DATA.get());
            if (data != null) {
                selectedColor = data.color();
            } else {
                selectedColor = 0xFFFFFF;
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.minecraft.player.closeContainer();
            return true;
        }
        if (this.urlBox.isFocused() && this.urlBox.keyPressed(keyCode, scanCode, modifiers))
            return true;
        if (this.nameBox.isFocused() && this.nameBox.keyPressed(keyCode, scanCode, modifiers))
            return true;
        if (this.urlBox.isFocused() || this.nameBox.isFocused())
            return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        if (slot.index == 0) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(slot.x, slot.y, 0);
            guiGraphics.pose().scale(2.0f, 2.0f, 1.0f);
            guiGraphics.pose().translate(-slot.x, -slot.y, 0);
            super.renderSlot(guiGraphics, slot);
            guiGraphics.pose().popPose();
        } else {
            super.renderSlot(guiGraphics, slot);
        }
    }

    @Override
    protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        Slot slot0 = this.menu.slots.get(0);
        if (x == slot0.x && y == slot0.y) {
            return super.isHovering(slot0.x - 1, slot0.y + 5, 34, 22, mouseX, mouseY);
        }
        return super.isHovering(x, y, width, height, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(GUI_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight,
                this.imageWidth, this.imageHeight);

        Slot slot0 = this.menu.slots.get(0);
        int slotX = this.leftPos + slot0.x - 1;
        int slotY = this.topPos + slot0.y + 5;

        ResourceLocation slotTexture = (this.hoveredSlot == slot0) ? SLOT_HOVER : SLOT_NORMAL;
        guiGraphics.blitSprite(slotTexture, slotX, slotY, 34, 22);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(10, 17, 0);
        guiGraphics.pose().scale(0.85f, 0.85f, 1.0f);
        guiGraphics.drawString(this.font, Component.translatable("gui.analogaudio.cassette_deck.url"), 0, 0, 0xFFFFFF,
                false);
        guiGraphics.pose().popPose();

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(10, 39, 0);
        guiGraphics.pose().scale(0.85f, 0.85f, 1.0f);
        guiGraphics.drawString(this.font, Component.translatable("gui.analogaudio.cassette_deck.name"), 0, 0, 0xFFFFFF,
                false);
        guiGraphics.pose().popPose();

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(112, 17, 0);
        guiGraphics.pose().scale(0.85f, 0.85f, 1.0f);
        guiGraphics.drawString(this.font, Component.translatable("gui.analogaudio.cassette_deck.select_color"), 0, 0,
                0xFFFFFF, false);
        guiGraphics.pose().popPose();

        if (statusMessage != Component.empty()) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(10, 61, 0);
            guiGraphics.pose().scale(0.6f, 0.6f, 1.0f);

            guiGraphics.drawString(this.font, Component.literal("§o").append(statusMessage), 0, 0, statusType.color,
                    false);
            guiGraphics.pose().popPose();
        }
    }
}
