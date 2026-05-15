package com.palm1.analogaudio.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.palm1.analogaudio.AnalogAudio;

public record TokenResponseS2CPacket(String token) implements CustomPacketPayload {
    public static final Type<TokenResponseS2CPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID, "token_response"));

    public static final StreamCodec<FriendlyByteBuf, TokenResponseS2CPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeUtf(packet.token()),
            buf -> new TokenResponseS2CPacket(buf.readUtf()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
