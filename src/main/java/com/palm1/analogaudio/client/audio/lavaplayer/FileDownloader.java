package com.palm1.analogaudio.client.audio.lavaplayer;

import com.palm1.analogaudio.AnalogAudio;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

public final class FileDownloader {

    private static final int CONNECT_TIMEOUT = 30000;
    private static final int READ_TIMEOUT = 30000;
    private static final int MAX_ATTEMPTS = 8;

    @FunctionalInterface
    public interface ProgressCallback {
        void onProgress(int percent);
    }

    public static void download(String urlStr, Path target) throws IOException {
        download(urlStr, target, null, null);
    }

    public static void download(String urlStr, Path target,
            @Nullable String userAgent,
            @Nullable ProgressCallback progressCallback) throws IOException {

        validateUrl(urlStr);

        Path tmp = target.resolveSibling(target.getFileName() + ".part");
        long downloadedBytes = Files.exists(tmp) ? Files.size(tmp) : 0;

        AnalogAudio.LOGGER.info("Downloading {} ...", urlStr);

        int attempt = 0;
        while (true) {
            try {
                downloadAttempt(urlStr, tmp, downloadedBytes, userAgent, progressCallback);
                break;
            } catch (IOException e) {
                attempt++;
                if (attempt >= MAX_ATTEMPTS) {
                    Files.deleteIfExists(tmp);
                    throw new IOException("Failed to download after " + MAX_ATTEMPTS + " attempts: " + urlStr, e);
                }
                AnalogAudio.LOGGER.warn("Download attempt {} failed: {}. Retrying...", attempt, e.getMessage());
                try {
                    Thread.sleep(1000L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Download interrupted", ie);
                }
                downloadedBytes = Files.exists(tmp) ? Files.size(tmp) : 0;
            }
        }

        atomicScooch(tmp, target);
        AnalogAudio.LOGGER.info("Downloaded {} bytes from {}", Files.size(target), urlStr);
    }

    private static void validateUrl(String urlStr) throws IOException {
        try {
            URI uri = new URI(urlStr);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                throw new IOException("Unsupported protocol: " + scheme + ". Only HTTP/HTTPS are allowed.");
            }
        } catch (URISyntaxException e) {
            throw new IOException("Malformed URL: " + urlStr, e);
        }
    }

    private static void atomicScooch(Path source, Path target) throws IOException {
        try {
            Files.move(source, target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (UnsupportedOperationException | IOException e) {
            AnalogAudio.LOGGER.debug("Too chonk, can't scooch. Using standard move: {}", e.getMessage());
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static HttpURLConnection createConnection(String urlStr, long startOffset,
            @Nullable String userAgent) throws IOException {
        URL url = URI.create(urlStr).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);

        if (userAgent != null) {
            conn.setRequestProperty("User-Agent", userAgent);
        }

        if (startOffset > 0) {
            conn.setRequestProperty("Range", "bytes=" + startOffset + "-");
        }
        return conn;
    }

    private static void downloadAttempt(String urlStr, Path tmp, long startOffset,
            @Nullable String userAgent,
            @Nullable ProgressCallback progressCallback) throws IOException {
        HttpURLConnection conn = createConnection(urlStr, startOffset, userAgent);
        int responseCode;
        long actualStartOffset = startOffset;
        boolean rangeSupported = true;

        try {
            responseCode = conn.getResponseCode();

            if (startOffset > 0 && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                AnalogAudio.LOGGER.info("Server does not support range requests (code {}). Restarting download from 0.",
                        responseCode);
                conn.disconnect();
                rangeSupported = false;
                actualStartOffset = 0;
                conn = createConnection(urlStr, 0, userAgent);
                responseCode = conn.getResponseCode();
            }

            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException(String.format("HTTP %d for URL: %s", responseCode, urlStr));
            }

            long contentLength = conn.getContentLengthLong();
            long totalExpected = (rangeSupported && responseCode == HttpURLConnection.HTTP_PARTIAL)
                    ? contentLength + actualStartOffset
                    : contentLength;

            boolean append = rangeSupported && actualStartOffset > 0 && responseCode == HttpURLConnection.HTTP_PARTIAL;
            StandardOpenOption[] writeOptions = append
                    ? new StandardOpenOption[] { StandardOpenOption.CREATE, StandardOpenOption.APPEND }
                    : new StandardOpenOption[] { StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING };

            try (InputStream in = conn.getInputStream();
                    OutputStream out = Files.newOutputStream(tmp, writeOptions)) {

                byte[] buffer = new byte[16384];
                long downloaded = actualStartOffset;
                int lastPercent = -1;
                int bytesRead;

                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    downloaded += bytesRead;

                    if (totalExpected > 0) {
                        int percent = (int) (downloaded * 100 / totalExpected);
                        if (percent != lastPercent) {
                            AnalogAudio.LOGGER.debug("Downloading {} ... {}%", tmp.getFileName(), percent);
                            if (progressCallback != null) {
                                progressCallback.onProgress(percent);
                            }
                            lastPercent = percent;
                        }
                    }
                }
            }

            if (totalExpected > 0 && Files.size(tmp) != totalExpected) {
                throw new IOException(String.format(
                        "Incomplete download: expected %d bytes, got %d bytes", totalExpected, Files.size(tmp)));
            }

        } finally {
            conn.disconnect();
        }
    }
}
