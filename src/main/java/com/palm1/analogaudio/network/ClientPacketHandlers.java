package com.palm1.analogaudio.network;

import com.palm1.analogaudio.client.ClientSignalTracker;
import com.palm1.analogaudio.client.gui.CassetteDeckScreen;
import com.palm1.analogaudio.network.packet.RadioSignalS2CPacket;
import com.palm1.analogaudio.network.packet.WriteResultS2CPacket;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPacketHandlers {
    public static void handleWriteResult(final WriteResultS2CPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof CassetteDeckScreen screen) {
                screen.handleResult(data.statusType());
            }
        });
    }

    public static void handleRadioSignal(final RadioSignalS2CPacket data, final IPayloadContext context) {
        ClientSignalTracker.updateSignal(data.senderUuid(), data.position(), data.frequency(), data.isSpeaker());
    }
}
