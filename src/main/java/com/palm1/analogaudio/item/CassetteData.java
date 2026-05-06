package com.palm1.analogaudio.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record CassetteData(String uuid, String url, String name, int color, float volume, long duration) {
        public static final Codec<CassetteData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("uuid").forGetter(CassetteData::uuid),
                        Codec.STRING.fieldOf("url").forGetter(CassetteData::url),
                        Codec.STRING.fieldOf("name").forGetter(CassetteData::name),
                        Codec.INT.fieldOf("color").forGetter(CassetteData::color),
                        Codec.FLOAT.optionalFieldOf("volume", 0.75f).forGetter(CassetteData::volume),
                        Codec.LONG.optionalFieldOf("duration", 0L).forGetter(CassetteData::duration)).apply(instance, CassetteData::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, CassetteData> STREAM_CODEC = StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8, CassetteData::uuid,
                        ByteBufCodecs.STRING_UTF8, CassetteData::url,
                        ByteBufCodecs.STRING_UTF8, CassetteData::name,
                        ByteBufCodecs.INT, CassetteData::color,
                        ByteBufCodecs.FLOAT, CassetteData::volume,
                        ByteBufCodecs.VAR_LONG, CassetteData::duration,
                        CassetteData::new);
}
