package com.palm1.analogaudio.util;

import com.palm1.analogaudio.AnalogAudio;
import com.palm1.analogaudio.config.ModConfig;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FileServerEngine {
    private static final String FOLDER_NAME = "analogaudio/audio";
    private static HttpServer standaloneServer;

    public static boolean isRunning() {
        return standaloneServer != null;
    }

    public static Path getStoragePath() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
            return null;

        Path path = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve(FOLDER_NAME);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                AnalogAudio.LOGGER.error("Failed to create audio storage directory: {}", e.getMessage());
            }
        }
        return path;
    }

    public static void start() {
        if (!ModConfig.FileServer.enabled)
            return;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
            return;

        int configPort = ModConfig.FileServer.port;
        int serverPort = server.getPort();

        if (configPort == serverPort) {
            AnalogAudio.LOGGER.warn(
                    "File server: Configured port ({}) is the same as the Minecraft server port. The file server will not start. Please change 'port' in analogaudio.fileserver.toml to a different value (e.g. 7000).",
                    configPort);
            return;
        }

        try {
            standaloneServer = HttpServer.create(new InetSocketAddress(configPort), 0);
            standaloneServer.createContext("/audio/", new DownloadHandler());
            standaloneServer.createContext("/upload", new UploadHandler());
            standaloneServer.setExecutor(null);
            standaloneServer.start();
            AnalogAudio.LOGGER.info("File server: Started fs on port {}", configPort);
        } catch (IOException e) {
            AnalogAudio.LOGGER.error(
                    "Failed to start standalone file server on port {}: {}. Ensure this port is not in use by another application.",
                    configPort,
                    e.getMessage());
            standaloneServer = null;
        }
    }

    public static void stop() {
        if (standaloneServer != null) {
            standaloneServer.stop(0);
            standaloneServer = null;
        }
    }

    public static class TokenManager {
        private static final Map<String, UUID> TOKENS = new ConcurrentHashMap<>();

        public static String generateToken(UUID playerUuid) {
            String token = UUID.randomUUID().toString();
            TOKENS.put(token, playerUuid);
            return token;
        }

        public static boolean validateToken(String token) {
            boolean valid = TOKENS.containsKey(token);
            if (!valid) {
                AnalogAudio.LOGGER.warn("Token could not validate for: {}", token);
            }
            return valid;
        }
    }

    private static class DownloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String uri = exchange.getRequestURI().toString();
            String hash = uri.substring(7);
            if (hash.contains("?"))
                hash = hash.substring(0, hash.indexOf('?'));

            String auth = getQueryParam(exchange.getRequestURI().getQuery(), "auth");
            if (auth == null)
                auth = exchange.getRequestHeaders().getFirst("X-AnalogAudio-Auth");

            if (auth == null || !TokenManager.validateToken(auth)) {
                exchange.sendResponseHeaders(403, 0);
                exchange.close();
                return;
            }

            File file = getFile(hash);
            if (file == null || !file.exists()) {
                exchange.sendResponseHeaders(404, 0);
                exchange.close();
                return;
            }
            long fileSize = file.length();
            String rangeHeader = exchange.getRequestHeaders().getFirst("Range");
            long start = 0;
            long end = fileSize - 1;

            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                try {
                    String[] ranges = rangeHeader.substring(6).split("-");
                    start = Long.parseLong(ranges[0]);
                    if (ranges.length > 1 && !ranges[1].isEmpty()) {
                        end = Long.parseLong(ranges[1]);
                    }
                } catch (Exception e) {
                    exchange.sendResponseHeaders(416, 0);
                    exchange.close();
                    return;
                }
            }

            long contentLength = end - start + 1;
            exchange.getResponseHeaders().set("Content-Type", Files.probeContentType(file.toPath()));
            exchange.getResponseHeaders().set("Accept-Ranges", "bytes");

            if (rangeHeader != null) {
                exchange.getResponseHeaders().set("Content-Range",
                        String.format("bytes %d-%d/%d", start, end, fileSize));
                exchange.sendResponseHeaders(206, contentLength);
            } else {
                exchange.sendResponseHeaders(200, contentLength);
            }

            try (RandomAccessFile raf = new RandomAccessFile(file, "r");
                    OutputStream os = exchange.getResponseBody()) {
                raf.seek(start);
                byte[] buffer = new byte[8192];
                long bytesToRead = contentLength;
                while (bytesToRead > 0) {
                    int read = raf.read(buffer, 0, (int) Math.min(buffer.length, bytesToRead));
                    if (read == -1)
                        break;
                    os.write(buffer, 0, read);
                    bytesToRead -= read;
                }
            }
            exchange.close();
        }
    }

    private static class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, 0);
                exchange.close();
                return;
            }

            String auth = exchange.getRequestHeaders().getFirst("X-AnalogAudio-Auth");
            if (auth == null || !TokenManager.validateToken(auth)) {
                exchange.sendResponseHeaders(403, 0);
                exchange.close();
                return;
            }

            String filename = exchange.getRequestHeaders().getFirst("X-AnalogAudio-Filename");
            if (filename == null)
                filename = "upload.bin";

            byte[] data = exchange.getRequestBody().readAllBytes();
            try {
                String hash = saveFile(data, filename);
                byte[] response = ("server:" + hash).getBytes();
                exchange.sendResponseHeaders(200, response.length);
                OutputStream os = exchange.getResponseBody();
                os.write(response);
                os.close();
            } catch (Exception e) {
                byte[] response = e.getMessage().getBytes();
                exchange.sendResponseHeaders(400, response.length);
                OutputStream os = exchange.getResponseBody();
                os.write(response);
                os.close();
            }
        }
    }

    private static String getQueryParam(String query, String name) {
        if (query == null)
            return null;
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1 && entry[0].equals(name)) {
                return entry[1];
            }
        }
        return null;
    }

    public static String saveFile(byte[] data, String originalFilename) throws IOException {
        String hash = calculateHash(data);
        String extension = getExtension(originalFilename);

        if (!ModConfig.FileServer.allowedFileFormats.contains(extension.toLowerCase())) {
            throw new IOException("File format not allowed: " + extension);
        }

        if (data.length > ModConfig.FileServer.maxFileSize * 1024 * 1024) {
            throw new IOException("File too large: " + data.length + " bytes (max allowed: "
                    + ModConfig.FileServer.maxFileSize + " MB)");
        }

        Path storage = getStoragePath();
        if (storage == null)
            throw new IOException("Server storage not available");

        Path target = storage.resolve(hash + "." + extension);
        if (!Files.exists(target)) {
            Files.write(target, data);
        }

        return hash + "." + extension;
    }

    public static byte[] readFile(String hash) throws IOException {
        Path storage = getStoragePath();
        if (storage == null)
            return null;

        File[] files = storage.toFile().listFiles((dir, name) -> name.startsWith(hash + "."));
        if (files == null || files.length == 0)
            return null;

        return Files.readAllBytes(files[0].toPath());
    }

    public static File getFile(String hash) {
        Path storage = getStoragePath();
        if (storage == null)
            return null;

        File exact = storage.resolve(hash).toFile();
        if (exact.exists())
            return exact;

        File[] files = storage.toFile().listFiles((dir, name) -> name.startsWith(hash + "."));
        if (files == null || files.length == 0)
            return null;

        return files[0];
    }

    private static String calculateHash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash).substring(0, 12);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1)
            return "bin";
        return filename.substring(lastDot + 1);
    }
}
