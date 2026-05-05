package com.palm1.analogaudio.client.gui;

import com.palm1.analogaudio.config.ModConfig;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class ModConfigScreen {

        public static Screen create(Screen parent) {
                return YetAnotherConfigLib.createBuilder()
                                .title(Component.translatable("config.analogaudio.title"))
                                .category(ConfigCategory.createBuilder()
                                                .name(Component.translatable("config.analogaudio.category.client"))
                                                .option(Option.<Boolean>createBuilder()
                                                                .name(Component.translatable(
                                                                                "config.analogaudio.enableCassetteAnimation"))
                                                                .description(OptionDescription.of(Component
                                                                                .translatable("config.analogaudio.enableCassetteAnimation.description")))
                                                                .binding(true, () -> ModConfig.Client.enableCassetteAnimation,
                                                                                val -> ModConfig.Client.enableCassetteAnimation = val)
                                                                .controller(TickBoxControllerBuilder::create)
                                                                .build())
                                                .option(Option.<Boolean>createBuilder()
                                                                .name(Component.translatable(
                                                                                "config.analogaudio.enableSpeakerAnimation"))
                                                                .description(OptionDescription.of(Component
                                                                                .translatable("config.analogaudio.enableSpeakerAnimation.description")))
                                                                .binding(true, () -> ModConfig.Client.enableSpeakerAnimation,
                                                                                val -> ModConfig.Client.enableSpeakerAnimation = val)
                                                                .controller(TickBoxControllerBuilder::create)
                                                                .build())
                                                .option(Option.<Boolean>createBuilder()
                                                                .name(Component.translatable(
                                                                                "config.analogaudio.renderCassetteText"))
                                                                .description(OptionDescription.of(Component
                                                                                .translatable("config.analogaudio.renderCassetteText.description")))
                                                                .binding(true, () -> ModConfig.Client.renderCassetteText,
                                                                                val -> ModConfig.Client.renderCassetteText = val)
                                                                .controller(TickBoxControllerBuilder::create)
                                                                .build())
                                                .build())
                                .category(ConfigCategory.createBuilder()
                                                .name(Component.translatable("config.analogaudio.category.server"))
                                                .option(ListOption.<String>createBuilder()
                                                                .name(Component.translatable(
                                                                                "config.analogaudio.whitelistedUrls"))
                                                                .description(OptionDescription.of(Component
                                                                                .translatable("config.analogaudio.whitelistedUrls.description")))
                                                                .binding(
                                                                                List.of("youtube.com", "youtu.be",
                                                                                                "soundcloud.com",
                                                                                                "bandcamp.com"),
                                                                                () -> ModConfig.Server.whitelistedUrls,
                                                                                val -> ModConfig.Server.whitelistedUrls = val)
                                                                .controller(StringControllerBuilder::create)
                                                                .initial("")
                                                                .build())
                                                .option(Option.<Boolean>createBuilder()
                                                                .name(Component.translatable(
                                                                                "config.analogaudio.whitelistAsBlacklist"))
                                                                .description(OptionDescription.of(Component
                                                                                .translatable("config.analogaudio.whitelistAsBlacklist.description")))
                                                                .binding(false, () -> ModConfig.Server.whitelistAsBlacklist,
                                                                                val -> ModConfig.Server.whitelistAsBlacklist = val)
                                                                .controller(TickBoxControllerBuilder::create)
                                                                .build())
                                                .option(Option.<Boolean>createBuilder()
                                                                .name(Component.translatable(
                                                                                "config.analogaudio.enableWalkieFiltering"))
                                                                .description(OptionDescription.of(Component
                                                                                .translatable("config.analogaudio.enableWalkieFiltering.description")))
                                                                .binding(true, () -> ModConfig.Server.enableWalkieFiltering,
                                                                                val -> ModConfig.Server.enableWalkieFiltering = val)
                                                                .controller(TickBoxControllerBuilder::create)
                                                                .build())
                                                .option(Option.<Boolean>createBuilder()
                                                                .name(Component.translatable(
                                                                                "config.analogaudio.allowFileUploads"))
                                                                .description(OptionDescription.of(Component
                                                                                .translatable("config.analogaudio.allowFileUploads.description")
                                                                                .append(Component.literal("\n\n"))
                                                                                .append(Component.translatable(
                                                                                                "config.analogaudio.warning")
                                                                                                .withStyle(ChatFormatting.BOLD,
                                                                                                                ChatFormatting.RED))
                                                                                .append(Component.translatable(
                                                                                                "config.analogaudio.allowFileUploads.disclaimer",
                                                                                                Component.literal(
                                                                                                                com.palm1.analogaudio.util.AudioUploader.FILE_HOST_URL)
                                                                                                                .withStyle(ChatFormatting.RED)))))
                                                                .binding(false, () -> ModConfig.Server.allowFileUploads,
                                                                                val -> ModConfig.Server.allowFileUploads = val)
                                                                .controller(TickBoxControllerBuilder::create)
                                                                .build())
                                                .build())
                                .save(ModConfig::save)
                                .build()
                                .generateScreen(parent);
        }
}
