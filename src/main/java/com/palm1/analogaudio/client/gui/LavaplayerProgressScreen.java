package com.palm1.analogaudio.client.gui;

import com.palm1.analogaudio.client.audio.lavaplayer.LavaplayerLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class LavaplayerProgressScreen extends Screen {

    private final Screen lastScreen;
    private int progress = 0;
    private State state = State.DOWNLOADING;

    private Button continueButton;
    private Button retryButton;
    private Button proceedButton;

    public LavaplayerProgressScreen(Screen lastScreen) {
        super(Component.translatable("gui.analogaudio.welcome.title").withStyle(ChatFormatting.GOLD,
                ChatFormatting.BOLD));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int buttonWidth = 200;
        int buttonHeight = 20;

        this.continueButton = this.addRenderableWidget(
                Button.builder(Component.translatable("gui.analogaudio.welcome.continue"), btn -> {
                    Minecraft.getInstance().setScreen(lastScreen);
                })
                        .bounds(centerX - buttonWidth / 2, centerY + 30, buttonWidth, buttonHeight)
                        .build());

        this.retryButton = this.addRenderableWidget(
                Button.builder(Component.translatable("gui.analogaudio.welcome.retry"), btn -> {
                    this.startDownload();
                })
                        .bounds(centerX - buttonWidth / 2, centerY + 20, buttonWidth, buttonHeight)
                        .build());

        this.proceedButton = this.addRenderableWidget(
                Button.builder(Component.translatable("gui.analogaudio.welcome.decline"), btn -> {
                    Minecraft.getInstance().setScreen(lastScreen);
                })
                        .bounds(centerX - buttonWidth / 2, centerY + 44, buttonWidth, buttonHeight)
                        .build());

        updateButtonVisibility();

        if (state == State.DOWNLOADING && progress == 0) {
            startDownload();
        }
    }

    private void startDownload() {
        this.state = State.DOWNLOADING;
        this.progress = 0;
        updateButtonVisibility();
        LavaplayerLoader.triggerDownload(
                p -> this.progress = p,
                success -> {
                    this.state = success ? State.SUCCESS : State.FAILURE;
                    updateButtonVisibility();
                });
    }

    private void updateButtonVisibility() {
        if (this.continueButton == null)
            return;
        this.continueButton.visible = (state == State.SUCCESS);
        this.retryButton.visible = (state == State.FAILURE);
        this.proceedButton.visible = (state == State.FAILURE);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        graphics.drawCenteredString(this.font, this.title, centerX, 40, 0xFFFFFF);

        Component statusText;
        int color = 0xFFFFFF;

        switch (state) {
            case DOWNLOADING -> {
                statusText = Component.translatable("gui.analogaudio.welcome.downloading").append(" " + progress + "%");
            }
            case SUCCESS -> {
                statusText = Component.translatable("gui.analogaudio.welcome.success").withStyle(ChatFormatting.GREEN);
                color = 0x55FF55;
            }
            case FAILURE -> {
                statusText = Component.translatable("gui.analogaudio.welcome.download_fail")
                        .withStyle(ChatFormatting.RED);
                color = 0xFF5555;
            }
            default -> statusText = Component.empty();
        }

        graphics.drawCenteredString(this.font, statusText, centerX, centerY - 10, color);

        if (state == State.DOWNLOADING) {
            renderProgressBar(graphics, centerX - 100, centerY + 5, 200, 10, progress);
        }
    }

    private void renderProgressBar(GuiGraphics graphics, int x, int y, int width, int height, int progress) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF555555);
        graphics.fill(x, y, x + width, y + height, 0xFF000000);
        int fillWidth = (int) (width * (progress / 100.0));
        graphics.fill(x, y, x + fillWidth, y + height, 0xFF55FF55);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return state != State.DOWNLOADING;
    }

    private enum State {
        DOWNLOADING, SUCCESS, FAILURE
    }
}
