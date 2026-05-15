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
import su.plo.voice.api.server.PlasmoVoiceServer;

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
            LOGGER.info("Activating integration with Simple Voice Chat.");
        }

        if (net.neoforged.fml.ModList.get().isLoaded("plasmovoice")) {
            LOGGER.info("Activating integration with Plasmo Voice.");
            try {
                PlasmoVoiceServer.getAddonsLoader()
                        .load(new com.palm1.analogaudio.integration.plasmovoice.AnalogAudioPlasmoVoiceAddon());
            } catch (Exception e) {
                LOGGER.error("Failed to load Plasmo Voice addon", e);
            }
        }

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(AnalogAudioNetwork::registerPayloads);
        modEventBus.register(ModCapabilities.class);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::serverStarting);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::serverStopping);

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
            LOGGER.info("Successfully integrated with Simple Voice Chat!");
        }
        if (net.neoforged.fml.ModList.get().isLoaded("plasmovoice")) {
            LOGGER.info("Successfully integrated with Plasmo Voice!");
        }
    }

    private void serverStarting(net.neoforged.neoforge.event.server.ServerStartingEvent event) {
        com.palm1.analogaudio.util.FileServerEngine.start();
    }

    private void serverStopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        com.palm1.analogaudio.util.FileServerEngine.stop();
    }
}
