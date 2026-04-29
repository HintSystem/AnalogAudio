package com.palm1.analogaudio.registry;

import com.palm1.analogaudio.AnalogAudio;
import com.palm1.analogaudio.item.CassetteTapeItem;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.fml.ModList;

public class ModItems {
        public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AnalogAudio.MODID);

        public static final DeferredItem<Item> CASSETTE_TAPE = ITEMS.register("cassette_tape",
                        () -> new CassetteTapeItem(new Item.Properties().stacksTo(1)));
        public static final DeferredItem<Item> CASSETTE_BAG = ITEMS.register("cassette_bag",
                        () -> new com.palm1.analogaudio.item.CassetteBagItem(new Item.Properties().stacksTo(1)));
        public static final DeferredItem<Item> WALKIE_TALKIE = ModList.get().isLoaded("voicechat")
                        ? ITEMS.register("walkie_talkie",
                                        () -> new com.palm1.analogaudio.item.WalkieTalkieItem(
                                                        new Item.Properties().stacksTo(1)))
                        : null;
        public static final DeferredItem<BlockItem> CASSETTE_DECK = ITEMS.registerSimpleBlockItem("cassette_deck",
                        ModBlocks.CASSETTE_DECK);
        public static final DeferredItem<BlockItem> RADIO = ITEMS.registerSimpleBlockItem("radio", ModBlocks.RADIO);
        public static final DeferredItem<BlockItem> SPEAKER = ModList.get().isLoaded("voicechat")
                        ? ITEMS.registerSimpleBlockItem("speaker",
                                        ModBlocks.SPEAKER)
                        : null;
}
