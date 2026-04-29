package com.palm1.analogaudio.integration;

import net.neoforged.fml.ModList;
import com.palm1.analogaudio.registry.ModBlocks;

public class CreateCompat {
    public static void init(net.neoforged.bus.api.IEventBus modEventBus) {
        if (ModList.get().isLoaded("create")) {
            modEventBus.addListener(CreateCompat::onCommonSetup);
        }
    }

    private static void onCommonSetup(final net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                Class<?> movementBehaviourClass = Class
                        .forName("com.simibubi.create.api.behaviour.movement.MovementBehaviour");
                java.lang.reflect.Field registryField = movementBehaviourClass.getField("REGISTRY");
                Object registry = registryField.get(null);
                java.lang.reflect.Method registerMethod = registry.getClass().getMethod("register",
                        net.minecraft.world.level.block.Block.class, movementBehaviourClass);

                Object behavior = Class.forName("com.palm1.analogaudio.integration.create.RadioMovementBehaviour")
                        .getDeclaredConstructor().newInstance();
                registerMethod.invoke(registry, ModBlocks.RADIO.get(), behavior);
            } catch (Exception e) {
                com.palm1.analogaudio.AnalogAudio.LOGGER.error("Failed to register Create movement behavior", e);
            }
        });
    }
}
