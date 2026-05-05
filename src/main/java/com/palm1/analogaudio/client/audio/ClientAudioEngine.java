package com.palm1.analogaudio.client.audio;

import com.palm1.analogaudio.AnalogAudio;
import com.palm1.analogaudio.client.audio.api.IRadioStreamer;
import com.palm1.analogaudio.client.audio.lavaplayer.LavaplayerLoader;
import com.palm1.analogaudio.client.ClientHooks;
import com.palm1.analogaudio.item.CassetteData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
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

            if (data.url().endsWith(".ogg") || data.url().startsWith("file:/")) {
                RadioStreamer newStreamer = new RadioStreamer();
                newStreamer.setCurrentUUID(data.uuid());
                PLAYING.put(identity, newStreamer);
                if (identity instanceof net.minecraft.core.BlockPos bPos) {
                    newStreamer.setOnTrackEnd(() -> {
                        net.neoforged.neoforge.network.PacketDistributor
                                .sendToServer(new com.palm1.analogaudio.network.packet.NextTrackC2SPacket(bPos));
                    });
                }
                if (!EXECUTOR.isShutdown() && active) {
                    EXECUTOR.submit(() -> ClientAudioEngine.downloadAndPlay(identity, data, startTime));
                }
                streamer = newStreamer;
            } else {
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
        }
        LAST_START_TIMES.put(identity, startTime);

        if (Minecraft.getInstance().player != null) {
            streamer.setSettings(volume, looping);
            Vec3 pPos = Minecraft.getInstance().player.position();
            Level level = Minecraft.getInstance().level;

            Vec3 globalPos = com.palm1.analogaudio.integration.SableCompat.getGlobalPos(level, pos);
            Vec3 velocity = com.palm1.analogaudio.integration.SableCompat.getVelocity(level, pos);

            streamer.updatePosition(globalPos.x, globalPos.y, globalPos.z, pPos.x, pPos.y, pPos.z, velocity.x,
                    velocity.y, velocity.z);

            if (streamer instanceof RadioStreamer) {
                RadioStreamer oggStreamer = (RadioStreamer) streamer;
                Path cachedFile = oggStreamer.getCurrentFile();
                if (cachedFile != null) {
                    oggStreamer.play(cachedFile, startTime);
                }
            }
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

    private static void downloadAndPlay(Object identity, CassetteData data, long startTime) {
        if (!active)
            return;
        AnalogAudio.LOGGER.info("Starting audio process for URL: {}", data.url());
        try {
            Path cacheDir = Minecraft.getInstance().gameDirectory.toPath().resolve("analogaudio_cache");
            if (!Files.exists(cacheDir)) {
                Files.createDirectories(cacheDir);
                AnalogAudio.LOGGER.info("Created cache directory: {}", cacheDir);
            }

            Path file = cacheDir.resolve(data.uuid() + ".ogg");
            if (!Files.exists(file)) {
                AnalogAudio.LOGGER.info("Downloading/Copying file to: {}", file);

                if (data.url().startsWith("file:/")) {
                    Path sourceFile = Path.of(new URL(data.url()).toURI());
                    Files.copy(sourceFile, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } else {
                    HttpURLConnection connection = (HttpURLConnection) URI.create(data.url()).toURL()
                            .openConnection();
                    connection.setRequestProperty("User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36");

                    int responseCode = connection.getResponseCode();
                    AnalogAudio.LOGGER.info("HTTP Response Code: {}", responseCode);

                    if (responseCode >= 200 && responseCode < 300) {
                        long contentLength = connection.getContentLengthLong();
                        try (InputStream in = connection.getInputStream();
                                OutputStream out = Files.newOutputStream(file)) {
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            long totalRead = 0;
                            while ((bytesRead = in.read(buffer)) != -1) {
                                out.write(buffer, 0, bytesRead);
                                totalRead += bytesRead;
                                if (contentLength > 0) {
                                    float progress = (float) totalRead / contentLength;
                                    displayActionBarProgress(progress);
                                }
                            }
                        }
                    } else {
                        ClientHooks.setStatus(
                                Component.translatable("gui.analogaudio.cassette_deck.status.error_code", responseCode),
                                com.palm1.analogaudio.client.gui.CassetteDeckScreen.StatusType.ERROR, 100);
                        throw new IOException("Server returned HTTP " + responseCode);
                    }
                }
                AnalogAudio.LOGGER.info("Process complete.");
                ClientHooks.setStatusMessage(Component
                        .translatable("gui.analogaudio.cassette_deck.status.write_success"));
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.displayClientMessage(Component.empty(),
                            true);
                }
            } else {
                AnalogAudio.LOGGER.info("File already in cache: {}", file);
                ClientHooks.setStatus(
                        Component.translatable("gui.analogaudio.cassette_deck.status.write_success"),
                        com.palm1.analogaudio.client.gui.CassetteDeckScreen.StatusType.SUCCESS, 100);
            }

            IRadioStreamer streamer = PLAYING.get(identity);
            if (streamer instanceof RadioStreamer) {
                ((RadioStreamer) streamer).play(file, startTime);
            } else {
                AnalogAudio.LOGGER.warn("No streamer found for identity: {}", identity);
            }
        } catch (Exception e) {
            AnalogAudio.LOGGER.error("Failed to download or play audio from {}: {}", data.url(),
                    e.getMessage());
            ClientHooks.setStatus(Component.translatable("gui.analogaudio.cassette_deck.status.download_fail"),
                    com.palm1.analogaudio.client.gui.CassetteDeckScreen.StatusType.ERROR, 100);
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                        Component.literal("§c")
                                .append(Component.translatable("gui.analogaudio.cassette_deck.status.download_fail")),
                        true);
            }
            e.printStackTrace();
            PLAYING.remove(identity);
            FAILED.add(identity);
        }
    }

    private static void displayActionBarProgress(float progress) {
        if (Minecraft.getInstance().player == null)
            return;

        int totalBars = 36;
        int completedBars = (int) (progress * totalBars);

        Component downloading = Component.translatable("gui.analogaudio.cassette_deck.status.downloading");

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < completedBars; i++)
            bar.append("|");
        String completed = bar.toString();

        bar = new StringBuilder();
        for (int i = completedBars; i < totalBars; i++)
            bar.append("|");
        String remaining = bar.toString();

        Minecraft.getInstance().player.displayClientMessage(
                Component.literal("§f").append(downloading).append(" §r[§a")
                        .append(completed).append("§7")
                        .append(remaining).append("§r]"),
                true);
    }
}
