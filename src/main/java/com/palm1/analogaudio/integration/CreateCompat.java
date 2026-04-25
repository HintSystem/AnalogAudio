package com.palm1.analogaudio.integration;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;

import net.neoforged.fml.ModList;

import com.palm1.analogaudio.integration.create.RadioMovementBehaviour;
import com.palm1.analogaudio.registry.ModBlocks;

public class CreateCompat {
    public static void init(net.neoforged.bus.api.IEventBus modEventBus) {
        if (ModList.get().isLoaded("create")) {
            modEventBus.addListener(CreateCompat::onCommonSetup);
        }
    }

    private static void onCommonSetup(final net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(CreateCompat::registerMovementBehaviours);
    }

    private static void registerMovementBehaviours() {
        MovementBehaviour.REGISTRY.register(ModBlocks.RADIO.get(), new RadioMovementBehaviour());
    }
}
