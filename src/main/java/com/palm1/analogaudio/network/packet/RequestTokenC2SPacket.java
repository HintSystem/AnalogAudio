package com.palm1.analogaudio.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.palm1.analogaudio.AnalogAudio;

public record RequestTokenC2SPacket() implements CustomPacketPayload {
    public static final Type<RequestTokenC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID, "request_token"));

    public static final StreamCodec<FriendlyByteBuf, RequestTokenC2SPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
            },
            buf -> new RequestTokenC2SPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
