package com.palm1.analogaudio.integration;

import java.util.ArrayList;
import java.util.List;

import com.palm1.analogaudio.config.ModConfig;
import com.palm1.analogaudio.item.WalkieTalkieItem;
import com.palm1.analogaudio.network.packet.RadioSignalS2CPacket;
import com.palm1.analogaudio.registry.ModDataComponents;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

// Shared logic for routing voice chat mod signals on server.
public class VoiceRoutingHelper {

    public static ItemStack getHeldWalkieTalkie(ServerPlayer player) {
        if (player.getMainHandItem().getItem() instanceof WalkieTalkieItem) {
            return player.getMainHandItem();
        } else if (player.getOffhandItem().getItem() instanceof WalkieTalkieItem) {
            return player.getOffhandItem();
        }
        return null;
    }

    public static List<ServerPlayer> getRecipientsOnFrequency(MinecraftServer server, ServerPlayer sender,
            int frequency) {
        List<ServerPlayer> recipients = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player == sender)
                continue;

            if (hasWalkieOnFrequency(player, frequency)) {
                recipients.add(player);
            }
        }
        return recipients;
    }

    public static boolean hasWalkieOnFrequency(ServerPlayer player, int frequency) {
        for (ItemStack itemStack : player.getInventory().items) {
            if (itemStack.getItem() instanceof WalkieTalkieItem
                    && itemStack.getOrDefault(ModDataComponents.FREQUENCY.get(), 1) == frequency) {
                return true;
            }
        }
        for (ItemStack itemStack : player.getInventory().offhand) {
            if (itemStack.getItem() instanceof WalkieTalkieItem
                    && itemStack.getOrDefault(ModDataComponents.FREQUENCY.get(), 1) == frequency) {
                return true;
            }
        }
        return false;
    }

    public static void sendRadioSignal(ServerPlayer recipient, UUID sourceId, Vec3 position, int frequency,
            boolean isSpeaker) {
        if (ModConfig.Server.enableWalkieFiltering) {
            PacketDistributor.sendToPlayer(recipient,
                    new RadioSignalS2CPacket(sourceId, position, frequency, isSpeaker));
        }
    }
}
