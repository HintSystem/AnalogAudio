package com.palm1.analogaudio.registry;

import com.palm1.analogaudio.AnalogAudio;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModSounds {
        public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT,
                        AnalogAudio.MODID);

        public static final DeferredHolder<SoundEvent, SoundEvent> WRITE = registerSoundEvent("write");
        public static final DeferredHolder<SoundEvent, SoundEvent> ERASE = registerSoundEvent("erase");
        public static final DeferredHolder<SoundEvent, SoundEvent> WALKIE_PRESS = registerSoundEvent("walkie_press");
        public static final DeferredHolder<SoundEvent, SoundEvent> WALKIE_UNPRESS = registerSoundEvent(
                        "walkie_unpress");
        public static final DeferredHolder<SoundEvent, SoundEvent> FREQUENCY_TICK = registerSoundEvent(
                        "frequency_tick");
        public static final DeferredHolder<SoundEvent, SoundEvent> FREQUENCY_SELECT = registerSoundEvent(
                        "frequency_select");
        public static final DeferredHolder<SoundEvent, SoundEvent> CASSETTE_INSERT = registerSoundEvent(
                        "cassette_insert");
        public static final DeferredHolder<SoundEvent, SoundEvent> CASSETTE_EJECT = registerSoundEvent(
                        "cassette_eject");
        public static final DeferredHolder<SoundEvent, SoundEvent> SWITCH_ON = registerSoundEvent("switch_on");
        public static final DeferredHolder<SoundEvent, SoundEvent> SWITCH_OFF = registerSoundEvent("switch_off");
        public static final DeferredHolder<SoundEvent, SoundEvent> BAG_OPEN = registerSoundEvent("bag_open");
        public static final DeferredHolder<SoundEvent, SoundEvent> BAG_CLOSE = registerSoundEvent("bag_close");

        private static DeferredHolder<SoundEvent, SoundEvent> registerSoundEvent(String name) {
                return SOUNDS.register(name, () -> SoundEvent
                                .createVariableRangeEvent(
                                                ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID, name)));
        }
}
