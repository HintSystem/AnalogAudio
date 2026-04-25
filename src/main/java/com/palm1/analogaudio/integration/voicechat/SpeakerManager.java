package com.palm1.analogaudio.integration.voicechat;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.palm1.analogaudio.AnalogAudio;

public class SpeakerManager {
    private static final ConcurrentHashMap<Integer, Set<SpeakerInstance>> SPEAKERS = new ConcurrentHashMap<>();

    public static void addSpeaker(SpeakerInstance speaker) {
        int freq = speaker.getFrequency();
        Object id = speaker.getIdentity();
        String className = speaker.getClass().getSimpleName();
        AnalogAudio.LOGGER
                .info("Registering Speaker | Type: {} | Identity: {} | Freq: {}", className, id, freq);
        SPEAKERS.computeIfAbsent(freq, k -> Collections.synchronizedSet(new HashSet<>())).add(speaker);
    }

    public static void removeSpeaker(SpeakerInstance speaker) {
        Set<SpeakerInstance> set = SPEAKERS.get(speaker.getFrequency());
        if (set != null) {
            set.remove(speaker);
            AnalogAudio.LOGGER.info("Unregistering Speaker | Identity: {}",
                    speaker.getIdentity());
            if (set.isEmpty()) {
                SPEAKERS.remove(speaker.getFrequency());
            }
        }
    }

    public static Set<SpeakerInstance> getSpeakersOnFrequency(int frequency) {
        return SPEAKERS.getOrDefault(frequency, Collections.emptySet());
    }

    public static void updateSpeakerFrequency(SpeakerInstance speaker, int oldFreq, int newFreq) {
        Set<SpeakerInstance> oldSet = SPEAKERS.get(oldFreq);
        if (oldSet != null) {
            oldSet.remove(speaker);
            if (oldSet.isEmpty()) {
                SPEAKERS.remove(oldFreq);
            }
        }
        SPEAKERS.computeIfAbsent(newFreq, k -> Collections.synchronizedSet(new HashSet<>())).add(speaker);
    }

    public static void addDynamicSpeaker(SpeakerInstance speaker) {
        addSpeaker(speaker);
    }

    public static void removeDynamicSpeaker(SpeakerInstance speaker) {
        removeSpeaker(speaker);
    }
}
