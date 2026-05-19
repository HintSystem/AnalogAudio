package com.palm1.analogaudio.integration.voicechat;

import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.packets.LocationalSoundPacket;
import de.maxhenkel.voicechat.api.packets.StaticSoundPacket;
import de.maxhenkel.voicechat.api.Position;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.UUID;

import com.palm1.analogaudio.config.ModConfig;
import com.palm1.analogaudio.integration.SableCompat;
import com.palm1.analogaudio.integration.VoiceRoutingHelper;
import com.palm1.analogaudio.registry.ModDataComponents;

public class VoiceAudioRouter {

    public static void onMicrophonePacket(MicrophonePacketEvent event) {
        VoicechatConnection senderConnection = event.getSenderConnection();
        if (senderConnection == null)
            return;

        ServerPlayer sender = senderConnection.getPlayer();
        if (sender == null)
            return;

        net.minecraft.server.level.ServerPlayer actualPlayer = (net.minecraft.server.level.ServerPlayer) sender
                .getPlayer();
        ItemStack walkieTalkie = VoiceRoutingHelper.getHeldWalkieTalkie(actualPlayer);

        if (walkieTalkie == null)
            return;

        int activeFrequency = walkieTalkie.getOrDefault(ModDataComponents.FREQUENCY.get(), 1);
        UUID channelId = actualPlayer.getUUID();

        StaticSoundPacket staticPacket = event.getPacket().staticSoundPacketBuilder()
                .channelId(channelId)
                .category("walkie_talkies")
                .build();

        for (net.minecraft.server.level.ServerPlayer recipient : VoiceRoutingHelper
                .getRecipientsOnFrequency(actualPlayer.server, actualPlayer, activeFrequency)) {
            VoicechatApiHandle.ifServerPresent(serverApi -> {
                VoicechatConnection recipientConn = serverApi.getConnectionOf(recipient.getUUID());
                if (recipientConn != null) {
                    VoiceRoutingHelper.sendRadioSignal(recipient, channelId, actualPlayer.position(), activeFrequency,
                            false);
                    serverApi.sendStaticSoundPacketTo(recipientConn, staticPacket);
                }
            });
        }

        Set<SpeakerInstance> speakers = SpeakerManager.getSpeakersOnFrequency(activeFrequency, false);
        for (SpeakerInstance speaker : speakers) {
            if (speaker.getLevel() == null)
                continue;

            Vec3 speakerPos = speaker.getPosition();
            VoicechatApiHandle.ifServerPresent(serverApi -> {
                Position position = serverApi.createPosition(speakerPos.x(), speakerPos.y(), speakerPos.z());

                UUID sessionChannelId = UUID.nameUUIDFromBytes(
                        ("speaker" + speaker.getIdentity().toString() + sender.getUuid()).getBytes());

                LocationalSoundPacket locationalPacket = event.getPacket().locationalSoundPacketBuilder()
                        .channelId(sessionChannelId)
                        .category("speaker_blocks")
                        .distance((float) ModConfig.Synced.globalSpeakerRange)
                        .position(position)
                        .build();

                for (net.minecraft.server.level.ServerPlayer recipient : actualPlayer.server.getPlayerList()
                        .getPlayers()) {
                    if (recipient == actualPlayer && !ModConfig.Client.speakerEcho)
                        continue;

                    Vec3 recipientGlobalPos = SableCompat.getGlobalPos(recipient.level(), recipient.position());
                    double distSq = recipientGlobalPos.distanceToSqr(speakerPos);

                    int range = ModConfig.Synced.globalSpeakerRange;
                    if (distSq > range * range)
                        continue;

                    VoicechatConnection targetConn = serverApi.getConnectionOf(recipient.getUUID());
                    if (targetConn != null) {
                        VoiceRoutingHelper.sendRadioSignal(recipient, sessionChannelId, speakerPos, activeFrequency,
                                true);
                        serverApi.sendLocationalSoundPacketTo(targetConn, locationalPacket);
                    }
                }
                speaker.onVoicePacketReceived(actualPlayer.getName().getString());
            });
        }
    }
}
