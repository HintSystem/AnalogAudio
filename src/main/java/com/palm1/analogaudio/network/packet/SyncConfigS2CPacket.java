package com.palm1.analogaudio.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

import com.palm1.analogaudio.AnalogAudio;

public record SyncConfigS2CPacket(List<String> whitelistedUrls, boolean whitelistAsBlacklist,
                boolean enableWalkieFiltering, boolean allowFileUploads)
                implements CustomPacketPayload {

        public static final Type<SyncConfigS2CPacket> TYPE = new Type<>(
                        ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID, "sync_config"));

        public static final StreamCodec<FriendlyByteBuf, SyncConfigS2CPacket> STREAM_CODEC = StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), SyncConfigS2CPacket::whitelistedUrls,
                        ByteBufCodecs.BOOL, SyncConfigS2CPacket::whitelistAsBlacklist,
                        ByteBufCodecs.BOOL, SyncConfigS2CPacket::enableWalkieFiltering,
                        ByteBufCodecs.BOOL, SyncConfigS2CPacket::allowFileUploads,
                        SyncConfigS2CPacket::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
                return TYPE;
        }
}
