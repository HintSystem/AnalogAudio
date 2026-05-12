package com.palm1.analogaudio.integration.plasmovoice;

import com.palm1.analogaudio.client.integration.plasmovoice.PlasmoVoiceClientAudioProcessor;

import su.plo.voice.api.addon.AddonInitializer;
import su.plo.voice.api.addon.AddonLoaderScope;
import su.plo.voice.api.addon.InjectPlasmoVoice;
import su.plo.voice.api.addon.annotation.Addon;
import su.plo.voice.api.client.PlasmoVoiceClient;

@Addon(id = "analogaudio_client", scope = AddonLoaderScope.CLIENT, version = "1.0.0", authors = { "palm1" })
public class AnalogAudioPlasmoVoiceClientAddon implements AddonInitializer {

    @InjectPlasmoVoice
    private PlasmoVoiceClient voiceClient;

    @Override
    public void onAddonInitialize() {
        PlasmoVoiceApiHandle.setClientApi(voiceClient);
        voiceClient.getEventBus().register(this, new PlasmoVoiceClientAudioProcessor(voiceClient));
    }
}
