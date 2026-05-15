package com.palm1.analogaudio.client.audio;

import com.palm1.analogaudio.AnalogAudio;
import com.palm1.analogaudio.client.audio.api.IRadioStreamer;
import com.palm1.analogaudio.client.audio.lavaplayer.LavaplayerLoader;
import com.palm1.analogaudio.config.ModConfig;
import com.palm1.analogaudio.integration.SableCompat;
import com.palm1.analogaudio.item.CassetteData;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.palm1.analogaudio.network.packet.NextTrackC2SPacket;
import com.palm1.analogaudio.network.packet.RequestTokenC2SPacket;
import com.palm1.analogaudio.util.UrlResolver;
import com.palm1.analogaudio.network.AnalogAudioNetwork;
import net.neoforged.neoforge.network.PacketDistributor;

public class ClientAudioEngine {
    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(1, r -> {
        Thread t = new Thread(r, "AnalogAudio-AudioEngine");
        t.setDaemon(true);
        return t;
    });
    private static final Map<Object, StreamerEntry> PLAYING = new ConcurrentHashMap<>();
    private static final Set<Object> FAILED = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Map<Object, Long> LAST_TICKED = new ConcurrentHashMap<>();
    private static final Map<Object, Long> LAST_START_TIMES = new ConcurrentHashMap<>();
    private static volatile boolean active = true;

    private record StreamerEntry(IRadioStreamer streamer, String uuid, boolean initializing) {
    }

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
        StreamerEntry entry = PLAYING.get(identity);
        return (entry != null && entry.streamer() != null) ? entry.streamer().getAmplitude() : 0f;
    }

    public static void tickRadio(Object identity, Vec3 pos, CassetteData data, long startTime,
            float volume, boolean looping) {
        if (!active || FAILED.contains(identity))
            return;

        LAST_TICKED.put(identity, System.currentTimeMillis());

        StreamerEntry entry = PLAYING.get(identity);
        Long lastStartTime = LAST_START_TIMES.get(identity);

        boolean timeChanged = lastStartTime != null && Math.abs(lastStartTime - startTime) > 2;

        if (entry == null || !data.uuid().equals(entry.uuid()) || timeChanged) {
            if (entry != null && entry.initializing())
                return;

            String url = data.url();
            if (url.startsWith("server:")) {
                if (!ModConfig.Client.enablePlayerSuppliedAudio) {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player != null && mc.player.position().distanceToSqr(pos) < 256) {
                        mc.player.displayClientMessage(
                                net.minecraft.network.chat.Component.translatable("gui.analogaudio.playback_disabled")
                                        .withStyle(net.minecraft.ChatFormatting.RED),
                                true);
                    }
                    PLAYING.put(identity, new StreamerEntry(null, data.uuid(), false));
                    return;
                }
            }

            if (entry != null && entry.streamer() != null) {
                entry.streamer().stop();
            }

            PLAYING.put(identity, new StreamerEntry(null, data.uuid(), true));

            if (data.url().startsWith("server:")) {
                AnalogAudioNetwork.tokenCallback = token -> {
                    StreamerEntry current = PLAYING.get(identity);
                    if (current == null || !current.uuid().equals(data.uuid()))
                        return;

                    String resolvedUrl = UrlResolver.resolve(data.url());
                    resolvedUrl += "?auth=" + token;
                    startPlayback(identity, data, startTime, resolvedUrl, volume, looping);
                };
                PacketDistributor.sendToServer(new RequestTokenC2SPacket());
                return;
            }

            String resolvedUrl = UrlResolver.resolve(data.url());
            startPlayback(identity, data, startTime, resolvedUrl, volume, looping);
            return;
        }

        IRadioStreamer streamer = entry.streamer();
        if (streamer != null && Minecraft.getInstance().player != null) {
            streamer.setSettings(volume, looping);
            Vec3 pPos = Minecraft.getInstance().player.position();
            Level level = Minecraft.getInstance().level;

            Vec3 globalPos = SableCompat.getGlobalPos(level, pos);
            Vec3 velocity = SableCompat.getVelocity(level, pos);

            streamer.updatePosition(globalPos.x, globalPos.y, globalPos.z, pPos.x, pPos.y, pPos.z, velocity.x,
                    velocity.y, velocity.z);
        }
    }

    private static void startPlayback(Object identity, CassetteData data, long startTime, String url, float volume,
            boolean looping) {
        LAST_START_TIMES.put(identity, startTime);

        IRadioStreamer newStreamer = LavaplayerLoader.getStreamer();
        if (newStreamer != null) {
            newStreamer.setCurrentUUID(data.uuid());
            PLAYING.put(identity, new StreamerEntry(newStreamer, data.uuid(), false));
            if (identity instanceof BlockPos bPos) {
                newStreamer.setOnTrackEnd(() -> {
                    PacketDistributor.sendToServer(new NextTrackC2SPacket(bPos));
                });
            }
            newStreamer.setOnError(msg -> {
                Minecraft mc = Minecraft.getInstance();
                mc.execute(() -> {
                    if (mc.player != null) {
                        mc.player.displayClientMessage(
                                Component.literal(msg)
                                        .withStyle(ChatFormatting.RED),
                                true);
                    }
                });
            });
            newStreamer.start();

            long currentTick = Minecraft.getInstance().level.getGameTime();
            long offsetMs = Math.max(0, (currentTick - startTime) * 50);

            newStreamer.playTrack(url, offsetMs);

            newStreamer.setSettings(volume, looping);
            if (Minecraft.getInstance().player != null) {
                Vec3 pPos = Minecraft.getInstance().player.position();
                Level level = Minecraft.getInstance().level;
                Vec3 pos = (identity instanceof BlockPos bp) ? Vec3.atCenterOf(bp) : (Vec3) identity;
                Vec3 globalPos = SableCompat.getGlobalPos(level, pos);
                Vec3 velocity = SableCompat.getVelocity(level, pos);
                newStreamer.updatePosition(globalPos.x, globalPos.y, globalPos.z, pPos.x, pPos.y, pPos.z, velocity.x,
                        velocity.y, velocity.z);
            }
        } else {
            FAILED.add(identity);
        }
    }

    public static void stopRadio(Object identity) {
        StreamerEntry entry = PLAYING.remove(identity);
        LAST_TICKED.remove(identity);
        if (entry != null && entry.streamer() != null) {
            entry.streamer().stop();
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
