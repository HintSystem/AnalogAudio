package com.palm1.analogaudio.util;

import com.palm1.analogaudio.AnalogAudio;
import com.palm1.analogaudio.config.ModConfig;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

@EventBusSubscriber(modid = AnalogAudio.MODID)
public class ModPermissions {
    public static final PermissionNode<Boolean> UPLOAD_FILES = new PermissionNode<>(
            AnalogAudio.MODID,
            "uploadfiles",
            PermissionTypes.BOOLEAN,
            (player, uuid, context) -> false);

    public static final PermissionNode<Boolean> CASSETTE_WRITE = new PermissionNode<>(
            AnalogAudio.MODID,
            "command.cassette.write",
            PermissionTypes.BOOLEAN,
            (player, uuid, context) -> false);

    public static final PermissionNode<Boolean> CASSETTE_PLAYLIST = new PermissionNode<>(
            AnalogAudio.MODID,
            "command.cassette.playlist",
            PermissionTypes.BOOLEAN,
            (player, uuid, context) -> false);

    @SubscribeEvent
    public static void onPermissionGather(PermissionGatherEvent.Nodes event) {
        event.addNodes(UPLOAD_FILES, CASSETTE_WRITE, CASSETTE_PLAYLIST);
    }

    public static boolean canUploadFiles(ServerPlayer player) {
        if (ModConfig.Server.allowFileUploads) {
            return true;
        }
        return player.hasPermissions(4) || PermissionAPI.getPermission(player, UPLOAD_FILES);
    }
}
