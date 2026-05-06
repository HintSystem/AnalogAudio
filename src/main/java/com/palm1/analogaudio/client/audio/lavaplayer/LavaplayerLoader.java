package com.palm1.analogaudio.client.audio.lavaplayer;

import com.palm1.analogaudio.AnalogAudio;
import com.palm1.analogaudio.client.audio.api.IRadioStreamer;
import net.minecraft.client.Minecraft;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.net.URL;
import java.net.URLClassLoader;

public class LavaplayerLoader {
    private static ClassLoader classLoader;

    private static IRadioStreamer utilityStreamer;

    public static IRadioStreamer getStreamer() {
        if (classLoader == null) {
            load();
        }
        if (classLoader == null) {
            return null;
        }
        try {
            return (IRadioStreamer) Class
                    .forName("com.palm1.analogaudio.lavaplayer.LavaRadioStreamer", true, classLoader)
                    .getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            AnalogAudio.LOGGER.error("Failed to instantiate LavaRadioStreamer from isolated classloader", e);
            return null;
        }
    }

    public static IRadioStreamer getUtilityStreamer() {
        if (utilityStreamer == null) {
            utilityStreamer = getStreamer();
        }
        return utilityStreamer;
    }

    private static synchronized void load() {
        if (classLoader != null) {
            return;
        }
        try {
            Path cacheDir = Minecraft.getInstance().gameDirectory.toPath().resolve("analogaudio_internal");
            if (!Files.exists(cacheDir)) {
                Files.createDirectories(cacheDir);
            }
            Path libJar = cacheDir.resolve("lavaplayer.jar");

            try (InputStream in = LavaplayerLoader.class
                    .getResourceAsStream("/assets/analogaudio/lavaplayer/lavaplayer.jar")) {
                if (in == null) {
                    throw new RuntimeException("Could not find lavaplayer.jar in mod assets!");
                }
                Files.copy(in, libJar, StandardCopyOption.REPLACE_EXISTING);
            }

            classLoader = new URLClassLoader(new URL[] { libJar.toUri().toURL() },
                    LavaplayerLoader.class.getClassLoader());
            AnalogAudio.LOGGER.info("Successfully initialized isolated Lavaplayer classloader");
        } catch (Exception e) {
            AnalogAudio.LOGGER.error("Failed to load isolated Lavaplayer library", e);
        }
    }
}
