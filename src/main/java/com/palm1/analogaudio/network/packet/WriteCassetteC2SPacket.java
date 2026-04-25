package com.palm1.analogaudio.network.packet;

import com.palm1.analogaudio.AnalogAudio;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record WriteCassetteC2SPacket(String url, String name, int color) implements CustomPacketPayload {
    public static final Type<WriteCassetteC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID, "write_cassette"));

    public static final StreamCodec<FriendlyByteBuf, WriteCassetteC2SPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, WriteCassetteC2SPacket::url,
            ByteBufCodecs.STRING_UTF8, WriteCassetteC2SPacket::name,
            ByteBufCodecs.INT, WriteCassetteC2SPacket::color,
            WriteCassetteC2SPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
