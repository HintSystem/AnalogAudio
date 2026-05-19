package com.palm1.analogaudio.network;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.UUID;

import com.palm1.analogaudio.block.entity.RadioBlockEntity;
import com.palm1.analogaudio.config.ModConfig;
import com.palm1.analogaudio.inventory.CassetteDeckMenu;
import com.palm1.analogaudio.item.CassetteData;
import com.palm1.analogaudio.network.packet.EraseCassetteC2SPacket;
import com.palm1.analogaudio.util.FileServerEngine;
import com.palm1.analogaudio.network.packet.NextTrackC2SPacket;
import com.palm1.analogaudio.network.packet.RadioSignalS2CPacket;
import com.palm1.analogaudio.network.packet.SetFrequencyC2SPacket;
import com.palm1.analogaudio.network.packet.SyncConfigS2CPacket;
import com.palm1.analogaudio.network.packet.UpdateRadioSettingsC2SPacket;
import com.palm1.analogaudio.network.packet.WriteCassetteC2SPacket;
import com.palm1.analogaudio.network.packet.WriteResultS2CPacket;
import com.palm1.analogaudio.network.packet.RequestTokenC2SPacket;
import com.palm1.analogaudio.network.packet.TokenResponseS2CPacket;
import com.palm1.analogaudio.registry.ModDataComponents;
import com.palm1.analogaudio.registry.ModItems;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.fml.common.EventBusSubscriber;

import com.palm1.analogaudio.util.ModPermissions;
import net.minecraft.server.level.ServerPlayer;
import java.util.function.BiConsumer;

@EventBusSubscriber(modid = "analogaudio")
public class AnalogAudioNetwork {
    public static java.util.function.Consumer<String> tokenCallback = null;
    public static BiConsumer<WriteResultS2CPacket, IPayloadContext> writeResultHandler = (d, c) -> {
    };
    public static BiConsumer<RadioSignalS2CPacket, IPayloadContext> radioSignalHandler = (d, c) -> {
    };
    public static BiConsumer<SyncConfigS2CPacket, IPayloadContext> syncConfigHandler = (d, c) -> {
        ModConfig.Synced.set(d.whitelistedUrls(), d.whitelistAsBlacklist(), d.enableWalkieFiltering(),
                d.allowFileUploads(), d.globalRadioRange(), d.globalSpeakerRange(),
                d.fileServerEnabled(), d.fileServerPort());
    };

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
                (data, context) -> writeResultHandler.accept(data, context));

        registrar.playToClient(
                RadioSignalS2CPacket.TYPE,
                RadioSignalS2CPacket.STREAM_CODEC,
                (data, context) -> radioSignalHandler.accept(data, context));

        registrar.playToServer(
                SetFrequencyC2SPacket.TYPE,
                SetFrequencyC2SPacket.STREAM_CODEC,
                SetFrequencyC2SPacket::handle);

        registrar.playToServer(
                NextTrackC2SPacket.TYPE,
                NextTrackC2SPacket.STREAM_CODEC,
                AnalogAudioNetwork::handleNextTrack);

        registrar.playToClient(
                SyncConfigS2CPacket.TYPE,
                SyncConfigS2CPacket.STREAM_CODEC,
                (data, context) -> syncConfigHandler.accept(data, context));

        registrar.playToServer(
                RequestTokenC2SPacket.TYPE,
                RequestTokenC2SPacket.STREAM_CODEC,
                AnalogAudioNetwork::handleRequestToken);

        registrar.playToClient(
                TokenResponseS2CPacket.TYPE,
                TokenResponseS2CPacket.STREAM_CODEC,
                (data, context) -> {
                    if (tokenCallback != null) {
                        tokenCallback.accept(data.token());
                        tokenCallback = null;
                    }
                });
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncConfig(serverPlayer);
        }
    }

    public static void syncConfig(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SyncConfigS2CPacket(
                ModConfig.Server.whitelistedUrls,
                ModConfig.Server.whitelistAsBlacklist,
                ModConfig.Server.enableWalkieFiltering,
                ModPermissions.canUploadFiles(player),
                ModConfig.Server.globalRadioRange,
                ModConfig.Server.globalSpeakerRange,
                FileServerEngine.isRunning(),
                ModConfig.FileServer.port));
    }

    private static void handleNextTrack(final NextTrackC2SPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            BlockEntity be = context.player().level().getBlockEntity(data.pos());
            if (be instanceof RadioBlockEntity radio) {
                radio.skipToNextTrack();
            }
        });
    }

    private static void handleWriteCassette(final WriteCassetteC2SPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();

            if (!data.url().isEmpty()) {
                if (!isValidUrl(data.url(), player)) {
                    context.reply(new WriteResultS2CPacket(4));
                    return;
                }

                String url = data.url().toLowerCase();
                if (url.startsWith("file:///") || url.startsWith("server:")) {
                    String extension = "";
                    int lastDot = url.lastIndexOf('.');
                    if (lastDot > 0) {
                        extension = url.substring(lastDot + 1);
                        if (extension.contains("?")) {
                            extension = extension.substring(0, extension.indexOf('?'));
                        }
                    }

                    boolean allowed = false;
                    for (String format : com.palm1.analogaudio.config.ModConfig.FileServer.allowedFileFormats) {
                        if (format.equalsIgnoreCase(extension)) {
                            allowed = true;
                            break;
                        }
                    }

                    if (!allowed) {
                        context.reply(new WriteResultS2CPacket(5));
                        return;
                    }
                }
            }

            if (player.containerMenu instanceof CassetteDeckMenu deckMenu) {
                ItemStack cassette = deckMenu.getInventory().getItem(0);
                if (!cassette.isEmpty() && cassette.is(ModItems.CASSETTE_TAPE.get())) {
                    CassetteData oldData = cassette.get(ModDataComponents.CASSETTE_DATA.get());

                    String uuid = oldData != null ? oldData.uuid() : UUID.randomUUID().toString();
                    String url = data.url().isEmpty() && oldData != null ? oldData.url() : data.url();
                    String name = data.name().isEmpty() && oldData != null ? oldData.name() : data.name();

                    cassette.set(ModDataComponents.CASSETTE_DATA.get(),
                            new CassetteData(uuid, url, name, data.color(),
                                    oldData != null ? oldData.volume() : -1.0f,
                                    data.duration(),
                                    player.getUUID().toString()));
                    deckMenu.getInventory().setChanged();
                    context.reply(new WriteResultS2CPacket(0));
                } else {
                    context.reply(new WriteResultS2CPacket(1));
                }
            }
        });
    }

    private static boolean isValidUrl(String url, Player player) {
        if (url.startsWith("server:") || url.startsWith("file:///")) {
            return true;
        }

        if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            return false;
        }

        try {
            String host = java.net.URI.create(url).toURL().getHost().toLowerCase();
            java.util.List<String> domains = ModConfig.Server.whitelistedUrls;
            boolean isBlacklist = ModConfig.Server.whitelistAsBlacklist;

            if (domains.isEmpty()) {
                return !isBlacklist;
            }

            boolean found = false;
            for (String domain : domains) {
                if (host.equals(domain.toLowerCase()) || host.endsWith("." + domain.toLowerCase())) {
                    found = true;
                    break;
                }
            }
            return isBlacklist ? !found : found;
        } catch (Exception e) {
            return false;
        }
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
                radio.setSettings(data.volume(), data.looping(), data.playing(), data.shuffle(), data.resetCassetteVolume());
            }
        });
    }

    private static void handleRequestToken(final RequestTokenC2SPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                String token = com.palm1.analogaudio.util.FileServerEngine.TokenManager.generateToken(player.getUUID());
                context.reply(new TokenResponseS2CPacket(token));
            }
        });
    }
}
