package com.palm1.analogaudio.client.audio;

import com.palm1.analogaudio.AnalogAudio;
import com.palm1.analogaudio.client.audio.api.IRadioStreamer;
import com.palm1.analogaudio.client.audio.lavaplayer.LavaplayerLoader;
import com.palm1.analogaudio.integration.SableCompat;
import com.palm1.analogaudio.item.CassetteData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ClientAudioEngine {
    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(1, r -> {
        Thread t = new Thread(r, "AnalogAudio-Downloader");
        t.setDaemon(true);
        return t;
    });
    private static final Map<Object, IRadioStreamer> PLAYING = new ConcurrentHashMap<>();
    private static final Set<Object> FAILED = Collections
            .newSetFromMap(new ConcurrentHashMap<>());
    private static final Map<Object, Long> LAST_TICKED = new ConcurrentHashMap<>();
    private static final Map<Object, Long> LAST_START_TIMES = new ConcurrentHashMap<>();
    private static volatile boolean active = true;

    static {
        EXECUTOR.scheduleAtFixedRate(() -> {
            if (!active)
                return;
            long now = System.currentTimeMillis();
            for (Map.Entry<Object, Long> entry : LAST_TICKED.entrySet()) {
                if (now - entry.getValue() > 2000) {
                    Object identity = entry.getKey();
                    AnalogAudio.LOGGER.warn("Streamer {} is stagnant. Force-killing ghost audio.",
                            identity);
                    stopRadio(identity);
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    public static float getLoudness(Object identity) {
        IRadioStreamer streamer = PLAYING.get(identity);
        return (streamer != null) ? streamer.getAmplitude() : 0f;
    }

    public static void tickRadio(Object identity, Vec3 pos, CassetteData data, long startTime,
            float volume, boolean looping) {
        if (!active || FAILED.contains(identity))
            return;

        LAST_TICKED.put(identity, System.currentTimeMillis());

        IRadioStreamer streamer = PLAYING.get(identity);
        Long lastStartTime = LAST_START_TIMES.get(identity);
        boolean timeChanged = lastStartTime != null && lastStartTime != startTime;

        if (streamer == null || !data.uuid().equals(streamer.getCurrentUUID()) || timeChanged) {
            if (streamer != null) {
                streamer.stop();
                PLAYING.remove(identity);
            }

            IRadioStreamer newStreamer = LavaplayerLoader.getStreamer();
            if (newStreamer != null) {
                newStreamer.setCurrentUUID(data.uuid());
                PLAYING.put(identity, newStreamer);
                if (identity instanceof net.minecraft.core.BlockPos bPos) {
                    newStreamer.setOnTrackEnd(() -> {
                        net.neoforged.neoforge.network.PacketDistributor
                                .sendToServer(new com.palm1.analogaudio.network.packet.NextTrackC2SPacket(bPos));
                    });
                }
                newStreamer.setOnError(msg -> {
                    Minecraft mc = Minecraft.getInstance();
                    mc.execute(() -> {
                        if (mc.player != null) {
                            mc.player.displayClientMessage(
                                    net.minecraft.network.chat.Component.literal(msg)
                                            .withStyle(net.minecraft.ChatFormatting.RED),
                                    true);
                        }
                    });
                });
                newStreamer.start();

                long currentTick = Minecraft.getInstance().level.getGameTime();
                long offsetMs = Math.max(0, (currentTick - startTime) * 50);

                newStreamer.playTrack(data.url(), offsetMs);
                streamer = newStreamer;
            } else {
                FAILED.add(identity);
                return;
            }
        }
        LAST_START_TIMES.put(identity, startTime);

        if (Minecraft.getInstance().player != null) {
            streamer.setSettings(volume, looping);
            Vec3 pPos = Minecraft.getInstance().player.position();
            Level level = Minecraft.getInstance().level;

            Vec3 globalPos = SableCompat.getGlobalPos(level, pos);
            Vec3 velocity = SableCompat.getVelocity(level, pos);

            streamer.updatePosition(globalPos.x, globalPos.y, globalPos.z, pPos.x, pPos.y, pPos.z, velocity.x,
                    velocity.y, velocity.z);

        }
    }

    public static void stopRadio(Object identity) {
        IRadioStreamer streamer = PLAYING.remove(identity);
        LAST_TICKED.remove(identity);
        if (streamer != null) {
            streamer.stop();
        }
        FAILED.remove(identity);
    }

    public static void stopAll(String trigger, boolean deactivate) {
        if (deactivate)
            active = false;
        AnalogAudio.LOGGER.info("Force-stop triggered by {}. Deactivated: {}. Active streams: {}", trigger, deactivate,
                PLAYING.size());
        for (Object identity : PLAYING.keySet()) {
            AnalogAudio.LOGGER.info("Stopping and clearing identity {}", identity);
            stopRadio(identity);
        }
        PLAYING.clear();
        FAILED.clear();
        LAST_TICKED.clear();
        AnalogAudio.LOGGER.info("Audio clean successful.");
    }

    public static void prepareForSession() {
        active = true;
    }
}
