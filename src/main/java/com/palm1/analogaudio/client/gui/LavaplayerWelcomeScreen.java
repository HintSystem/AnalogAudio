package com.palm1.analogaudio.client.gui;

import com.palm1.analogaudio.client.audio.lavaplayer.LavaplayerLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public final class LavaplayerWelcomeScreen extends Screen {

    private final Screen lastScreen;
    private final Component messageText;

    private int ticksUntilEnable;
    private Button disableButton;
    private Button installButton;
    private Button manualButton;

    private MultiLineLabel messageLabel;

    public LavaplayerWelcomeScreen(final Screen lastScreen) {
        super(Component.translatable("gui.analogaudio.welcome.title").withStyle(ChatFormatting.GOLD,
                ChatFormatting.BOLD));
        this.lastScreen = lastScreen;
        this.messageText = Component.translatable("gui.analogaudio.welcome.message", LavaplayerLoader.getVersion());
        this.ticksUntilEnable = 40;
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinForNarration(super.getNarrationMessage(), this.messageText);
    }

    @Override
    protected void init() {
        super.init();
        final int buttonWidth = 200;
        final int buttonHeight = 20;
        final int centerX = this.width / 2;
        int bottomY = this.height * 3 / 6;

        int centerButton = centerX - buttonWidth / 2;

        this.installButton = this.addRenderableWidget(
                Button.builder(Component.translatable("gui.analogaudio.welcome.install")
                        .withStyle(ChatFormatting.GREEN), btn -> {
                            Minecraft.getInstance().setScreen(new LavaplayerProgressScreen(lastScreen));
                        })
                        .bounds(centerButton, bottomY, buttonWidth, buttonHeight)
                        .build());

        bottomY += buttonHeight + 4;
        this.manualButton = this.addRenderableWidget(
                Button.builder(Component.translatable("gui.analogaudio.welcome.manual"), btn -> {
                    this.handleComponentClicked(Style.EMPTY.withClickEvent(
                            new ClickEvent(ClickEvent.Action.OPEN_URL, LavaplayerLoader.getGitHubUrl())));
                })
                        .bounds(centerButton, bottomY, buttonWidth, buttonHeight)
                        .build());

        bottomY += buttonHeight + 4;
        this.disableButton = this.addRenderableWidget(
                Button.builder(Component.translatable("gui.analogaudio.welcome.decline"), btn -> {
                    Minecraft.getInstance().setScreen(lastScreen);
                })
                        .bounds(centerButton, bottomY, buttonWidth, buttonHeight)
                        .build());

        this.messageLabel = MultiLineLabel.create(this.font, this.messageText, this.width - 50);

        activateIfReady();
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 30, 0xFFFFFF);
        this.messageLabel.renderCentered(graphics, this.width / 2, 55);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.ticksUntilEnable > 0) {
            this.ticksUntilEnable--;
            activateIfReady();
        }
    }

    private void activateIfReady() {
        if (this.ticksUntilEnable <= 0) {
            this.installButton.active = true;
            this.manualButton.active = true;
            this.disableButton.active = true;
        } else {
            this.installButton.active = false;
            this.manualButton.active = false;
            this.disableButton.active = false;
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.lastScreen);
    }
}
