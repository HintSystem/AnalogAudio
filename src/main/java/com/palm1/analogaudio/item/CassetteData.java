package com.palm1.analogaudio.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record CassetteData(String uuid, String url, String name, int color, float volume, long duration, String authorUuid) {
        public static final Codec<CassetteData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("uuid").forGetter(CassetteData::uuid),
                        Codec.STRING.fieldOf("url").forGetter(CassetteData::url),
                        Codec.STRING.fieldOf("name").forGetter(CassetteData::name),
                        Codec.INT.fieldOf("color").forGetter(CassetteData::color),
                        Codec.FLOAT.optionalFieldOf("volume", -1.0f).forGetter(CassetteData::volume),
                        Codec.LONG.optionalFieldOf("duration", 0L).forGetter(CassetteData::duration),
                        Codec.STRING.optionalFieldOf("authorUuid", "").forGetter(CassetteData::authorUuid)).apply(instance, CassetteData::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, CassetteData> STREAM_CODEC = StreamCodec.of(
                        (buf, data) -> {
                                buf.writeUtf(data.uuid());
                                buf.writeUtf(data.url());
                                buf.writeUtf(data.name());
                                buf.writeInt(data.color());
                                buf.writeFloat(data.volume());
                                buf.writeVarLong(data.duration());
                                buf.writeUtf(data.authorUuid());
                        },
                        buf -> new CassetteData(
                                        buf.readUtf(),
                                        buf.readUtf(),
                                        buf.readUtf(),
                                        buf.readInt(),
                                        buf.readFloat(),
                                        buf.readVarLong(),
                                        buf.readUtf()));
}
