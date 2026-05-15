package com.palm1.analogaudio.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import com.palm1.analogaudio.AnalogAudio;
import com.palm1.analogaudio.config.ModConfig;
import com.palm1.analogaudio.network.packet.RequestTokenC2SPacket;
import com.palm1.analogaudio.network.AnalogAudioNetwork;
import net.neoforged.neoforge.network.PacketDistributor;

public class AudioUploader {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    public static CompletableFuture<String> upload(File file) {
        if (ModConfig.Synced.fileServerEnabled) {
            return uploadToLocalServer(file);
        } else if (ModConfig.Synced.allowFileUploads) {
            return CompletableFuture.completedFuture("client:" + file.getName());
        } else {
            return CompletableFuture.failedFuture(new RuntimeException("File uploads are disabled"));
        }
    }

    private static CompletableFuture<String> uploadToLocalServer(File file) {
        CompletableFuture<String> future = new CompletableFuture<>();

        AnalogAudioNetwork.tokenCallback = token -> {
            CompletableFuture.runAsync(() -> {
                try {
                    String ip = "127.0.0.1";
                    net.minecraft.client.multiplayer.ServerData serverData = net.minecraft.client.Minecraft
                            .getInstance()
                            .getCurrentServer();
                    if (serverData != null) {
                        ip = serverData.ip;
                        if (ip.contains(":")) {
                            ip = ip.substring(0, ip.indexOf(':'));
                        }
                    }

                    int port = ModConfig.Synced.fileServerPort;
                    String uploadUrl = String.format("http://%s:%d/upload", ip, port);

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(uploadUrl))
                            .header("X-AnalogAudio-Auth", token)
                            .header("X-AnalogAudio-Filename", file.getName())
                            .POST(HttpRequest.BodyPublishers.ofFile(file.toPath()))
                            .build();

                    HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        future.complete(response.body().trim());
                    } else {
                        AnalogAudio.LOGGER.error("File server upload failed with status {}: {}", response.statusCode(),
                                response.body());
                        future.completeExceptionally(
                                new IOException("Server error: " + response.statusCode() + " " + response.body()));
                    }
                } catch (Exception e) {
                    AnalogAudio.LOGGER.error("File server upload failed with exception", e);
                    future.completeExceptionally(new RuntimeException("Upload failed: " + e.getMessage(), e));
                }
            });
        };

        PacketDistributor.sendToServer(new RequestTokenC2SPacket());
        return future;
    }
}
