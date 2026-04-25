package com.palm1.analogaudio.network.packet;

import com.palm1.analogaudio.AnalogAudio;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record EraseCassetteC2SPacket() implements CustomPacketPayload {
    public static final Type<EraseCassetteC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID, "erase_cassette"));

    public static final StreamCodec<FriendlyByteBuf, EraseCassetteC2SPacket> STREAM_CODEC = StreamCodec
            .unit(new EraseCassetteC2SPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
