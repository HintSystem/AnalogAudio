package com.palm1.analogaudio.integration.create;

import com.palm1.analogaudio.AnalogAudio;
import com.palm1.analogaudio.registry.ModBlockEntities;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.registry.CreateRegistries;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CreateDisplaySources {
    public static final DeferredRegister<DisplaySource> DISPLAY_SOURCES = DeferredRegister.create(CreateRegistries.DISPLAY_SOURCE, AnalogAudio.MODID);

    public static final DeferredHolder<DisplaySource, RadioDisplaySource> RADIO = DISPLAY_SOURCES.register("radio", RadioDisplaySource::new);
    public static final DeferredHolder<DisplaySource, SpeakerDisplaySource> SPEAKER = DISPLAY_SOURCES.register("speaker", SpeakerDisplaySource::new);

    public static void register(IEventBus modEventBus) {
        DISPLAY_SOURCES.register(modEventBus);
        modEventBus.addListener(CreateDisplaySources::onCommonSetup);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            DisplaySource.BY_BLOCK_ENTITY.add(ModBlockEntities.RADIO.get(), RADIO.get());
            if (ModBlockEntities.SPEAKER != null) {
                DisplaySource.BY_BLOCK_ENTITY.add(ModBlockEntities.SPEAKER.get(), SPEAKER.get());
            }
        });
    }
}
