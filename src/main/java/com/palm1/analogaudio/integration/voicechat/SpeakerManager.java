package com.palm1.analogaudio.integration.voicechat;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.palm1.analogaudio.AnalogAudio;

public class SpeakerManager {
    private static final ConcurrentHashMap<Integer, Set<SpeakerInstance>> CLIENT_SPEAKERS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, Set<SpeakerInstance>> SERVER_SPEAKERS = new ConcurrentHashMap<>();

    private static ConcurrentHashMap<Integer, Set<SpeakerInstance>> getMap(SpeakerInstance speaker) {
        if (speaker.getLevel() != null && speaker.getLevel().isClientSide()) {
            return CLIENT_SPEAKERS;
        } else {
            return SERVER_SPEAKERS;
        }
    }

    public static void addSpeaker(SpeakerInstance speaker) {
        int freq = speaker.getFrequency();
        Object id = speaker.getIdentity();
        String className = speaker.getClass().getSimpleName();
        AnalogAudio.LOGGER
                .info("Registering Speaker | Type: {} | Identity: {} | Freq: {}", className, id, freq);
        getMap(speaker).computeIfAbsent(freq, k -> Collections.synchronizedSet(new HashSet<>())).add(speaker);
    }

    public static void removeSpeaker(SpeakerInstance speaker) {
        ConcurrentHashMap<Integer, Set<SpeakerInstance>> map = getMap(speaker);
        Set<SpeakerInstance> set = map.get(speaker.getFrequency());
        if (set != null) {
            set.remove(speaker);
            AnalogAudio.LOGGER.info("Unregistering Speaker | Identity: {}",
                    speaker.getIdentity());
            if (set.isEmpty()) {
                map.remove(speaker.getFrequency());
            }
        }
    }

    public static Set<SpeakerInstance> getSpeakersOnFrequency(int frequency, boolean clientSide) {
        return (clientSide ? CLIENT_SPEAKERS : SERVER_SPEAKERS).getOrDefault(frequency, Collections.emptySet());
    }

    public static void updateSpeakerFrequency(SpeakerInstance speaker, int oldFreq, int newFreq) {
        ConcurrentHashMap<Integer, Set<SpeakerInstance>> map = getMap(speaker);
        Set<SpeakerInstance> oldSet = map.get(oldFreq);
        if (oldSet != null) {
            oldSet.remove(speaker);
            if (oldSet.isEmpty()) {
                map.remove(oldFreq);
            }
        }
        map.computeIfAbsent(newFreq, k -> Collections.synchronizedSet(new HashSet<>())).add(speaker);
    }

    public static void addDynamicSpeaker(SpeakerInstance speaker) {
        addSpeaker(speaker);
    }

    public static void removeDynamicSpeaker(SpeakerInstance speaker) {
        removeSpeaker(speaker);
    }
}
