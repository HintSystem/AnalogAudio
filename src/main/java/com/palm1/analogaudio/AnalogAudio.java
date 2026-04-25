package com.palm1.analogaudio;

import com.mojang.logging.LogUtils;
import com.palm1.analogaudio.network.AnalogAudioNetwork;
import com.palm1.analogaudio.registry.*;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(AnalogAudio.MODID)
public class AnalogAudio {
    public static final String MODID = "analogaudio";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AnalogAudio(IEventBus modEventBus, net.neoforged.fml.ModContainer modContainer) {
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER,
                com.palm1.analogaudio.config.ModConfig.SERVER_SPEC);
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT,
                com.palm1.analogaudio.config.ModConfig.CLIENT_SPEC);

        com.palm1.analogaudio.registry.ModBlocks.BLOCKS.register(modEventBus);
        com.palm1.analogaudio.registry.ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);

        com.palm1.analogaudio.integration.CreateCompat.init(modEventBus);

        com.palm1.analogaudio.registry.ModMenus.MENUS.register(modEventBus);
        com.palm1.analogaudio.registry.ModSounds.SOUNDS.register(modEventBus);
        com.palm1.analogaudio.registry.ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        com.palm1.analogaudio.registry.ModRecipeSerializers.SERIALIZERS.register(modEventBus);

        if (net.neoforged.fml.ModList.get().isLoaded("voicechat")) {
            LOGGER.info("Integrating voice chat.");
        }

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(AnalogAudioNetwork::registerPayloads);

        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
            com.palm1.analogaudio.client.AnalogAudioClient.register(modEventBus);
            modContainer.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                    (client, parent) -> new com.palm1.analogaudio.client.gui.ConfigScreen(parent));
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(com.palm1.analogaudio.integration.SableCompat::init);
        LOGGER.info("Successfully integrated voice chat!");
    }
}
