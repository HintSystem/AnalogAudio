package com.palm1.analogaudio.network.packet;

import com.palm1.analogaudio.AnalogAudio;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record WriteResultS2CPacket(int statusType) implements CustomPacketPayload {
    public static final Type<WriteResultS2CPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID, "write_result"));

    public static final StreamCodec<FriendlyByteBuf, WriteResultS2CPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, WriteResultS2CPacket::statusType,
            WriteResultS2CPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
