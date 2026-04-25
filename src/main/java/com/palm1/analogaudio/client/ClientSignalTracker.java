package com.palm1.analogaudio.client;

import net.minecraft.world.phys.Vec3;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientSignalTracker {
    public static class SignalInfo {
        public final Vec3 pos;
        public final int frequency;
        public final boolean isSpeaker;
        public long lastUpdateTime;

        public SignalInfo(Vec3 pos, int frequency, boolean isSpeaker) {
            this.pos = pos;
            this.frequency = frequency;
            this.isSpeaker = isSpeaker;
            this.lastUpdateTime = System.currentTimeMillis();
        }
    }

    private static final Map<UUID, SignalInfo> ACTIVE_SIGNALS = new ConcurrentHashMap<>();

    public static void updateSignal(UUID sender, Vec3 pos, int frequency, boolean isSpeaker) {
        ACTIVE_SIGNALS.put(sender, new SignalInfo(pos, frequency, isSpeaker));
    }

    public static SignalInfo getSignal(UUID sender) {
        SignalInfo info = ACTIVE_SIGNALS.get(sender);
        if (info != null && System.currentTimeMillis() - info.lastUpdateTime > 5000) {
            ACTIVE_SIGNALS.remove(sender);
            return null;
        }
        return info;
    }
}
