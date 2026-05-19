package com.palm1.analogaudio.client.audio.lavaplayer;

import com.palm1.analogaudio.AnalogAudio;
import com.palm1.analogaudio.client.audio.api.IRadioStreamer;
import com.palm1.analogaudio.config.ModConfig;
import net.minecraft.client.Minecraft;

import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;

public class LavaplayerLoader {
    private static ClassLoader classLoader;
    private static boolean downloadTriggered = false;

    private static final String VERSION = "1.0.1";

    public static String getVersion() {
        return VERSION;
    }

    public static String getGitHubUrl() {
        return "https://github.com/palmmc/analogplayer";
    }

    public static void triggerDownload() {
        triggerDownload(null, null);
    }

    public static void triggerDownload(@Nullable FileDownloader.ProgressCallback progressCallback,
            @Nullable Consumer<Boolean> resultCallback) {
        if (downloadTriggered)
            return;
        downloadTriggered = true;
        ModConfig.Client.lavaplayerDownloadAttempted = true;
        ModConfig.save();

        Thread thread = new Thread(() -> {
            try {
                Path cacheDir = Minecraft.getInstance().gameDirectory.toPath().resolve(".analogaudio/internal");
                if (!Files.exists(cacheDir))
                    Files.createDirectories(cacheDir);

                Path target = cacheDir.resolve("analogplayer-" + VERSION + ".jar");
                FileDownloader.download(
                        ("https://github.com/palmmc/analogplayer/releases/download/" + VERSION + "/analogplayer-"
                                + VERSION + ".jar"),
                        target, "AnalogAudio/1.0", progressCallback);

                deleteOldVersions(cacheDir);

                if (resultCallback != null)
                    resultCallback.accept(true);

            } catch (Exception e) {
                AnalogAudio.LOGGER.error("Failed to download analogplayer", e);
                if (resultCallback != null)
                    resultCallback.accept(false);
                downloadTriggered = false;
            }
        }, "Lavaplayer-Downloader");
        thread.setDaemon(true);
        thread.start();
    }

    public static void disable() {
        ModConfig.Client.lavaplayerWelcomeScreen = false;
        ModConfig.save();
    }

    public static boolean isMissing() {
        return !isInstalled();
    }

    public static boolean isInstalled() {
        Path cacheDir = Minecraft.getInstance().gameDirectory.toPath().resolve(".analogaudio/internal");
        return Files.exists(cacheDir.resolve("analogplayer-" + VERSION + ".jar"));
    }

    public static boolean hasOlderVersion() {
        try {
            Path cacheDir = Minecraft.getInstance().gameDirectory.toPath().resolve(".analogaudio/internal");
            if (Files.isDirectory(cacheDir)) {
                try (var stream = Files.list(cacheDir)) {
                    return stream.anyMatch(path -> {
                        String name = path.getFileName().toString();
                        return name.startsWith("analogplayer") && name.endsWith(".jar")
                                && !name.equals("analogplayer-" + VERSION + ".jar");
                    });
                }
            }
        } catch (Exception e) {
        }
        return false;
    }

    public static IRadioStreamer getUtilityStreamer() {
        return getStreamer();
    }

    public static synchronized IRadioStreamer getStreamer() {
        if (classLoader == null) {
            if (!isInstalled())
                return null;
            load();
        }
        try {
            Class<?> clazz = Class.forName("com.palm1.analogaudio.lavaplayer.LavaRadioStreamer", true, classLoader);
            return (IRadioStreamer) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            AnalogAudio.LOGGER.error("Failed to initialize Lavaplayer streamer", e);
            return null;
        }
    }

    private static void load() {
        try {
            Path cacheDir = Minecraft.getInstance().gameDirectory.toPath().resolve(".analogaudio/internal");
            Path libJar = cacheDir.resolve("analogplayer-" + VERSION + ".jar");

            deleteOldVersions(cacheDir);

            classLoader = new URLClassLoader(
                    new URL[] { libJar.toUri().toURL() },
                    LavaplayerLoader.class.getClassLoader());
        } catch (Exception e) {
            AnalogAudio.LOGGER.error("Failed to load AnalogPlayer library", e);
        }
    }

    private static void deleteOldVersions(Path cacheDir) {
        try {
            if (Files.isDirectory(cacheDir)) {
                try (var stream = Files.list(cacheDir)) {
                    stream.forEach(path -> {
                        try {
                            String name = path.getFileName().toString();
                            if (name.startsWith("analogplayer") && name.endsWith(".jar")
                                    && !name.equals("analogplayer-" + VERSION + ".jar")) {
                                Files.deleteIfExists(path);
                                AnalogAudio.LOGGER.info("Deleted old analogplayer jar: " + name);
                            }
                        } catch (Exception e) {
                        }
                    });
                }
            }
        } catch (Exception e) {
        }
    }
}
