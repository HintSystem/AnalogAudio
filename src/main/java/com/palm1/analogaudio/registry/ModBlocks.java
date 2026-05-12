package com.palm1.analogaudio.registry;

import com.palm1.analogaudio.AnalogAudio;
import com.palm1.analogaudio.block.CassetteDeckBlock;
import com.palm1.analogaudio.block.RadioBlock;
import com.palm1.analogaudio.block.SpeakerBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.fml.ModList;

public class ModBlocks {
        public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AnalogAudio.MODID);

        public static final DeferredBlock<Block> CASSETTE_DECK = BLOCKS.register("cassette_deck",
                        () -> new CassetteDeckBlock(BlockBehaviour.Properties.of().strength(1.5f).sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops()));
        public static final DeferredBlock<Block> RADIO = BLOCKS.register("radio",
                        () -> new RadioBlock(BlockBehaviour.Properties.of().strength(0.8f).sound(SoundType.WOOD)));
        public static final DeferredBlock<Block> SPEAKER = (ModList.get().isLoaded("voicechat")
                        || ModList.get().isLoaded("plasmovoice"))
                                        ? BLOCKS.register("speaker",
                                                        () -> new SpeakerBlock(BlockBehaviour.Properties.of()
                                                                        .strength(0.8f).sound(SoundType.WOOD)))
                                        : null;
}
