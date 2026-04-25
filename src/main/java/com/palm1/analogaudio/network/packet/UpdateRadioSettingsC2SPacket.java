package com.palm1.analogaudio.network.packet;

import com.palm1.analogaudio.AnalogAudio;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UpdateRadioSettingsC2SPacket(BlockPos pos, float volume, boolean looping, boolean playing)
        implements CustomPacketPayload {
    public static final Type<UpdateRadioSettingsC2SPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID, "update_radio_settings"));

    public static final StreamCodec<FriendlyByteBuf, UpdateRadioSettingsC2SPacket> STREAM_CODEC = StreamCodec.of(
            (buf, val) -> {
                buf.writeBlockPos(val.pos());
                buf.writeFloat(val.volume());
                buf.writeBoolean(val.looping());
                buf.writeBoolean(val.playing());
            },
            buf -> new UpdateRadioSettingsC2SPacket(buf.readBlockPos(), buf.readFloat(), buf.readBoolean(),
                    buf.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
