package com.palm1.analogaudio.integration.plasmovoice;

import su.plo.voice.api.event.EventSubscribe;
import su.plo.voice.api.server.audio.capture.PlayerActivationInfo;
import su.plo.voice.api.server.audio.line.ServerSourceLine;
import su.plo.voice.api.server.audio.source.ServerAudioSource;
import su.plo.voice.api.server.audio.source.ServerBroadcastSource;
import su.plo.voice.api.server.audio.source.ServerStaticSource;
import su.plo.voice.api.server.event.audio.source.PlayerSpeakEndEvent;
import su.plo.voice.api.server.event.audio.source.PlayerSpeakEvent;
import su.plo.voice.api.server.player.VoiceServerPlayer;
import su.plo.voice.proto.data.audio.codec.opus.OpusDecoderInfo;
import su.plo.voice.proto.packets.udp.clientbound.SourceAudioPacket;
import su.plo.slib.api.server.position.ServerPos3d;
import su.plo.slib.api.server.world.McServerWorld;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.List;

import com.palm1.analogaudio.config.ModConfig;
import com.palm1.analogaudio.integration.SableCompat;
import com.palm1.analogaudio.integration.VoiceRoutingHelper;
import com.palm1.analogaudio.integration.voicechat.SpeakerInstance;
import com.palm1.analogaudio.integration.voicechat.SpeakerManager;
import com.palm1.analogaudio.registry.ModDataComponents;

public class PlasmoVoiceAudioRouter {

    private final AnalogAudioPlasmoVoiceAddon addon;
    private final ServerSourceLine walkieLine;
    private final ServerSourceLine speakerLine;

    private final Map<UUID, ServerBroadcastSource> walkieSources = new ConcurrentHashMap<>();
    private final Map<String, ServerStaticSource> speakerSources = new ConcurrentHashMap<>();

    public PlasmoVoiceAudioRouter(AnalogAudioPlasmoVoiceAddon addon, ServerSourceLine walkieLine,
            ServerSourceLine speakerLine) {
        this.addon = addon;
        this.walkieLine = walkieLine;
        this.speakerLine = speakerLine;
    }

    @EventSubscribe(ignoreCancelled = false)
    public void onPlayerSpeak(PlayerSpeakEvent event) {
        VoiceServerPlayer player = (VoiceServerPlayer) event.getPlayer();
        UUID playerId = player.getInstance().getUuid();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
            return;
        ServerPlayer actualPlayer = server.getPlayerList().getPlayer(playerId);
        if (actualPlayer == null)
            return;

        ItemStack walkieTalkie = VoiceRoutingHelper.getHeldWalkieTalkie(actualPlayer);
        if (walkieTalkie == null)
            return;

        int activeFrequency = walkieTalkie.getOrDefault(ModDataComponents.FREQUENCY.get(), 1);

        ServerBroadcastSource walkieSource = walkieSources.computeIfAbsent(playerId, id -> {
            ServerBroadcastSource source = walkieLine.createBroadcastSource(false, new OpusDecoderInfo());
            source.setSender(player);
            source.setName("Walkie Talkie");
            return source;
        });

        List<VoiceServerPlayer> recipients = new ArrayList<>();
        for (ServerPlayer recipient : VoiceRoutingHelper.getRecipientsOnFrequency(server, actualPlayer,
                activeFrequency)) {
            addon.getVoiceServer().getPlayerManager().getPlayerById(recipient.getUUID()).ifPresent(voiceRecipient -> {
                recipients.add((VoiceServerPlayer) voiceRecipient);
                VoiceRoutingHelper.sendRadioSignal(recipient, walkieSource.getId(), actualPlayer.position(),
                        activeFrequency, false);
            });
        }

        if (!recipients.isEmpty()) {
            walkieSource.setPlayers(recipients);
            sendAudio(walkieSource, event.getPacket(), player);
        }

        Set<SpeakerInstance> speakers = SpeakerManager.getSpeakersOnFrequency(activeFrequency);
        for (SpeakerInstance speaker : speakers) {
            if (speaker.getLevel() == null)
                continue;

            Vec3 speakerPos = speaker.getPosition();
            Object speakerId = speaker.getIdentity();

            String dimensionName = speaker.getLevel().dimension().location().toString();
            McServerWorld mcWorld = addon.getVoiceServer().getMinecraftServer().getWorlds().stream()
                    .filter(w -> w.getName().equals(dimensionName))
                    .findFirst()
                    .orElse(null);

            if (mcWorld == null)
                continue;

            String speakerKey = speakerId.toString() + "-" + playerId.toString();
            ServerStaticSource speakerSource = speakerSources.computeIfAbsent(speakerKey, id -> {
                ServerStaticSource source = speakerLine.createStaticSource(
                        new ServerPos3d(mcWorld, speakerPos.x(), speakerPos.y(), speakerPos.z()),
                        false,
                        new OpusDecoderInfo());
                source.setName("Speaker");
                return source;
            });

            speakerSource.setPosition(new ServerPos3d(mcWorld, speakerPos.x(), speakerPos.y(), speakerPos.z()));

            sendAudio(speakerSource, event.getPacket(), (short) ModConfig.Synced.globalSpeakerRange, player);
            speaker.onVoicePacketReceived(actualPlayer.getName().getString());

            int range = ModConfig.Synced.globalSpeakerRange;
            for (ServerPlayer recipient : server.getPlayerList().getPlayers()) {
                Vec3 recipientGlobalPos = SableCompat.getGlobalPos(recipient.level(), recipient.position());
                double distSq = recipientGlobalPos.distanceToSqr(speakerPos);
                if (distSq <= range * range) {
                    VoiceRoutingHelper.sendRadioSignal(recipient, speakerSource.getId(), speakerPos, activeFrequency,
                            true);
                }
            }
        }
    }

    @EventSubscribe(ignoreCancelled = false)
    public void onPlayerSpeakEnd(PlayerSpeakEndEvent event) {
        VoiceServerPlayer player = (VoiceServerPlayer) event.getPlayer();
        UUID playerId = player.getInstance().getUuid();

        ServerBroadcastSource walkieSource = walkieSources.remove(playerId);
        if (walkieSource != null) {
            walkieSource.sendAudioEnd(event.getPacket().getSequenceNumber());
            walkieSource.remove();
        }

        short distance = (short) ModConfig.Synced.globalSpeakerRange;
        speakerSources.entrySet().removeIf(entry -> {
            if (entry.getKey().endsWith("-" + playerId.toString())) {
                entry.getValue().sendAudioEnd(event.getPacket().getSequenceNumber(), distance);
                entry.getValue().remove();
                return true;
            }
            return false;
        });
    }

    private void sendAudio(ServerAudioSource<?> source,
            su.plo.voice.proto.packets.udp.serverbound.PlayerAudioPacket packet, VoiceServerPlayer player) {
        sendAudio(source, packet, (short) 0, player);
    }

    private void sendAudio(ServerAudioSource<?> source,
            su.plo.voice.proto.packets.udp.serverbound.PlayerAudioPacket packet, short distance,
            VoiceServerPlayer player) {
        if (source instanceof ServerStaticSource staticSource) {
            if (ModConfig.Client.speakerEcho) {
                staticSource.sendAudioFrame(packet.getData(), packet.getSequenceNumber(), distance, null);
            } else {
                SourceAudioPacket sourcePacket = new SourceAudioPacket(
                        packet.getSequenceNumber(),
                        (byte) source.getState(),
                        packet.getData(),
                        source.getId(),
                        distance);
                staticSource.sendAudioPacket(sourcePacket, distance, new PlayerActivationInfo(player, packet));
            }
        } else if (source instanceof ServerBroadcastSource broadcastSource) {
            SourceAudioPacket sourcePacket = new SourceAudioPacket(
                    packet.getSequenceNumber(),
                    (byte) source.getState(),
                    packet.getData(),
                    source.getId(),
                    distance);
            broadcastSource.sendAudioPacket(sourcePacket, new PlayerActivationInfo(player, packet));
        }
    }
}
