package com.palm1.analogaudio.client.integration.soundphysics;

import de.maxhenkel.voicechat.api.events.OpenALSoundEvent;
import com.palm1.analogaudio.integration.SoundPhysicsIntegration;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public class SoundPhysicsVoiceIntegration {
    public static void onOpenALSound(OpenALSoundEvent event) {
        if (!SoundPhysicsIntegration.isLoaded()) {
            return;
        }

        String category = event.getCategory();
        if ("walkie_talkies".equals(category)) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                Vec3 pos = mc.player.position();
                SoundPhysicsIntegration.processSound(
                        event.getSource(),
                        pos.x, pos.y, pos.z,
                        "VOICE",
                        "voicechat",
                        "walkie_talkies",
                        false);
            }
        }
    }
}
