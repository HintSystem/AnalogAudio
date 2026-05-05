package com.palm1.analogaudio.client;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.Pack.Position;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import com.palm1.analogaudio.AnalogAudio;
import com.palm1.analogaudio.client.gui.CassetteBagScreen;
import com.palm1.analogaudio.client.gui.CassetteDeckScreen;
import com.palm1.analogaudio.client.gui.ModConfigScreen;
import com.palm1.analogaudio.client.gui.RadioScreen;
import com.palm1.analogaudio.client.network.ClientPacketHandlers;
import com.palm1.analogaudio.registry.ModBlockEntities;
import com.palm1.analogaudio.registry.ModDataComponents;
import com.palm1.analogaudio.registry.ModItems;
import com.palm1.analogaudio.registry.ModMenus;
import com.palm1.analogaudio.client.render.RadioBlockRenderer;
import com.palm1.analogaudio.client.render.CassetteDeckBlockRenderer;
import com.palm1.analogaudio.client.render.SpeakerBlockRenderer;
import com.palm1.analogaudio.integration.voicechat.VoicechatClientHooks;
import com.palm1.analogaudio.item.CassetteData;
import com.palm1.analogaudio.network.AnalogAudioNetwork;

public class AnalogAudioClient {
    public static void register(IEventBus modEventBus, net.neoforged.fml.ModContainer modContainer) {
        modEventBus.addListener(AnalogAudioClient::onRegisterMenuScreens);
        modEventBus.addListener(AnalogAudioClient::onRegisterRenderers);
        modEventBus.addListener(AnalogAudioClient::onClientSetup);
        modEventBus.addListener(AnalogAudioClient::registerItemColors);
        modEventBus.addListener(AnalogAudioClient::registerModels);
        modEventBus.addListener(AnalogAudioClient::onAddPackFinders);

        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (client, parent) -> ModConfigScreen.create(parent));

        AnalogAudioNetwork.writeResultHandler = ClientPacketHandlers::handleWriteResult;
        AnalogAudioNetwork.radioSignalHandler = ClientPacketHandlers::handleRadioSignal;

        if (ModList.get().isLoaded("voicechat")) {
            modEventBus.addListener(VoicechatClientHooks::registerRenderers);
            modEventBus.addListener(VoicechatClientHooks::registerClientExtensions);
            modEventBus.addListener(VoicechatClientHooks::registerAdditionalModels);
        }
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.CASSETTE_DECK_MENU.get(), CassetteDeckScreen::new);
        event.register(ModMenus.RADIO_MENU.get(), RadioScreen::new);
        event.register(ModMenus.CASSETTE_BAG_MENU.get(), CassetteBagScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.RADIO.get(), RadioBlockRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.CASSETTE_DECK.get(), CassetteDeckBlockRenderer::new);
        if (ModBlockEntities.SPEAKER != null)
            event.registerBlockEntityRenderer(ModBlockEntities.SPEAKER.get(), SpeakerBlockRenderer::new);
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            if (tintIndex == 0) {
                CassetteData data = stack.get(ModDataComponents.CASSETTE_DATA.get());
                if (data != null) {
                    return 0xFF000000 | data.color();
                }
                return 0xFFFFFFFF;
            }
            return -1;
        }, ModItems.CASSETTE_TAPE.get());

        event.register((stack, tintIndex) -> {
            if (tintIndex == 1) {
                DyedItemColor dyedColor = stack
                        .get(DataComponents.DYED_COLOR);
                if (dyedColor != null) {
                    return 0xFF000000 | dyedColor.rgb();
                }
            }
            return -1;
        }, ModItems.CASSETTE_BAG.get());
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(ModelResourceLocation
                .standalone(ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID, "item/cassette_tape_3d")));
    }

    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            event.addPackFinders(
                    ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID, "resourcepacks/3d_cassettes"),
                    PackType.CLIENT_RESOURCES,
                    Component.literal("3D Cassettes"),
                    PackSource.BUILT_IN,
                    false,
                    Position.TOP);
        }
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(ModItems.CASSETTE_BAG.get(),
                    ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID, "dyed"),
                    (stack, level, entity,
                            seed) -> stack.has(DataComponents.DYED_COLOR) ? 1.0F : 0.0F);
        });
    }
}
