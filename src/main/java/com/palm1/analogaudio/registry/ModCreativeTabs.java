package com.palm1.analogaudio.registry;

import com.palm1.analogaudio.AnalogAudio;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModCreativeTabs {
        public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister
                        .create(Registries.CREATIVE_MODE_TAB, AnalogAudio.MODID);

        public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ANALOG_AUDIO_TAB = CREATIVE_TABS
                        .register("analog_audio_tab", () -> CreativeModeTab.builder()
                                        .title(Component.translatable("itemGroup.analogaudio.analog_audio_tab"))
                                        .icon(() -> new ItemStack(ModItems.CASSETTE_TAPE.get()))
                                        .displayItems((parameters, output) -> {
                                                output.accept(ModItems.CASSETTE_DECK.get());
                                                output.accept(ModItems.RADIO.get());
                                                if (ModItems.SPEAKER != null)
                                                        output.accept(ModItems.SPEAKER.get());
                                                if (ModItems.WALKIE_TALKIE != null)
                                                        output.accept(ModItems.WALKIE_TALKIE.get());

                                                output.accept(ModItems.CASSETTE_TAPE.get());
                                                output.accept(ModItems.CASSETTE_BAG.get());
                                        })
                                        .build());
}
