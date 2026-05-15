package com.palm1.analogaudio.util;

import com.palm1.analogaudio.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

public class UrlResolver {
    public static String resolve(String url) {
        if (url == null || url.isEmpty())
            return url;

        if (url.startsWith("server:")) {
            String hash = url.substring(7);
            String ip = "127.0.0.1";

            ServerData serverData = Minecraft.getInstance().getCurrentServer();
            if (serverData != null) {
                ip = serverData.ip;
                if (ip.contains(":")) {
                    ip = ip.substring(0, ip.indexOf(':'));
                }
            }

            int port = ModConfig.Synced.fileServerPort;

            return String.format("http://%s:%d/audio/%s", ip, port, hash);
        }

        return url;
    }
}
