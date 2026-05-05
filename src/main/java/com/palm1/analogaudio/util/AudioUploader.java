package com.palm1.analogaudio.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AudioUploader {
    public static final String FILE_HOST_URL = "catbox.moe";
    private static final String CATBOX_API = "https://" + FILE_HOST_URL + "/user/api.php";
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    public static CompletableFuture<String> uploadToCatbox(File file) {
        String boundary = "Boundary-" + UUID.randomUUID().toString();

        return CompletableFuture.supplyAsync(() -> {
            try {
                byte[] fileContent = Files.readAllBytes(file.toPath());
                String fileName = file.getName();
                String contentType = fileName.endsWith(".ogg") ? "audio/ogg" : "audio/mpeg";

                StringBuilder bodyStart = new StringBuilder();
                bodyStart.append("--").append(boundary).append("\r\n");
                bodyStart.append("Content-Disposition: form-data; name=\"reqtype\"\r\n\r\n");
                bodyStart.append("fileupload\r\n");

                bodyStart.append("--").append(boundary).append("\r\n");
                bodyStart.append("Content-Disposition: form-data; name=\"fileToUpload\"; filename=\"").append(fileName)
                        .append("\"\r\n");
                bodyStart.append("Content-Type: ").append(contentType).append("\r\n\r\n");

                byte[] startBytes = bodyStart.toString().getBytes();
                byte[] endBytes = ("\r\n--" + boundary + "--\r\n").getBytes();

                byte[] totalBody = new byte[startBytes.length + fileContent.length + endBytes.length];
                System.arraycopy(startBytes, 0, totalBody, 0, startBytes.length);
                System.arraycopy(fileContent, 0, totalBody, startBytes.length, fileContent.length);
                System.arraycopy(endBytes, 0, totalBody, startBytes.length + fileContent.length, endBytes.length);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(CATBOX_API))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .header("User-Agent", "AnalogAudio Minecraft Mod")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(totalBody))
                        .build();

                HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    String url = response.body().trim();
                    if (url.startsWith("http")) {
                        return url;
                    } else {
                        throw new IOException("API Error: " + url);
                    }
                } else {
                    throw new IOException("Failed to upload: " + response.statusCode() + " " + response.body());
                }
            } catch (Exception e) {
                throw new RuntimeException("Upload failed: " + e.getMessage(), e);
            }
        });
    }
}
