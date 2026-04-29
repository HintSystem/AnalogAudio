package com.palm1.analogaudio.client;

import com.palm1.analogaudio.network.packet.SetFrequencyC2SPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.palm1.analogaudio.item.CassetteData;
import com.palm1.analogaudio.network.packet.RadioSignalS2CPacket;
import com.palm1.analogaudio.network.packet.WriteResultS2CPacket;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientHooks {
    public static void openItemFrequencyScreen(int currentFreq, InteractionHand hand) {
        net.minecraft.client.Minecraft.getInstance()
                .setScreen(new com.palm1.analogaudio.client.gui.RotaryTunerScreen(currentFreq, freq -> {
                    PacketDistributor.sendToServer(new SetFrequencyC2SPacket(hand, freq));
                }));
    }

    public static void openBlockFrequencyScreen(int currentFreq, BlockPos pos) {
        net.minecraft.client.Minecraft.getInstance()
                .setScreen(new com.palm1.analogaudio.client.gui.RotaryTunerScreen(currentFreq, freq -> {
                    PacketDistributor.sendToServer(new SetFrequencyC2SPacket(pos, freq));
                }));
    }

    public static net.minecraft.client.gui.screens.Screen getCurrentScreen() {
        return net.minecraft.client.Minecraft.getInstance().screen;
    }

    public static net.minecraft.world.entity.player.Player getClientPlayer() {
        return net.minecraft.client.Minecraft.getInstance().player;
    }

    public static float getLoudness(Object identity) {
        return com.palm1.analogaudio.client.audio.ClientAudioEngine.getLoudness(identity);
    }

    public static void tickRadio(Object identity, Vec3 pos, CassetteData data, long startTime, float volume,
            boolean looping) {
        com.palm1.analogaudio.client.audio.ClientAudioEngine.tickRadio(identity, pos, data, startTime, volume, looping);
    }

    public static void stopRadio(Object identity) {
        com.palm1.analogaudio.client.audio.ClientAudioEngine.stopRadio(identity);
    }

    public static void setStatus(net.minecraft.network.chat.Component component,
            com.palm1.analogaudio.client.gui.CassetteDeckScreen.StatusType statusType, int time) {
        com.palm1.analogaudio.client.gui.CassetteDeckScreen.setStatus(component, statusType, time);
    }

    public static void setStatusMessage(net.minecraft.network.chat.Component component) {
        com.palm1.analogaudio.client.gui.CassetteDeckScreen.statusMessage = component;
    }

    public static void handleWriteResult(final WriteResultS2CPacket data, final IPayloadContext context) {
        com.palm1.analogaudio.client.network.ClientPacketHandlers.handleWriteResult(data, context);
    }

    public static void handleRadioSignal(final RadioSignalS2CPacket data, final IPayloadContext context) {
        com.palm1.analogaudio.client.network.ClientPacketHandlers.handleRadioSignal(data, context);
    }

    public static HitResult getMouseOver() {
        return net.minecraft.client.Minecraft.getInstance().hitResult;
    }
}
