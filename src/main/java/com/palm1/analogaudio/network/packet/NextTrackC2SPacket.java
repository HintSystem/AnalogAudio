package com.palm1.analogaudio.network.packet;

import com.palm1.analogaudio.AnalogAudio;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record NextTrackC2SPacket(BlockPos pos) implements CustomPacketPayload {
    public static final Type<NextTrackC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID, "next_track"));

    public static final StreamCodec<FriendlyByteBuf, NextTrackC2SPacket> STREAM_CODEC = StreamCodec.of(
            (buf, val) -> buf.writeBlockPos(val.pos()),
            buf -> new NextTrackC2SPacket(buf.readBlockPos()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
