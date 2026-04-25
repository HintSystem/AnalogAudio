package com.palm1.analogaudio.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record CassetteData(String uuid, String url, String name, int color) {
        public static final Codec<CassetteData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("uuid").forGetter(CassetteData::uuid),
                        Codec.STRING.fieldOf("url").forGetter(CassetteData::url),
                        Codec.STRING.fieldOf("name").forGetter(CassetteData::name),
                        Codec.INT.fieldOf("color").forGetter(CassetteData::color)).apply(instance, CassetteData::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, CassetteData> STREAM_CODEC = StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8, CassetteData::uuid,
                        ByteBufCodecs.STRING_UTF8, CassetteData::url,
                        ByteBufCodecs.STRING_UTF8, CassetteData::name,
                        ByteBufCodecs.INT, CassetteData::color,
                        CassetteData::new);
}
