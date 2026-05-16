package com.palm1.analogaudio.client;

import com.palm1.analogaudio.AnalogAudio;
import com.palm1.analogaudio.client.audio.ClientAudioEngine;
import com.palm1.analogaudio.client.audio.lavaplayer.LavaplayerLoader;
import com.palm1.analogaudio.client.gui.LavaplayerWelcomeScreen;
import com.palm1.analogaudio.config.ModConfig;
import com.palm1.analogaudio.registry.ModDataComponents;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.Minecraft;
import java.util.List;
import java.util.ArrayList;

@EventBusSubscriber(modid = AnalogAudio.MODID, value = Dist.CLIENT)
public class AnalogAudioClientEvents {
    @SubscribeEvent
    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        ClientAudioEngine.prepareForSession();
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof TitleScreen && !ModConfig.Client.lavaplayerDisabled) {
            if (LavaplayerLoader.isMissing()) {
                event.setNewScreen(new LavaplayerWelcomeScreen(event.getNewScreen()));
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientAudioEngine.stopAll("LOGOUT", true);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            ClientAudioEngine.stopAll("LEVEL_UNLOAD", false);
        }
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel().isClientSide()) {
            ClientAudioEngine.prepareForSession();
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(AnalogAudio.MODID)) {
            List<Component> tooltip = event.getToolTip();
            boolean shift = Screen.hasShiftDown();
            String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            String descKey = "tooltip.analogaudio.description." + path;

            if (shift) {
                String highlight = null;
                if (path.equals("walkie_talkie")) {
                    highlight = Minecraft.getInstance().options.keyUse.getTranslatedKeyMessage().getString();
                    Component desc = Component.translatable(descKey, highlight).withStyle(ChatFormatting.GRAY);
                    tooltip.addAll(wrapComponent(desc, 32, highlight, ChatFormatting.YELLOW));
                } else {
                    tooltip.addAll(wrapComponent(Component.translatable(descKey).withStyle(ChatFormatting.GRAY), 32,
                            null, null));
                }

                if (path.equals("cassette_tape")) {
                    var data = stack.get(ModDataComponents.CASSETTE_DATA.get());
                    if (data != null) {
                        String authorName = "???";
                        String authorUuidStr = data.authorUuid();
                        if (authorUuidStr != null && !authorUuidStr.isEmpty()) {
                            try {
                                var uuid = java.util.UUID.fromString(authorUuidStr);
                                var connection = Minecraft.getInstance().getConnection();
                                if (connection != null) {
                                    var info = connection.getPlayerInfo(uuid);
                                    if (info != null) {
                                        authorName = info.getProfile().getName();
                                    } else if (Minecraft.getInstance().player != null
                                            && Minecraft.getInstance().player.getUUID().equals(uuid)) {
                                        authorName = Minecraft.getInstance().player.getName().getString();
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                        tooltip.add(Component.translatable("tooltip.analogaudio.author",
                                Component.literal(authorName).withStyle(ChatFormatting.GRAY))
                                .withStyle(ChatFormatting.WHITE));
                    }
                }
            } else {
                tooltip.add(Component.translatable("tooltip.analogaudio.view_tooltip_description",
                        Component.literal("SHIFT").withStyle(ChatFormatting.YELLOW))
                        .withStyle(ChatFormatting.WHITE));
            }
        }
    }

    private static List<Component> wrapComponent(Component component, int maxWidth, String highlight,
            ChatFormatting highlightColor) {
        String originalText = component.getString();
        List<Component> wrapped = new ArrayList<>();
        String[] lines = originalText.split("\n", -1);

        for (String lineText : lines) {
            if (lineText.isEmpty()) {
                wrapped.add(Component.empty());
                continue;
            }

            String[] words = lineText.split(" ");
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                if (currentLine.length() + word.length() + 1 > maxWidth) {
                    wrapped.add(applyHighlight(currentLine.toString().trim(), component.getStyle(), highlight,
                            highlightColor));
                    currentLine = new StringBuilder();
                }
                currentLine.append(word).append(" ");
            }

            if (currentLine.length() > 0) {
                wrapped.add(
                        applyHighlight(currentLine.toString().trim(), component.getStyle(), highlight, highlightColor));
            }
        }

        return wrapped;
    }

    private static Component applyHighlight(String text, Style style, String highlight,
            ChatFormatting highlightColor) {
        if (highlight == null || !text.contains(highlight)) {
            return Component.literal(text).withStyle(style);
        }

        int index = text.indexOf(highlight);
        Component result = Component.literal(text.substring(0, index)).withStyle(style);
        result = result.copy().append(Component.literal(highlight).withStyle(highlightColor));
        result = result.copy().append(Component.literal(text.substring(index + highlight.length())).withStyle(style));
        return result;
    }
}
