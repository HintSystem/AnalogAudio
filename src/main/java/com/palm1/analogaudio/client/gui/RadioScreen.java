package com.palm1.analogaudio.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
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
import net.minecraft.ChatFormatting;

import com.mojang.math.Axis;
import org.lwjgl.glfw.GLFW;

import com.palm1.analogaudio.AnalogAudio;
import com.palm1.analogaudio.inventory.RadioMenu;
import com.palm1.analogaudio.network.packet.UpdateRadioSettingsC2SPacket;
import com.palm1.analogaudio.registry.ModSounds;
import com.palm1.analogaudio.registry.ModItems;
import com.palm1.analogaudio.block.entity.RadioBlockEntity;

public class RadioScreen extends AbstractContainerScreen<RadioMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/radio.png");
    private static final ResourceLocation ICONS = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/cassette_slot.png");
    private static final ResourceLocation ICONS_HOVER = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/cassette_slot_hover.png");

    private static final ResourceLocation BAG_ICON = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/cassette_bag_slot.png");
    private static final ResourceLocation BAG_HOVER = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/cassette_bag_slot_hover.png");

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

    private static final ResourceLocation SHUFFLE_SPRITE = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/shuffle.png");
    private static final ResourceLocation SHUFFLE_HOVER = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/shuffle_hover.png");
    private static final ResourceLocation SHUFFLE_SELECTED = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/shuffle_selected.png");

    private float volume;
    private boolean looping;
    private boolean playing;
    private boolean shuffle;

    private float dialRotation = 0;
    private boolean isDraggingDial = false;
    private int resetCooldownTicks = 0;

    public RadioScreen(RadioMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;

        if (menu.getBlockEntity() != null) {
            this.volume = menu.getBlockEntity().getVolume();
            this.looping = menu.getBlockEntity().isLooping();
            this.playing = menu.getBlockEntity().isPlaying();
            this.shuffle = menu.getBlockEntity().isShuffle();
        } else {
            this.volume = 0.75f;
            this.looping = false;
            this.playing = false;
            this.shuffle = false;
        }
        this.dialRotation = (volume / 1.5f * 270.0f) - 135.0f;
    }

    @Override
    protected void init() {
        super.init();

        ImageButton playBtn = new ImageButton(this.leftPos + 86, this.topPos + 25, 16, 16,
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

        ImageButton pauseBtn = new ImageButton(this.leftPos + 86, this.topPos + 43, 16, 16,
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

        ImageButton shuffleBtn = new ImageButton(this.leftPos + 86, this.topPos + 61, 16, 16,
                new WidgetSprites(SHUFFLE_SPRITE, SHUFFLE_SPRITE), (btn) -> {
                    this.shuffle = !this.shuffle;
                    this.playClickSound();
                    this.sendUpdate();
                }) {
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                this.setTooltip(Tooltip.create(Component.translatable("gui.analogaudio.radio.shuffle")
                        .append(": ")
                        .append(Component
                                .translatable(shuffle ? "gui.analogaudio.radio.on" : "gui.analogaudio.radio.off")
                                .withStyle(shuffle ? ChatFormatting.GREEN : ChatFormatting.RED))));

                ResourceLocation tex = shuffle ? SHUFFLE_SELECTED : SHUFFLE_SPRITE;
                if (this.isHovered() && tex != SHUFFLE_SELECTED) {
                    if (GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(),
                             GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
                        tex = SHUFFLE_SELECTED;
                    } else {
                        tex = SHUFFLE_HOVER;
                    }
                }
                guiGraphics.blit(tex, this.getX(), this.getY(), 0, 0, this.width, this.height, this.width, this.height);
            }
        };
        this.addRenderableWidget(shuffleBtn);

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
                this.setTooltip(Tooltip.create(Component.translatable("gui.analogaudio.radio.loop")
                        .append(": ")
                        .append(Component
                                .translatable(looping ? "gui.analogaudio.radio.on" : "gui.analogaudio.radio.off")
                                .withStyle(looping ? ChatFormatting.GREEN : ChatFormatting.RED))));

                int u = this.isHovered() ? 59 : 38;
                int v = looping ? 1 : 10;
                guiGraphics.blit(TEXTURE, this.getX(), this.getY(), u, v, 20, 9, 176, 186);
            }

            @Override
            public void playDownSound(SoundManager handler) {
            }
        };
        this.addRenderableWidget(loopBtn);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (resetCooldownTicks > 0) {
            resetCooldownTicks--;
        }
        if (this.menu.getBlockEntity() != null && !isDraggingDial) {
            RadioBlockEntity be = this.menu.getBlockEntity();
            this.volume = be.getVolume();
            this.looping = be.isLooping();
            this.playing = be.isPlaying();
            this.shuffle = be.isShuffle();
            this.dialRotation = (volume / 1.5f * 270.0f) - 135.0f;
        }
    }

    private void playClickSound() {
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    private void sendUpdate() {
        PacketDistributor
                .sendToServer(new UpdateRadioSettingsC2SPacket(this.menu.getPos(), volume, looping, playing, shuffle, false));
        if (this.menu.getBlockEntity() != null) {
            this.menu.getBlockEntity().setSettings(volume, looping, playing, shuffle, false);
        }
    }

    @Override
    protected void renderSlotHighlight(GuiGraphics guiGraphics, Slot slot, int mouseX, int mouseY, float partialTick) {
        if (slot.index < 2)
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

        // Render Cassette Slot
        boolean isSlotHovered = this.isHovering(this.menu.slots.get(1).x, this.menu.slots.get(1).y, 16, 16, mouseX,
                mouseY);
        ResourceLocation slotIcon = isSlotHovered ? ICONS_HOVER : ICONS;
        guiGraphics.blit(slotIcon, this.leftPos + 14, this.topPos + 29, 68, 44, 0, 0, 34, 22, 34, 22);

        // Render Bag Slot
        boolean isBagHovered = this.isHovering(this.menu.slots.get(0).x, this.menu.slots.get(0).y, 16, 16, mouseX,
                mouseY);
        ResourceLocation bagIcon = isBagHovered ? BAG_HOVER : BAG_ICON;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(this.leftPos + 108, this.topPos + 40, 0);
        guiGraphics.pose().scale(1.5f, 1.5f, 1.0f);
        guiGraphics.blit(bagIcon, 0, 0, 18, 18, 0, 0, 18, 18, 18, 18);
        guiGraphics.pose().popPose();

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
        if (isDialHovered) {
            guiGraphics.renderTooltip(this.font, Component.translatable("gui.analogaudio.radio.volume"),
                    mouseX, mouseY);
        }
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
        if (this.menu.slots.size() >= 2) {
            Slot bagSlot = this.menu.slots.get(0);
            Slot cassetteSlot = this.menu.slots.get(1);

            if (x == bagSlot.x && y == bagSlot.y) {
                return super.isHovering(108, 40, 27, 27, mouseX, mouseY);
            }

            if (x == cassetteSlot.x && y == cassetteSlot.y) {
                if (super.isHovering(108, 40, 25, 25, mouseX, mouseY)) {
                    return false;
                }
                return super.isHovering(14, 29, 68, 44, mouseX, mouseY);
            }
        }
        return super.isHovering(x, y, width, height, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovering(140, 44, 16, 16, mouseX, mouseY)) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                if (this.resetCooldownTicks > 0) {
                    return true;
                }
                this.volume = 0.75f;
                this.dialRotation = (volume / 1.5f * 270.0f) - 135.0f;
                this.resetCooldownTicks = 10; // 0.5 second spam cooldown
                Minecraft.getInstance().getSoundManager()
                        .play(SimpleSoundInstance.forUI(ModSounds.FREQUENCY_TICK.get(), 1.0f + (volume * 0.5f), 1.0f));
                PacketDistributor.sendToServer(new UpdateRadioSettingsC2SPacket(this.menu.getPos(), volume, looping, playing, shuffle, true));
                if (this.menu.getBlockEntity() != null) {
                    this.menu.getBlockEntity().setSettings(volume, looping, playing, shuffle, true);
                }
                return true;
            } else {
                isDraggingDial = true;
                return true;
            }
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
            int menuIndex = this.menu.slots.indexOf(slot);
            if (stack.isEmpty()) {
                if (menuIndex == 1) {
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(14 + 34, 29 + 22, 0);
                    guiGraphics.pose().scale(4.0f, 4.0f, 1.0f);
                    renderGhostItem(guiGraphics, ModItems.CASSETTE_TAPE.get().getDefaultInstance(), -8, -8,
                            this.hoveredSlot == slot);
                    guiGraphics.pose().popPose();
                } else if (menuIndex == 0) {
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(slot.x + 8, slot.y + 8, 0);
                    guiGraphics.pose().scale(1.15f, 1.15f, 1.15f);
                    renderGhostItem(guiGraphics, ModItems.CASSETTE_BAG.get().getDefaultInstance(), -8, -8,
                            this.hoveredSlot == slot);
                    guiGraphics.pose().popPose();
                }
            } else {
                if (menuIndex == 1) {
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(14 + 34, 29 + 22, 0);
                    guiGraphics.pose().scale(4.0f, 4.0f, 1.0f);
                    guiGraphics.renderItem(stack, -8, -8);
                    guiGraphics.pose().popPose();
                    return;
                } else if (menuIndex == 0) {
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(slot.x + 8, slot.y + 8, 0);
                    guiGraphics.pose().scale(1.15f, 1.15f, 1.15f);
                    guiGraphics.renderItem(stack, -8, -8);
                    guiGraphics.pose().popPose();
                    return;
                }
            }
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
