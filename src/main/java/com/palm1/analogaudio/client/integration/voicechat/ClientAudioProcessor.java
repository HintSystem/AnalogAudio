package com.palm1.analogaudio.client.integration.voicechat;

import de.maxhenkel.voicechat.api.events.ClientReceiveSoundEvent;
import com.palm1.analogaudio.client.ClientSignalTracker;
import com.palm1.analogaudio.client.audio.CommonAudioProcessor;

import java.util.UUID;

public class ClientAudioProcessor {

    public static void onSoundReceived(ClientReceiveSoundEvent event) {
        UUID senderId = event.getId();
        ClientSignalTracker.SignalInfo signal = ClientSignalTracker.getSignal(senderId);

        if (signal != null) {
            short[] audio = event.getRawAudio();
            CommonAudioProcessor.processAudio(audio, senderId, signal);
            event.setRawAudio(audio);
        }
    }

    public static void registerEvents(de.maxhenkel.voicechat.api.events.EventRegistration registration) {
        registration.registerEvent(ClientReceiveSoundEvent.EntitySound.class, ClientAudioProcessor::onSoundReceived);
        registration.registerEvent(ClientReceiveSoundEvent.LocationalSound.class,
                ClientAudioProcessor::onSoundReceived);
        registration.registerEvent(ClientReceiveSoundEvent.StaticSound.class, ClientAudioProcessor::onSoundReceived);
    }
}
