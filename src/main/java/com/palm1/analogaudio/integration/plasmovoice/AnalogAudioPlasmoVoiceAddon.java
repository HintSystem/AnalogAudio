package com.palm1.analogaudio.integration.plasmovoice;

import su.plo.voice.api.addon.AddonInitializer;
import su.plo.voice.api.addon.AddonLoaderScope;
import su.plo.voice.api.addon.InjectPlasmoVoice;
import su.plo.voice.api.addon.annotation.Addon;
import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.audio.line.ServerSourceLine;

@Addon(id = "analogaudio", scope = AddonLoaderScope.SERVER, version = "0.1.0", authors = { "palm1" })
public class AnalogAudioPlasmoVoiceAddon implements AddonInitializer {

    @InjectPlasmoVoice
    private PlasmoVoiceServer voiceServer;

    private ServerSourceLine walkieLine;
    private ServerSourceLine speakerLine;

    @Override
    public void onAddonInitialize() {
        PlasmoVoiceApiHandle.setServerApi(voiceServer);
        this.walkieLine = voiceServer.getSourceLineManager().createBuilder(
                this,
                "walkie_talkies",
                "voicechat.category.walkie_talkies",
                "plasmovoice:textures/icons/microphone.png",
                100).build();

        this.speakerLine = voiceServer.getSourceLineManager().createBuilder(
                this,
                "speaker_blocks",
                "voicechat.category.speaker_blocks",
                "plasmovoice:textures/icons/speaker.png",
                101).build();
        voiceServer.getEventBus().register(this, new PlasmoVoiceAudioRouter(this, walkieLine, speakerLine));
    }

    public PlasmoVoiceServer getVoiceServer() {
        return voiceServer;
    }
}
