package com.palm1.analogaudio;

import com.mojang.logging.LogUtils;
import com.palm1.analogaudio.config.ModConfig;
import com.palm1.analogaudio.integration.CreateCompat;
import com.palm1.analogaudio.network.AnalogAudioNetwork;
import com.palm1.analogaudio.registry.*;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(AnalogAudio.MODID)
public class AnalogAudio {
    public static final String MODID = "analogaudio";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AnalogAudio(IEventBus modEventBus, ModContainer modContainer) {
        ModConfig.load();

        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        CreateCompat.init(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        ModRecipeSerializers.SERIALIZERS.register(modEventBus);

        if (net.neoforged.fml.ModList.get().isLoaded("voicechat")) {
            LOGGER.info("Integrating voice chat.");
        }

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(AnalogAudioNetwork::registerPayloads);
        modEventBus.register(ModCapabilities.class);

        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
            try {
                Class.forName("com.palm1.analogaudio.client.AnalogAudioClient")
                        .getMethod("register", IEventBus.class, net.neoforged.fml.ModContainer.class)
                        .invoke(null, modEventBus, modContainer);
            } catch (Exception e) {
                LOGGER.error("Failed to register client-side components", e);
            }
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(com.palm1.analogaudio.integration.SableCompat::init);
        if (net.neoforged.fml.ModList.get().isLoaded("voicechat")) {
            LOGGER.info("Successfully integrated voice chat!");
        }
    }
}
