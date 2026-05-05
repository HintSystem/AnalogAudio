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
import java.util.stream.Collectors;

public class ModConfig {
    private static final String FOLDER_NAME = "analogaudio";
    private static final String CLIENT_FILE = "analogaudio.client.toml";
    private static final String SERVER_FILE = "analogaudio.server.toml";

    public static class Server {
        public static List<String> whitelistedUrls = new ArrayList<>(
                Arrays.asList("youtube.com", "youtu.be", "soundcloud.com", "bandcamp.com"));
        public static boolean whitelistAsBlacklist = false;
        public static boolean enableWalkieFiltering = true;
        public static boolean allowFileUploads = false;
    }

    public static class Client {
        public static boolean enableCassetteAnimation = true;
        public static boolean enableSpeakerAnimation = true;
        public static boolean renderCassetteText = true;
    }

    public static class Synced {
        public static List<String> whitelistedUrls = new ArrayList<>(Server.whitelistedUrls);
        public static boolean whitelistAsBlacklist = Server.whitelistAsBlacklist;
        public static boolean enableWalkieFiltering = Server.enableWalkieFiltering;
        public static boolean allowFileUploads = Server.allowFileUploads;

        public static void set(List<String> urls, boolean asBlacklist, boolean walkieFiltering, boolean fileUploads) {
            whitelistedUrls = new ArrayList<>(urls);
            whitelistAsBlacklist = asBlacklist;
            enableWalkieFiltering = walkieFiltering;
            allowFileUploads = fileUploads;
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
                    }
                } catch (Exception ex) {
                    System.err.println("Failed to parse client config key '" + key + "': " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load AnalogAudio client config: " + e.getMessage());
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
        if (FMLEnvironment.dist == Dist.CLIENT) {
            saveClient(configDir.resolve(CLIENT_FILE));
        }
    }

    private static void saveServer(Path path) {
        List<String> lines = new ArrayList<>();
        lines.add("# ============================================================");
        lines.add("# Analog Audio - Server Configuration");
        lines.add("# ============================================================");
        lines.add("");
        lines.add("# List of domains allowed for cassette writing. (e.g., youtube.com, catbox.moe)");
        lines.add("whitelistedUrls = ["
                + Server.whitelistedUrls.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")) + "]");
        lines.add("");
        lines.add(
                "# If enabled, the list above will be treated as a blacklist (disallowing those domains) instead of a whitelist.");
        lines.add("whitelistAsBlacklist = " + Server.whitelistAsBlacklist);
        lines.add("");
        lines.add("# Enables radio static effects and frequency filtering for Walkie Talkies and Speakers.");
        lines.add("enableWalkieFiltering = " + Server.enableWalkieFiltering);
        lines.add("");
        lines.add("# Allows players to upload and write local files to cassette tapes.");
        lines.add("allowFileUploads = " + Server.allowFileUploads);

        try {
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Failed to save AnalogAudio server config: " + e.getMessage());
        }
    }

    private static void saveClient(Path path) {
        List<String> lines = new ArrayList<>();
        lines.add("# ============================================================");
        lines.add("# Analog Audio - Client Configuration");
        lines.add("# ============================================================");
        lines.add("");
        lines.add("# Enables the spring-out and spinning animations for the Cassette Deck and Radio.");
        lines.add("enableCassetteAnimation = " + Client.enableCassetteAnimation);
        lines.add("");
        lines.add("# Enables the pulse animation for Speaker blocks when playing audio.");
        lines.add("enableSpeakerAnimation = " + Client.enableSpeakerAnimation);
        lines.add("");
        lines.add("# Renders the custom name of the cassette on the tape model while inserted.");
        lines.add("renderCassetteText = " + Client.renderCassetteText);

        try {
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Failed to save AnalogAudio client config: " + e.getMessage());
        }
    }
}
