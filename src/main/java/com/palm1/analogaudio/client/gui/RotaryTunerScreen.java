package com.palm1.analogaudio.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.palm1.analogaudio.AnalogAudio;
import com.palm1.analogaudio.registry.ModSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class RotaryTunerScreen extends Screen {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/rotary_frequency.png");

    private int ticksOpen = 0;
    private int soundCoolDown = 0;
    private final Consumer<Integer> frequencySaver;
    private int frequency;

    private static final float POINTER_ANGLE = 214.0f;
    private static final float START_ANGLE = 214.0f;
    private static final float STEP_ANGLE = 28.5f;
    private static final float HOLE_RADIUS = 66.5f;

    private static final int HOLE_U = 210;
    private static final int HOLE_V = 4;
    private static final int HOLE_SIZE = 38;

    private static final int MAX_FREQUENCY = 255;
    private static final int HOLD_THRESHOLD = 30;

    private float currentRotation = 0;
    private float targetRotation = 0;

    private int holdTimer = 0;
    private int holdDirection = 0;

    private int resetTimer = 0;
    private boolean isResetting = false;

    public RotaryTunerScreen(int initialFrequency, Consumer<Integer> frequencySaver) {
        super(Component.translatable("gui.analogaudio.rotary_tuner.title"));
        this.frequency = Mth.clamp(initialFrequency, 1, MAX_FREQUENCY);
        this.frequencySaver = frequencySaver;
        this.targetRotation = getAngleForFrequency(this.frequency);
        this.currentRotation = this.targetRotation;
    }

    @Override
    protected void init() {
        super.init();
        setCursorToFreq(this.frequency);
    }

    private void setCursorToFreq(int freq) {
        int centerX = Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2;
        int centerY = Minecraft.getInstance().getWindow().getGuiScaledHeight() / 2;

        int holeIdx = (freq - 1) % 9;
        float nativeHoleAngle = START_ANGLE + holeIdx * STEP_ANGLE;
        double rad = Math.toRadians(nativeHoleAngle - 90);
        float hX = (float) (Math.cos(rad) * HOLE_RADIUS);
        float hY = (float) (Math.sin(rad) * HOLE_RADIUS);
        setCursor(new Vec2(centerX + hX, centerY + hY));
    }

    private float getAngleForFrequency(int freq) {
        return POINTER_ANGLE - (START_ANGLE + ((freq - 1) % 9) * STEP_ANGLE);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int darkAlpha = (int) (0x50 * Math.min(1, (ticksOpen + partialTicks) / 20f));
        int backgroundColor = (darkAlpha << 24) | 0x101010;
        graphics.fillGradient(0, 0, this.width, this.height, backgroundColor, backgroundColor);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        float visualRotation = Mth.lerp(0.4f, currentRotation, targetRotation);
        currentRotation = visualRotation;

        if (holdTimer > 0) {
            visualRotation += holdDirection * (holdTimer / 2.0f);
        }

        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 100);
        graphics.pose().pushPose();
        graphics.pose().rotateAround(com.mojang.math.Axis.ZP.rotationDegrees(visualRotation), 0, 0, 0);
        graphics.blit(TEXTURE, -90, -90, 0, 0, 180, 180, 256, 256);

        for (int i = 1; i <= 9; i++) {
            float holeAngleOnWheel = START_ANGLE + (i - 1) * STEP_ANGLE;
            double rad = Math.toRadians(holeAngleOnWheel - 90);
            float hX = (float) (Math.cos(rad) * HOLE_RADIUS);
            float hY = (float) (Math.sin(rad) * HOLE_RADIUS);

            graphics.blit(TEXTURE, (int) hX - HOLE_SIZE / 2, (int) hY - HOLE_SIZE / 2, HOLE_U, HOLE_V, HOLE_SIZE,
                    HOLE_SIZE, 256, 256);
        }
        graphics.pose().popPose();

        int page = (this.frequency - 1) / 9;
        for (int i = 1; i <= 9; i++) {
            int val = page * 9 + i;
            if (val > MAX_FREQUENCY)
                continue;

            float worldHoleAngle = START_ANGLE + (i - 1) * STEP_ANGLE + visualRotation;
            double rad = Math.toRadians(worldHoleAngle - 90);
            float hX = (float) (Math.cos(rad) * HOLE_RADIUS);
            float hY = (float) (Math.sin(rad) * HOLE_RADIUS);

            String text = String.valueOf(val);
            int tw = font.width(text);
            graphics.drawString(font, text, (int) hX - tw / 2, (int) hY - 4, 0x442000, false);
        }

        graphics.pose().pushPose();
        graphics.pose().rotateAround(com.mojang.math.Axis.ZP.rotationDegrees(POINTER_ANGLE), 0, 0, 0);
        graphics.blit(TEXTURE, -8, -80, 181, 0, 16, 80, 256, 256);
        graphics.pose().popPose();

        float shakeX = 0, shakeY = 0;
        if (resetTimer > 0) {
            shakeX = (float) ((Math.random() - 0.5) * (resetTimer / 15f));
            shakeY = (float) ((Math.random() - 0.5) * (resetTimer / 15f));
        }
        graphics.blit(TEXTURE, (int) (-12 + shakeX), (int) (-12 + shakeY), 181, 81, 24, 24, 256, 256);
        graphics.pose().popPose();

        if (ticksOpen > 1) {
            updateFrequencyFromMouse(mouseX, mouseY);
        }
    }

    private void setCursor(Vec2 pos) {
        Window window = minecraft.getWindow();
        double guiScale = window.getGuiScale();
        GLFW.glfwSetCursorPos(window.getWindow(), pos.x * guiScale, pos.y * guiScale);
    }

    private void updateFrequencyFromMouse(int mouseX, int mouseY) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist <= 30) {
            this.holdDirection = 0;
            this.isResetting = true;
        } else if (dist < 120) {
            this.isResetting = false;
            double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90;
            if (angle < 0)
                angle += 360;

            int page = (this.frequency - 1) / 9;
            int bestFreq = this.frequency;

            this.holdDirection = 0;
            if (angle > 82.5 && angle < 213.5) {
                if ((this.frequency % 9 == 0) && this.frequency < MAX_FREQUENCY) {
                    this.holdDirection = 1;
                    bestFreq = this.frequency;
                } else if ((this.frequency % 9 == 1) && this.frequency > 9) {
                    this.holdDirection = -1;
                    bestFreq = this.frequency;
                }
            }

            if (this.holdDirection == 0) {
                float bestD = Float.MAX_VALUE;
                for (int i = 1; i <= 9; i++) {
                    int val = page * 9 + i;
                    if (val > MAX_FREQUENCY)
                        continue;

                    float nativeHoleAngle = START_ANGLE + (i - 1) * STEP_ANGLE;
                    float diff = Math.abs(Mth.degreesDifference(nativeHoleAngle, (float) angle));
                    if (diff < bestD) {
                        bestD = diff;
                        bestFreq = val;
                    }
                }
            }

            if (bestFreq != this.frequency) {
                this.frequency = bestFreq;
                this.targetRotation = getAngleForFrequency(this.frequency);
                setCursorToFreq(this.frequency);

                if (soundCoolDown == 0) {
                    float pitch = 0.5f + ((this.frequency % 9) / 9f);
                    minecraft.getSoundManager()
                            .play(SimpleSoundInstance.forUI(ModSounds.FREQUENCY_TICK.get(), pitch, 1.0F));
                    soundCoolDown = 2;
                }
            }
        } else {
            this.isResetting = false;
            this.holdDirection = 0;
        }
    }

    @Override
    public void tick() {
        ticksOpen++;
        if (soundCoolDown > 0)
            soundCoolDown--;

        if (this.holdDirection != 0) {
            this.holdTimer++;
            if (this.holdTimer >= HOLD_THRESHOLD) {
                int currentPage = (this.frequency - 1) / 9;
                int nextPage = currentPage + this.holdDirection;

                if (nextPage >= 0 && nextPage * 9 < MAX_FREQUENCY) {
                    if (this.holdDirection > 0) {
                        this.frequency = nextPage * 9 + 1;
                    } else {
                        this.frequency = nextPage * 9 + 9;
                    }
                    this.targetRotation = getAngleForFrequency(this.frequency);
                    setCursorToFreq(this.frequency);
                    minecraft.getSoundManager()
                            .play(SimpleSoundInstance.forUI(ModSounds.FREQUENCY_SELECT.get(), 1.2F, 1.0F));
                    this.holdTimer = 0;
                }
            }
        } else {
            this.holdTimer = 0;
        }

        if (this.isResetting) {
            this.resetTimer++;
            if (this.resetTimer >= 40) {
                this.frequency = 1;
                this.targetRotation = getAngleForFrequency(1);
                setCursorToFreq(1);
                minecraft.getSoundManager()
                        .play(SimpleSoundInstance.forUI(ModSounds.FREQUENCY_SELECT.get(), 0.8F, 1.0F));
                this.resetTimer = 0;
                this.isResetting = false;
            }
        } else {
            this.resetTimer = 0;
        }

        if (!isUseKeyDown()) {
            this.onClose();
        }
    }

    private boolean isUseKeyDown() {
        InputConstants.Key key = minecraft.options.keyUse.getKey();
        long window = minecraft.getWindow().getWindow();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        } else if (key.getType() == InputConstants.Type.KEYSYM) {
            return GLFW.glfwGetKey(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        return false;
    }

    @Override
    public void onClose() {
        this.frequencySaver.accept(this.frequency);
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.FREQUENCY_SELECT.get(), 1.0F, 1.0F));
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
    }
}
