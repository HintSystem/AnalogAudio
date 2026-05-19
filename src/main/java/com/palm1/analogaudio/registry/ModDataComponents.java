package com.palm1.analogaudio.registry;

import com.mojang.serialization.Codec;
import com.mojang.datafixers.util.Either;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import com.palm1.analogaudio.AnalogAudio;
import com.palm1.analogaudio.item.CassetteData;

public class ModDataComponents {
        public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister
                        .create(Registries.DATA_COMPONENT_TYPE, AnalogAudio.MODID);

        public static final Supplier<DataComponentType<CassetteData>> CASSETTE_DATA = DATA_COMPONENTS
                        .register("cassette_data", () -> DataComponentType.<CassetteData>builder()
                                        .persistent(CassetteData.CODEC)
                                        .networkSynchronized(CassetteData.STREAM_CODEC)
                                        .build());

        public static final Supplier<DataComponentType<Integer>> FREQUENCY = DATA_COMPONENTS.register("frequency",
                        () -> DataComponentType.<Integer>builder()
                                        .persistent(ExtraCodecs.intRange(1, 255))
                                        .networkSynchronized(ByteBufCodecs.VAR_INT)
                                        .build());

        private static final Codec<ItemContainerContents> MIGRATION_CODEC = Codec.either(
                        ItemContainerContents.CODEC,
                        ItemStack.OPTIONAL_CODEC.listOf()).xmap(
                                        either -> either.map(
                                                        contents -> contents,
                                                        list -> ItemContainerContents.fromItems(list)),
                                        Either::left);

        public static final Supplier<DataComponentType<ItemContainerContents>> BAG_CONTENTS = DATA_COMPONENTS
                        .register("bag_contents",
                                        () -> DataComponentType.<ItemContainerContents>builder()
                                                        .persistent(MIGRATION_CODEC)
                                                        .networkSynchronized(ItemContainerContents.STREAM_CODEC)
                                                        .build());
}
