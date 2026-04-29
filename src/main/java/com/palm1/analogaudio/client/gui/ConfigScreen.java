package com.palm1.analogaudio.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import com.palm1.analogaudio.config.ModConfig;

public class ConfigScreen extends Screen {
    private final Screen parent;

    public ConfigScreen(Screen parent) {
        super(Component.literal("Analog Audio Configuration"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int y = this.height / 4 + 24;
        int centerX = this.width / 2;

        this.addRenderableWidget(CycleButton.onOffBuilder(ModConfig.CLIENT_CONFIG.enableCassetteAnimation.get())
                .create(centerX - 155, y, 150, 20, Component.literal("Cassette Animation"), (button, value) -> {
                    ModConfig.CLIENT_CONFIG.enableCassetteAnimation.set(value);
                    ModConfig.CLIENT_CONFIG.enableCassetteAnimation.save();
                }));

        this.addRenderableWidget(CycleButton.onOffBuilder(ModConfig.CLIENT_CONFIG.enableSpeakerAnimation.get())
                .create(centerX + 5, y, 150, 20, Component.literal("Speaker Animation"), (button, value) -> {
                    ModConfig.CLIENT_CONFIG.enableSpeakerAnimation.set(value);
                    ModConfig.CLIENT_CONFIG.enableSpeakerAnimation.save();
                }));

        y += 24;
        this.addRenderableWidget(CycleButton.onOffBuilder(ModConfig.CLIENT_CONFIG.renderCassetteText.get())
                .create(centerX - 75, y, 150, 20, Component.literal("Cassette Text"), (button, value) -> {
                    ModConfig.CLIENT_CONFIG.renderCassetteText.set(value);
                    ModConfig.CLIENT_CONFIG.renderCassetteText.save();
                }));

        y += 44;
        this.addRenderableWidget(CycleButton.onOffBuilder(ModConfig.SERVER_CONFIG.enableWalkieFiltering.get())
                .create(centerX - 75, y, 150, 20, Component.literal("Walkie Filtering"), (button, value) -> {
                    ModConfig.SERVER_CONFIG.enableWalkieFiltering.set(value);
                    ModConfig.SERVER_CONFIG.enableWalkieFiltering.save();
                }));
        y += 40;

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (button) -> {
            this.minecraft.setScreen(this.parent);
        }).bounds(centerX - 100, y, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        int y = this.height / 4 + 8;
        graphics.drawCenteredString(this.font, Component.literal("Client Settings"), this.width / 2, y, 0xAAAAAA);

        graphics.drawCenteredString(this.font, Component.literal("Server Settings"), this.width / 2, y + 72,
                0xAAAAAA);
    }
}
