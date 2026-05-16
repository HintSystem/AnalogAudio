package com.palm1.analogaudio.integration.ponder;

import com.palm1.analogaudio.AnalogAudio;
import com.palm1.analogaudio.registry.ModBlocks;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

public class PonderIntegration implements PonderPlugin {

    public static void register() {
        PonderIndex.addPlugin(new PonderIntegration());
    }

    @Override
    public String getModId() {
        return AnalogAudio.MODID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(ModBlocks.RADIO.getId())
                .addStoryBoard("radio/radio", RadioScenes::intro)
                .addStoryBoard("radio/radio2", RadioScenes::redstone);

        if (ModList.get().isLoaded("create")) {
            helper.forComponents(ModBlocks.RADIO.getId())
                    .addStoryBoard("radio/radio3", RadioScenes::displayLink)
                    .addStoryBoard("radio/radio4", RadioScenes::mechanicalArms);
        }

        if (ModBlocks.SPEAKER != null) {
            helper.forComponents(ModBlocks.SPEAKER.getId())
                    .addStoryBoard("speaker/speaker", SpeakerScenes::intro)
                    .addStoryBoard("speaker/speaker2", SpeakerScenes::redstone);

            if (ModList.get().isLoaded("create")) {
                helper.forComponents(ModBlocks.SPEAKER.getId())
                        .addStoryBoard("speaker/speaker3", SpeakerScenes::displayLink);
            }
        }
    }
}
