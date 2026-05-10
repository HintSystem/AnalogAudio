package com.palm1.analogaudio.integration;

import net.minecraft.world.level.block.Block;
import net.neoforged.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.palm1.analogaudio.AnalogAudio;
import com.palm1.analogaudio.registry.ModBlocks;

public class CreateCompat {
    public static void init(net.neoforged.bus.api.IEventBus modEventBus) {
        if (ModList.get().isLoaded("create")) {
            modEventBus.addListener(CreateCompat::onCommonSetup);
            try {
                Class.forName("com.palm1.analogaudio.integration.create.CreateDisplaySources")
                        .getMethod("register", net.neoforged.bus.api.IEventBus.class)
                        .invoke(null, modEventBus);
            } catch (Exception e) {
                AnalogAudio.LOGGER.error("Failed to register Create Display Sources", e);
            }
        }
    }

    private static void onCommonSetup(final net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                Class<?> movementBehaviourClass = Class
                        .forName("com.simibubi.create.api.behaviour.movement.MovementBehaviour");
                Field registryField = movementBehaviourClass.getField("REGISTRY");
                Object registry = registryField.get(null);
                Method addMethod = registry.getClass().getMethod("add",
                        Block.class, movementBehaviourClass);

                Object behavior = Class.forName("com.palm1.analogaudio.integration.create.RadioMovementBehaviour")
                        .getDeclaredConstructor().newInstance();
                addMethod.invoke(registry, ModBlocks.RADIO.get(), behavior);
            } catch (Exception e) {
                AnalogAudio.LOGGER.error("Failed to register Create movement behavior", e);
            }
        });
    }
}
