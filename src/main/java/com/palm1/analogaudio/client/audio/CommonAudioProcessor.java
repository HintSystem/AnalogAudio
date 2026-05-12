package com.palm1.analogaudio.client.audio;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.palm1.analogaudio.block.entity.SpeakerBlockEntity;
import com.palm1.analogaudio.client.ClientSignalTracker;
import com.palm1.analogaudio.config.ModConfig;
import com.palm1.analogaudio.integration.voicechat.SpeakerInstance;
import com.palm1.analogaudio.integration.voicechat.SpeakerManager;

import net.minecraft.client.Minecraft;

// Shared logic for processing walkie talkie audio for voice chat mods on client.
public class CommonAudioProcessor {
    private static final Map<UUID, ClientAudioFilter> FILTERS = new ConcurrentHashMap<>();

    public static void processAudio(short[] audio, UUID sourceId, ClientSignalTracker.SignalInfo signal) {
        if (audio.length == 0)
            return;

        if (signal.isSpeaker) {
            long sum = 0;
            for (short s : audio) {
                sum += (long) s * s;
            }
            float rms = (float) Math.sqrt((double) sum / audio.length) / 32768f;

            if (rms < 0.01f)
                rms = 0;

            float volume = rms * 2.0f;

            for (SpeakerInstance speaker : SpeakerManager.getSpeakersOnFrequency(signal.frequency)) {
                if (speaker.getPosition().distanceToSqr(signal.pos) < 1.0) {
                    if (speaker instanceof SpeakerBlockEntity sbe && sbe.getLevel().isClientSide()) {
                        sbe.setScale(1.0f + volume);
                    }
                }
            }
        }

        if (ModConfig.Synced.enableWalkieFiltering && Minecraft.getInstance().player != null) {
            double distance = Minecraft.getInstance().player.position().distanceTo(signal.pos);
            ClientAudioFilter filter = FILTERS.computeIfAbsent(sourceId, k -> new ClientAudioFilter());
            filter.apply(audio, distance, signal.frequency);
        }
    }

    public static void cleanUp(UUID sourceId) {
        FILTERS.remove(sourceId);
    }
}
