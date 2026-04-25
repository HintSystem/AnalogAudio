package com.palm1.analogaudio.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import com.mojang.math.Axis;
import org.lwjgl.glfw.GLFW;

import com.palm1.analogaudio.AnalogAudio;
import com.palm1.analogaudio.inventory.RadioMenu;
import com.palm1.analogaudio.network.packet.UpdateRadioSettingsC2SPacket;
import com.palm1.analogaudio.registry.ModSounds;

public class RadioScreen extends AbstractContainerScreen<RadioMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/radio.png");
    private static final ResourceLocation ICONS = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/cassette_slot.png");
    private static final ResourceLocation ICONS_HOVER = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/cassette_slot_hover.png");

    private static final ResourceLocation PLAY_SPRITE = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/play.png");
    private static final ResourceLocation PLAY_HOVER = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/play_hover.png");
    private static final ResourceLocation PLAY_SELECTED = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/play_selected.png");

    private static final ResourceLocation PAUSE_SPRITE = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/pause.png");
    private static final ResourceLocation PAUSE_HOVER = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/pause_hover.png");
    private static final ResourceLocation PAUSE_SELECTED = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/pause_selected.png");

    private float volume;
    private boolean looping;
    private boolean playing;

    private float dialRotation = 0;
    private boolean isDraggingDial = false;

    public RadioScreen(RadioMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;

        if (menu.getBlockEntity() != null) {
            this.volume = menu.getBlockEntity().getVolume();
            this.looping = menu.getBlockEntity().isLooping();
            this.playing = menu.getBlockEntity().isPlaying();
        } else {
            this.volume = 0.75f;
            this.looping = false;
            this.playing = false;
        }
        this.dialRotation = (volume / 1.5f * 270.0f) - 135.0f;
    }

    @Override
    protected void init() {
        super.init();

        ImageButton playBtn = new ImageButton(this.leftPos + 100, this.topPos + 44, 16, 16,
                new WidgetSprites(PLAY_SPRITE, PLAY_SPRITE), (btn) -> {
                    this.playing = true;
                    this.playClickSound();
                    this.sendUpdate();
                }) {
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                ResourceLocation tex = PLAY_SPRITE;
                if (this.isHovered()) {
                    if (GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(),
                            GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
                        tex = PLAY_SELECTED;
                    } else {
                        tex = PLAY_HOVER;
                    }
                }
                guiGraphics.blit(tex, this.getX(), this.getY(), 0, 0, this.width, this.height, this.width, this.height);
            }
        };
        playBtn.setTooltip(Tooltip.create(Component.translatable("gui.analogaudio.radio.play")));
        this.addRenderableWidget(playBtn);

        ImageButton pauseBtn = new ImageButton(this.leftPos + 118, this.topPos + 44, 16, 16,
                new WidgetSprites(PAUSE_SPRITE, PAUSE_SPRITE), (btn) -> {
                    this.playing = false;
                    this.playClickSound();
                    this.sendUpdate();
                }) {
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                ResourceLocation tex = PAUSE_SPRITE;
                if (this.isHovered()) {
                    if (GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(),
                            GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
                        tex = PAUSE_SELECTED;
                    } else {
                        tex = PAUSE_HOVER;
                    }
                }
                guiGraphics.blit(tex, this.getX(), this.getY(), 0, 0, this.width, this.height, this.width, this.height);
            }
        };
        pauseBtn.setTooltip(Tooltip.create(Component.translatable("gui.analogaudio.radio.pause")));
        this.addRenderableWidget(pauseBtn);

        ImageButton loopBtn = new ImageButton(this.leftPos + 139, this.topPos + 64, 20, 9,
                new WidgetSprites(TEXTURE, TEXTURE), (btn) -> {
                    this.looping = !this.looping;
                    float pitch = 1.0f;
                    float vol = 1.0f;
                    Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(
                            (this.looping ? ModSounds.SWITCH_ON : ModSounds.SWITCH_OFF).get().getLocation(),
                            SoundSource.MASTER, vol, pitch,
                            RandomSource.create(), false, 0,
                            SoundInstance.Attenuation.NONE,
                            0.0D, 0.0D, 0.0D, true));
                    this.sendUpdate();
                }) {
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                int u = this.isHovered() ? 59 : 38;
                int v = looping ? 1 : 10;
                guiGraphics.blit(TEXTURE, this.getX(), this.getY(), u, v, 20, 9, 176, 186);
            }

            @Override
            public void playDownSound(SoundManager handler) {
            }
        };
        loopBtn.setTooltip(Tooltip.create(Component.translatable("gui.analogaudio.radio.loop")));
        this.addRenderableWidget(loopBtn);
    }

    private void playClickSound() {
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    private void sendUpdate() {
        PacketDistributor.sendToServer(new UpdateRadioSettingsC2SPacket(this.menu.getPos(), volume, looping, playing));
    }

    @Override
    protected void renderSlotHighlight(GuiGraphics guiGraphics, Slot slot, int mouseX, int mouseY, float partialTick) {
        if (slot.index == 0)
            return;
        super.renderSlotHighlight(guiGraphics, slot, mouseX, mouseY, partialTick);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (isDraggingDial) {
            updateVolumeFromMouse(mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 19, this.imageWidth, this.imageHeight, 176, 186);

        boolean isSlotHovered = this.isHovering(this.menu.slots.get(0).x, this.menu.slots.get(0).y, 16, 16, mouseX,
                mouseY);
        ResourceLocation slotIcon = isSlotHovered ? ICONS_HOVER : ICONS;
        guiGraphics.blit(slotIcon, this.leftPos + 16, this.topPos + 29, 68, 44, 0, 0, 34, 22, 34, 22);

        int volInt = (int) (volume * 100);
        String volText = volInt + "%";
        int tw = font.width(volText);
        int color = 0xFFFFFF;
        if (volInt == 0)
            color = 0xFF0000;
        else if (volInt > 100)
            color = 0xFFAA00;
        guiGraphics.drawString(font, volText, this.leftPos + 149 - tw / 2, this.topPos + 29, color, false);

        boolean isDialHovered = this.isHovering(140, 44, 18, 18, mouseX, mouseY) || isDraggingDial;
        int dialU = isDialHovered ? 19 : 0;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(this.leftPos + 149.0f, this.topPos + 52, 0);
        guiGraphics.pose().rotateAround(Axis.ZP.rotationDegrees(dialRotation), 0, 0, 0);
        guiGraphics.blit(TEXTURE, -9, -9, dialU, 0, 18, 18, 176, 186);
        guiGraphics.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        if (this.menu.slots.get(0).x == x && this.menu.slots.get(0).y == y) {
            return super.isHovering(16, 29, 68, 44, mouseX, mouseY);
        }
        return super.isHovering(x, y, width, height, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovering(140, 44, 16, 16, mouseX, mouseY)) {
            isDraggingDial = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDraggingDial) {
            isDraggingDial = false;
            sendUpdate();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void updateVolumeFromMouse(double mouseX, double mouseY) {
        double centerX = this.leftPos + 149.0;
        double centerY = this.topPos + 52;
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;

        float angle = (float) Math.toDegrees(Math.atan2(dy, dx)) + 90;
        if (angle < -180)
            angle += 360;
        if (angle > 180)
            angle -= 360;

        float newRotation = Mth.clamp(angle, -135, 135);
        if (Math.abs(newRotation - dialRotation) > 1.0f) {
            dialRotation = newRotation;
            volume = ((dialRotation + 135.0f) / 270.0f) * 1.5f;

            Minecraft.getInstance().getSoundManager()
                    .play(SimpleSoundInstance.forUI(ModSounds.FREQUENCY_TICK.get(), 1.0f + (volume * 0.5f), 1.0f));
        }
    }

    @Override
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        if (slot.container == this.menu.getBlockEntity().inventory) {
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(slot.x + 8, slot.y + 8, 0);
                guiGraphics.pose().scale(4.0f, 4.0f, 1.0f);
                guiGraphics.renderItem(stack, -8, -8);
                guiGraphics.pose().popPose();
                return;
            }
        }
        super.renderSlot(guiGraphics, slot);
    }
}
