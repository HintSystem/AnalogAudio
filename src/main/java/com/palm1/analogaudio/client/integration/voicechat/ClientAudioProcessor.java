package com.palm1.analogaudio.client.integration.voicechat;

import de.maxhenkel.voicechat.api.events.ClientReceiveSoundEvent;
import com.palm1.analogaudio.client.ClientHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.palm1.analogaudio.block.entity.SpeakerBlockEntity;
import com.palm1.analogaudio.client.ClientSignalTracker;
import com.palm1.analogaudio.integration.voicechat.SpeakerInstance;
import com.palm1.analogaudio.integration.voicechat.SpeakerManager;

public class ClientAudioProcessor {
    private static final Map<UUID, ClientAudioFilter> FILTERS = new HashMap<>();

    public static void onSoundReceived(ClientReceiveSoundEvent event) {
        UUID senderId = event.getId();

        ClientSignalTracker.SignalInfo signal = ClientSignalTracker.getSignal(senderId);

        if (signal != null) {
            short[] audio = event.getRawAudio();
            if (audio.length == 0)
                return;

            long sum = 0;
            for (short s : audio)
                sum += (s * s);
            float rms = (float) Math.sqrt(sum / (float) audio.length) / 32768f;
            float scaleIncrease = rms * 2.0f;

            if (signal.isSpeaker) {
                for (SpeakerInstance s : SpeakerManager.getSpeakersOnFrequency(signal.frequency)) {
                    if (s.getPosition().distanceToSqr(signal.pos) < 0.1) {
                        if (s instanceof SpeakerBlockEntity speaker && speaker.getLevel().isClientSide()) {
                            speaker.setScale(1.0f + scaleIncrease);
                        }
                    }
                }
            }

            ClientAudioFilter filter = FILTERS.computeIfAbsent(senderId, k -> new ClientAudioFilter());

            double distance = 0;
            net.minecraft.world.entity.player.Player player = ClientHooks.getClientPlayer();
            if (player != null) {
                distance = player.position().distanceTo(signal.pos);
            }

            filter.apply(audio, distance, signal.frequency);
            event.setRawAudio(audio);
        } else {
        }
    }

    public static void registerEvents(de.maxhenkel.voicechat.api.events.EventRegistration registration) {
        registration.registerEvent(ClientReceiveSoundEvent.EntitySound.class, ClientAudioProcessor::onSoundReceived);
        registration.registerEvent(ClientReceiveSoundEvent.LocationalSound.class, ClientAudioProcessor::onSoundReceived);
        registration.registerEvent(ClientReceiveSoundEvent.StaticSound.class, ClientAudioProcessor::onSoundReceived);
    }
}
