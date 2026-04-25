package com.palm1.analogaudio.network.packet;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

import com.palm1.analogaudio.AnalogAudio;

public record RadioSignalS2CPacket(UUID senderUuid, Vec3 position, int frequency, boolean isSpeaker)
                implements CustomPacketPayload {

        public static final Type<RadioSignalS2CPacket> TYPE = new Type<>(
                        ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID, "radio_signal"));

        public static final StreamCodec<FriendlyByteBuf, Vec3> VEC3_STREAM_CODEC = StreamCodec.composite(
                        ByteBufCodecs.DOUBLE, Vec3::x,
                        ByteBufCodecs.DOUBLE, Vec3::y,
                        ByteBufCodecs.DOUBLE, Vec3::z,
                        Vec3::new);

        public static final StreamCodec<FriendlyByteBuf, RadioSignalS2CPacket> STREAM_CODEC = StreamCodec.composite(
                        UUIDUtil.STREAM_CODEC, RadioSignalS2CPacket::senderUuid,
                        VEC3_STREAM_CODEC, RadioSignalS2CPacket::position,
                        ByteBufCodecs.VAR_INT, RadioSignalS2CPacket::frequency,
                        ByteBufCodecs.BOOL, RadioSignalS2CPacket::isSpeaker,
                        RadioSignalS2CPacket::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
                return TYPE;
        }
}
