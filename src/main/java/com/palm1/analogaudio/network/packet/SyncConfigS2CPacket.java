package com.palm1.analogaudio.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

import com.palm1.analogaudio.AnalogAudio;

public record SyncConfigS2CPacket(List<String> whitelistedUrls, boolean whitelistAsBlacklist,
                boolean enableWalkieFiltering, boolean allowFileUploads, int globalRadioRange, int globalSpeakerRange,
                boolean fileServerEnabled, int fileServerPort)
                implements CustomPacketPayload {

        public static final Type<SyncConfigS2CPacket> TYPE = new Type<>(
                        ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID, "sync_config"));

        public static final StreamCodec<FriendlyByteBuf, SyncConfigS2CPacket> STREAM_CODEC = StreamCodec.of(
                        (buf, packet) -> {
                                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).encode(buf, packet.whitelistedUrls());
                                buf.writeBoolean(packet.whitelistAsBlacklist());
                                buf.writeBoolean(packet.enableWalkieFiltering());
                                buf.writeBoolean(packet.allowFileUploads());
                                buf.writeVarInt(packet.globalRadioRange());
                                buf.writeVarInt(packet.globalSpeakerRange());
                                buf.writeBoolean(packet.fileServerEnabled());
                                buf.writeVarInt(packet.fileServerPort());
                        },
                        buf -> new SyncConfigS2CPacket(
                                        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).decode(buf),
                                        buf.readBoolean(),
                                        buf.readBoolean(),
                                        buf.readBoolean(),
                                        buf.readVarInt(),
                                        buf.readVarInt(),
                                        buf.readBoolean(),
                                        buf.readVarInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
                return TYPE;
        }
}
