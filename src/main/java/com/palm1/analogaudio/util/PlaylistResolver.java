package com.palm1.analogaudio.util;

import com.palm1.analogaudio.AnalogAudio;
import com.palm1.analogaudio.client.audio.api.IPlaylistResolver;
import net.neoforged.fml.loading.FMLPaths;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PlaylistResolver {
    private static ClassLoader classLoader;
    private static IPlaylistResolver instance;

    public static synchronized ClassLoader getOrLoadClassLoader() throws Exception {
        if (classLoader == null) {
            Path cacheDir = FMLPaths.GAMEDIR.get().resolve(".analogaudio/internal");
            Path libJar = cacheDir.resolve("analogplayer-" + AnalogAudio.ANALOGPLAYER_VERSION + ".jar");
            if (!Files.exists(libJar)) {
                throw new java.io.FileNotFoundException(
                        "Analogplayer is not installed: " + libJar.toAbsolutePath());
            }
            classLoader = new URLClassLoader(
                    new URL[] { libJar.toUri().toURL() },
                    PlaylistResolver.class.getClassLoader());
        }
        return classLoader;
    }

    public static synchronized IPlaylistResolver getResolver() {
        if (instance == null) {
            try {
                ClassLoader loader = getOrLoadClassLoader();
                Class<?> cls = Class.forName("com.palm1.analogaudio.lavaplayer.LavaPlaylistResolver", true, loader);
                instance = (IPlaylistResolver) cls.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Could not initialize PlaylistResolver", e);
            }
        }
        return instance;
    }

    public static CompletableFuture<List<IPlaylistResolver.ResolvedTrack>> resolvePlaylist(String playlistUrl) {
        try {
            return getResolver().resolve(playlistUrl);
        } catch (Exception e) {
            CompletableFuture<List<IPlaylistResolver.ResolvedTrack>> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
}
