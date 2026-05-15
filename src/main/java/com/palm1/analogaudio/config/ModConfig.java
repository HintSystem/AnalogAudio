package com.palm1.analogaudio.config;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.palm1.analogaudio.AnalogAudio;

import java.io.InputStreamReader;
import java.io.InputStream;

public class ModConfig {
    private static final String FOLDER_NAME = "analogaudio";
    private static final String CLIENT_FILE = "analogaudio.client.toml";
    private static final String SERVER_FILE = "analogaudio.server.toml";
    private static final String FILE_SERVER_FILE = "analogaudio.fileserver.toml";
    private static Map<String, String> translations = null;

    private static String t(String key) {
        if (translations == null) {
            translations = new HashMap<>();
            try (InputStream is = ModConfig.class.getResourceAsStream("/assets/analogaudio/lang/en_us.json")) {
                if (is != null) {
                    translations = new Gson().fromJson(new InputStreamReader(is, StandardCharsets.UTF_8),
                            new TypeToken<Map<String, String>>() {
                            }.getType());
                }
            } catch (Exception e) {
                AnalogAudio.LOGGER.error("Failed to load config descriptions: {}", e.getMessage());
            }
        }
        return translations.getOrDefault(key, key);
    }

    public static class Server {
        public static List<String> whitelistedUrls = new ArrayList<>(
                Arrays.asList("youtube.com", "youtu.be", "soundcloud.com", "bandcamp.com"));
        public static boolean whitelistAsBlacklist = false;
        public static boolean enableWalkieFiltering = true;
        public static boolean allowFileUploads = false;
        public static int globalRadioRange = 64;
        public static int globalSpeakerRange = 64;
    }

    public static class FileServer {
        public static boolean enabled = false;
        public static int port = 7000;
        public static long maxFileSize = 50;
        public static List<String> allowedFileFormats = new ArrayList<>(
                Arrays.asList("ogg", "mp3", "wav", "flac", "aac", "m4a"));
    }

    public static class Client {
        public static boolean enableCassetteAnimation = true;
        public static boolean enableSpeakerAnimation = true;
        public static boolean renderCassetteText = true;
        public static boolean enableSpatialAudio = true;
        public static float spatialityThreshold = 0.3f;
        public static float globalRadioVolume = 1.0f;
        public static boolean speakerEcho = false;
        public static boolean enablePlayerSuppliedAudio = false;
        public static boolean cassetteTapeDisclaimers = true;
    }

    public static class Synced {
        public static List<String> whitelistedUrls = new ArrayList<>(Server.whitelistedUrls);
        public static boolean whitelistAsBlacklist = Server.whitelistAsBlacklist;
        public static boolean enableWalkieFiltering = Server.enableWalkieFiltering;
        public static boolean allowFileUploads = Server.allowFileUploads;
        public static int globalRadioRange = Server.globalRadioRange;
        public static int globalSpeakerRange = Server.globalSpeakerRange;

        public static boolean fileServerEnabled = FileServer.enabled;
        public static int fileServerPort = FileServer.port;

        public static void set(List<String> urls, boolean asBlacklist, boolean walkieFiltering, boolean fileUploads,
                int radioRange, int speakerRange, boolean fsEnabled, int fsPort) {
            whitelistedUrls = new ArrayList<>(urls);
            whitelistAsBlacklist = asBlacklist;
            enableWalkieFiltering = walkieFiltering;
            allowFileUploads = fileUploads;
            globalRadioRange = radioRange;
            globalSpeakerRange = speakerRange;
            fileServerEnabled = fsEnabled;
            fileServerPort = fsPort;
        }
    }

    public static void load() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(FOLDER_NAME);
        if (!Files.exists(configDir)) {
            try {
                Files.createDirectories(configDir);
            } catch (IOException e) {
                System.err.println("Failed to create AnalogAudio config directory: " + e.getMessage());
            }
        }

        loadServer(configDir.resolve(SERVER_FILE));
        loadFileServer(configDir.resolve(FILE_SERVER_FILE));
        if (FMLEnvironment.dist == Dist.CLIENT) {
            loadClient(configDir.resolve(CLIENT_FILE));
        }
    }

    private static void loadServer(Path path) {
        if (!Files.exists(path)) {
            saveServer(path);
            return;
        }

        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;

                String[] parts = line.split("=", 2);
                if (parts.length < 2)
                    continue;

                String key = parts[0].trim();
                String value = parts[1].trim();

                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }

                try {
                    switch (key) {
                        case "whitelistedUrls" -> {
                            if (value.startsWith("[") && value.endsWith("]")) {
                                String content = value.substring(1, value.length() - 1);
                                Server.whitelistedUrls = Arrays.stream(content.split(","))
                                        .map(String::trim)
                                        .filter(s -> !s.isEmpty())
                                        .map(s -> s.startsWith("\"") && s.endsWith("\"")
                                                ? s.substring(1, s.length() - 1)
                                                : s)
                                        .collect(Collectors.toList());
                            }
                        }
                        case "whitelistAsBlacklist" -> Server.whitelistAsBlacklist = Boolean.parseBoolean(value);
                        case "enableWalkieFiltering" -> Server.enableWalkieFiltering = Boolean.parseBoolean(value);
                        case "allowFileUploads" -> Server.allowFileUploads = Boolean.parseBoolean(value);
                        case "globalRadioRange" -> Server.globalRadioRange = Integer.parseInt(value);
                        case "globalSpeakerRange" -> Server.globalSpeakerRange = Integer.parseInt(value);
                    }
                } catch (Exception ex) {
                    System.err.println("Failed to parse server config key '" + key + "': " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load AnalogAudio server config: " + e.getMessage());
        }
    }

    private static void loadClient(Path path) {
        if (!Files.exists(path)) {
            saveClient(path);
            return;
        }

        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;

                String[] parts = line.split("=", 2);
                if (parts.length < 2)
                    continue;

                String key = parts[0].trim();
                String value = parts[1].trim();

                try {
                    switch (key) {
                        case "enableCassetteAnimation" -> Client.enableCassetteAnimation = Boolean.parseBoolean(value);
                        case "enableSpeakerAnimation" -> Client.enableSpeakerAnimation = Boolean.parseBoolean(value);
                        case "renderCassetteText" -> Client.renderCassetteText = Boolean.parseBoolean(value);
                        case "enableSpatialAudio" -> Client.enableSpatialAudio = Boolean.parseBoolean(value);
                        case "spatialityThreshold" -> Client.spatialityThreshold = Float.parseFloat(value);
                        case "globalRadioVolume" -> Client.globalRadioVolume = Float.parseFloat(value);
                        case "speakerEcho" -> Client.speakerEcho = Boolean.parseBoolean(value);
                        case "enablePlayerSuppliedAudio" -> Client.enablePlayerSuppliedAudio = Boolean.parseBoolean(value);
                        case "cassetteTapeDisclaimers" -> Client.cassetteTapeDisclaimers = Boolean.parseBoolean(value);
                    }
                } catch (Exception ex) {
                    System.err.println("Failed to parse client config key '" + key + "': " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load AnalogAudio client config: " + e.getMessage());
        }
    }

    private static void loadFileServer(Path path) {
        if (!Files.exists(path)) {
            saveFileServer(path);
            return;
        }

        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;

                String[] parts = line.split("=", 2);
                if (parts.length < 2)
                    continue;

                String key = parts[0].trim();
                String value = parts[1].trim();

                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }

                try {
                    switch (key) {
                        case "enabled" -> FileServer.enabled = Boolean.parseBoolean(value);
                        case "port" -> FileServer.port = Integer.parseInt(value);
                        case "maxFileSize" -> FileServer.maxFileSize = Long.parseLong(value);
                        case "allowedFileFormats" -> {
                            if (value.startsWith("[") && value.endsWith("]")) {
                                String content = value.substring(1, value.length() - 1);
                                FileServer.allowedFileFormats = Arrays.stream(content.split(","))
                                        .map(String::trim)
                                        .filter(s -> !s.isEmpty())
                                        .map(s -> s.startsWith("\"") && s.endsWith("\"")
                                                ? s.substring(1, s.length() - 1)
                                                : s)
                                        .collect(Collectors.toList());
                            }
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("Failed to parse file server config key '" + key + "': " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load AnalogAudio file server config: " + e.getMessage());
        }
    }

    public static void save() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(FOLDER_NAME);
        if (!Files.exists(configDir)) {
            try {
                Files.createDirectories(configDir);
            } catch (IOException e) {
                System.err.println("Failed to create AnalogAudio config directory: " + e.getMessage());
            }
        }
        saveServer(configDir.resolve(SERVER_FILE));
        saveFileServer(configDir.resolve(FILE_SERVER_FILE));
        if (FMLEnvironment.dist == Dist.CLIENT) {
            saveClient(configDir.resolve(CLIENT_FILE));
        }
    }

    private static void saveServer(Path path) {
        List<String> lines = new ArrayList<>();
        lines.add("# ============================================================");
        lines.add("# " + t("config.analogaudio.category.server"));
        lines.add("# ============================================================");
        lines.add("");
        lines.add("# " + t("config.analogaudio.whitelistedUrls.description"));
        lines.add("whitelistedUrls = ["
                + Server.whitelistedUrls.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")) + "]");
        lines.add("");
        lines.add("# " + t("config.analogaudio.whitelistAsBlacklist.description"));
        lines.add("whitelistAsBlacklist = " + Server.whitelistAsBlacklist);
        lines.add("");
        lines.add("# " + t("config.analogaudio.enableWalkieFiltering.description"));
        lines.add("enableWalkieFiltering = " + Server.enableWalkieFiltering);
        lines.add("");
        lines.add("# " + t("config.analogaudio.allowFileUploads.description"));
        lines.add("allowFileUploads = " + Server.allowFileUploads);
        lines.add("");
        lines.add("# " + t("config.analogaudio.globalRadioRange.description"));
        lines.add("globalRadioRange = " + Server.globalRadioRange);
        lines.add("");
        lines.add("# " + t("config.analogaudio.globalSpeakerRange.description"));
        lines.add("globalSpeakerRange = " + Server.globalSpeakerRange);

        try {
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Failed to save AnalogAudio server config: " + e.getMessage());
        }
    }

    private static void saveClient(Path path) {
        List<String> lines = new ArrayList<>();
        lines.add("# ============================================================");
        lines.add("# " + t("config.analogaudio.category.client"));
        lines.add("# ============================================================");
        lines.add("");
        lines.add("# " + t("config.analogaudio.enableCassetteAnimation.description"));
        lines.add("enableCassetteAnimation = " + Client.enableCassetteAnimation);
        lines.add("");
        lines.add("# " + t("config.analogaudio.enableSpeakerAnimation.description"));
        lines.add("enableSpeakerAnimation = " + Client.enableSpeakerAnimation);
        lines.add("");
        lines.add("# " + t("config.analogaudio.renderCassetteText.description"));
        lines.add("renderCassetteText = " + Client.renderCassetteText);
        lines.add("");
        lines.add("# " + t("config.analogaudio.enableSpatialAudio.description"));
        lines.add("enableSpatialAudio = " + Client.enableSpatialAudio);
        lines.add("");
        lines.add("# " + t("config.analogaudio.spatialityThreshold.description"));
        lines.add("spatialityThreshold = " + Client.spatialityThreshold);
        lines.add("");
        lines.add("# " + t("config.analogaudio.globalRadioVolume.description"));
        lines.add("globalRadioVolume = " + Client.globalRadioVolume);
        lines.add("");
        lines.add("# " + t("config.analogaudio.speakerEcho.description"));
        lines.add("speakerEcho = " + Client.speakerEcho);
        lines.add("");
        lines.add("# " + t("config.analogaudio.enablePlayerSuppliedAudio.description"));
        lines.add("enablePlayerSuppliedAudio = " + Client.enablePlayerSuppliedAudio);
        lines.add("");
        lines.add("# " + t("config.analogaudio.cassetteTapeDisclaimers.description"));
        lines.add("cassetteTapeDisclaimers = " + Client.cassetteTapeDisclaimers);

        try {
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Failed to save AnalogAudio client config: " + e.getMessage());
        }
    }

    private static void saveFileServer(Path path) {
        List<String> lines = new ArrayList<>();
        lines.add("# ============================================================");
        lines.add("# " + t("config.analogaudio.category.fileserver"));
        lines.add("# ============================================================");
        lines.add("");
        lines.add("# " + t("config.analogaudio.fileserver.enabled.description"));
        lines.add("# " + t("config.analogaudio.fileserver.enabled.disclaimer"));
        lines.add("enabled = " + FileServer.enabled);
        lines.add("");
        lines.add("# " + t("config.analogaudio.fileserver.port.description"));
        lines.add("port = " + FileServer.port);
        lines.add("");
        lines.add("# " + t("config.analogaudio.fileserver.maxFileSize.description"));
        lines.add("maxFileSize = " + FileServer.maxFileSize);
        lines.add("");
        lines.add("# " + t("config.analogaudio.fileserver.allowedFileFormats.description"));
        lines.add("allowedFileFormats = ["
                + FileServer.allowedFileFormats.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(", "))
                + "]");

        try {
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Failed to save AnalogAudio file server config: " + e.getMessage());
        }
    }
}
