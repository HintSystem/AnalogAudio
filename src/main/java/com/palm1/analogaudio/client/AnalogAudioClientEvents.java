package com.palm1.analogaudio.client;

import com.palm1.analogaudio.AnalogAudio;
import com.palm1.analogaudio.client.audio.ClientAudioEngine;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

@EventBusSubscriber(modid = AnalogAudio.MODID, value = Dist.CLIENT)
public class AnalogAudioClientEvents {
    @SubscribeEvent
    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        ClientAudioEngine.prepareForSession();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientAudioEngine.stopAll("LOGOUT");
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            ClientAudioEngine.stopAll("LEVEL_UNLOAD");
        }
    }
}
