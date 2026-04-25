package com.palm1.analogaudio.integration.voicechat;

import com.palm1.analogaudio.client.render.SpeakerBlockRenderer;
import com.palm1.analogaudio.client.render.WalkieTalkieRenderer;
import com.palm1.analogaudio.registry.ModBlockEntities;
import com.palm1.analogaudio.registry.ModItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

public class VoicechatClientHooks {
    private static WalkieTalkieRenderer cachedWalkieTalkieRenderer;

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.SPEAKER.get(),
                SpeakerBlockRenderer::new);
    }

    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (cachedWalkieTalkieRenderer == null) {
                    cachedWalkieTalkieRenderer = new WalkieTalkieRenderer(
                            Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                            Minecraft.getInstance().getEntityModels());
                }
                return cachedWalkieTalkieRenderer;
            }
        }, ModItems.WALKIE_TALKIE.get());
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(WalkieTalkieRenderer.BODY_MODEL);
        event.register(WalkieTalkieRenderer.BUTTON_MODEL);
    }
}
