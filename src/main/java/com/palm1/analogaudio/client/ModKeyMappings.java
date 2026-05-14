package com.palm1.analogaudio.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.palm1.analogaudio.AnalogAudio;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = AnalogAudio.MODID, value = Dist.CLIENT)
public class ModKeyMappings {
    public static final KeyMapping SWITCH_INPUT_MODE = new KeyMapping(
            "key.analogaudio.switch_input_mode",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            "key.categories.analogaudio");

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(SWITCH_INPUT_MODE);
    }
}
