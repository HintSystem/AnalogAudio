package com.palm1.analogaudio.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.fml.ModList;

import java.util.function.Supplier;

import com.palm1.analogaudio.AnalogAudio;
import com.palm1.analogaudio.block.entity.CassetteDeckBlockEntity;
import com.palm1.analogaudio.block.entity.RadioBlockEntity;
import com.palm1.analogaudio.block.entity.SpeakerBlockEntity;

public class ModBlockEntities {
        public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
                        .create(Registries.BLOCK_ENTITY_TYPE, AnalogAudio.MODID);

        public static final Supplier<BlockEntityType<CassetteDeckBlockEntity>> CASSETTE_DECK = BLOCK_ENTITIES
                        .register("cassette_deck",
                                         () -> BlockEntityType.Builder.of(CassetteDeckBlockEntity::new, ModBlocks.CASSETTE_DECK.get()).build(null));

        public static final Supplier<BlockEntityType<RadioBlockEntity>> RADIO = BLOCK_ENTITIES.register("radio",
                        () -> BlockEntityType.Builder.of(RadioBlockEntity::new, ModBlocks.RADIO.get()).build(null));

        public static final Supplier<BlockEntityType<SpeakerBlockEntity>> SPEAKER = ModList.get().isLoaded("voicechat") ? BLOCK_ENTITIES
                        .register("speaker",
                                         () -> BlockEntityType.Builder.of(SpeakerBlockEntity::new, ModBlocks.SPEAKER.get()).build(null)) : null;
}
