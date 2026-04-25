package com.palm1.analogaudio.network;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.UUID;

import com.palm1.analogaudio.block.entity.RadioBlockEntity;
import com.palm1.analogaudio.inventory.CassetteDeckMenu;
import com.palm1.analogaudio.item.CassetteData;
import com.palm1.analogaudio.network.packet.EraseCassetteC2SPacket;
import com.palm1.analogaudio.network.packet.RadioSignalS2CPacket;
import com.palm1.analogaudio.network.packet.SetFrequencyC2SPacket;
import com.palm1.analogaudio.network.packet.UpdateRadioSettingsC2SPacket;
import com.palm1.analogaudio.network.packet.WriteCassetteC2SPacket;
import com.palm1.analogaudio.network.packet.WriteResultS2CPacket;
import com.palm1.analogaudio.registry.ModDataComponents;
import com.palm1.analogaudio.registry.ModItems;

public class AnalogAudioNetwork {
    public static void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(
                WriteCassetteC2SPacket.TYPE,
                WriteCassetteC2SPacket.STREAM_CODEC,
                AnalogAudioNetwork::handleWriteCassette);

        registrar.playToServer(
                EraseCassetteC2SPacket.TYPE,
                EraseCassetteC2SPacket.STREAM_CODEC,
                AnalogAudioNetwork::handleEraseCassette);

        registrar.playToServer(
                UpdateRadioSettingsC2SPacket.TYPE,
                UpdateRadioSettingsC2SPacket.STREAM_CODEC,
                AnalogAudioNetwork::handleUpdateRadioSettings);

        registrar.playToClient(
                WriteResultS2CPacket.TYPE,
                WriteResultS2CPacket.STREAM_CODEC,
                ClientPacketHandlers::handleWriteResult);

        registrar.playToClient(
                RadioSignalS2CPacket.TYPE,
                RadioSignalS2CPacket.STREAM_CODEC,
                ClientPacketHandlers::handleRadioSignal);

        registrar.playToServer(
                SetFrequencyC2SPacket.TYPE,
                SetFrequencyC2SPacket.STREAM_CODEC,
                SetFrequencyC2SPacket::handle);
    }

    private static void handleWriteCassette(final WriteCassetteC2SPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();

            if (!data.url().isEmpty() && !isValidUrl(data.url())) {
                context.reply(new WriteResultS2CPacket(4));
                return;
            }

            if (player.containerMenu instanceof CassetteDeckMenu deckMenu) {
                ItemStack cassette = deckMenu.getInventory().getItem(0);
                if (!cassette.isEmpty() && cassette.is(ModItems.CASSETTE_TAPE.get())) {
                    CassetteData oldData = cassette.get(ModDataComponents.CASSETTE_DATA.get());

                    String uuid = oldData != null ? oldData.uuid() : UUID.randomUUID().toString();
                    String url = data.url().isEmpty() && oldData != null ? oldData.url() : data.url();
                    String name = data.name().isEmpty() && oldData != null ? oldData.name() : data.name();

                    cassette.set(ModDataComponents.CASSETTE_DATA.get(),
                            new CassetteData(uuid, url, name, data.color()));
                    deckMenu.getInventory().setChanged();
                    context.reply(new WriteResultS2CPacket(0));
                } else {
                    context.reply(new WriteResultS2CPacket(1));
                }
            }
        });
    }

    private static boolean isValidUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    private static void handleEraseCassette(final EraseCassetteC2SPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.containerMenu instanceof CassetteDeckMenu deckMenu) {
                ItemStack cassette = deckMenu.getInventory().getItem(0);
                if (!cassette.isEmpty() && cassette.is(ModItems.CASSETTE_TAPE.get())) {
                    cassette.remove(ModDataComponents.CASSETTE_DATA.get());
                    deckMenu.getInventory().setChanged();
                    context.reply(new WriteResultS2CPacket(2));
                } else {
                    context.reply(new WriteResultS2CPacket(3));
                }
            }
        });
    }

    private static void handleUpdateRadioSettings(final UpdateRadioSettingsC2SPacket data,
            final IPayloadContext context) {
        context.enqueueWork(() -> {
            BlockEntity be = context.player().level().getBlockEntity(data.pos());
            if (be instanceof RadioBlockEntity radio) {
                radio.setSettings(data.volume(), data.looping(), data.playing());
            }
        });
    }
}
