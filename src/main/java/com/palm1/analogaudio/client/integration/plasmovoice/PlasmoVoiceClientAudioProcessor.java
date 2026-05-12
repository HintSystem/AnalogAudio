package com.palm1.analogaudio.client.integration.plasmovoice;

import com.palm1.analogaudio.client.ClientSignalTracker;
import com.palm1.analogaudio.client.audio.CommonAudioProcessor;
import su.plo.voice.api.client.PlasmoVoiceClient;
import su.plo.voice.api.client.audio.device.source.AlSource;
import su.plo.voice.api.client.audio.source.ClientAudioSource;
import su.plo.voice.api.client.event.audio.device.source.AlSourceClosedEvent;
import su.plo.voice.api.client.event.audio.device.source.AlSourceWriteEvent;
import su.plo.voice.api.client.event.audio.source.AudioSourceInitializedEvent;
import su.plo.voice.api.event.EventSubscribe;
import su.plo.voice.api.util.AudioUtil;
import su.plo.voice.proto.data.audio.source.SourceInfo;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlasmoVoiceClientAudioProcessor {
    @SuppressWarnings("unused")
    private final PlasmoVoiceClient voiceClient;
    private final Map<Long, ClientAudioSource<?>> sourceMapping = new ConcurrentHashMap<>();

    public PlasmoVoiceClientAudioProcessor(PlasmoVoiceClient voiceClient) {
        this.voiceClient = voiceClient;
        voiceClient.getSourceManager().getSources().forEach(source -> {
            sourceMapping.put(source.getSource().getPointer(), source);
        });
    }

    @EventSubscribe
    public void onSourceInitialized(AudioSourceInitializedEvent event) {
        ClientAudioSource<?> source = event.getSource();
        sourceMapping.put(source.getSource().getPointer(), source);
    }

    @EventSubscribe
    public void onSourceClosed(AlSourceClosedEvent event) {
        AlSource alSource = event.getSource();
        ClientAudioSource<?> source = sourceMapping.remove(alSource.getPointer());
        if (source != null) {
            CommonAudioProcessor.cleanUp(source.getSourceInfo().getId());
        }
    }

    @EventSubscribe
    public void onAlSourceWrite(AlSourceWriteEvent event) {
        AlSource alSource = event.getSource();
        ClientAudioSource<?> clientSource = sourceMapping.get(alSource.getPointer());

        if (clientSource == null)
            return;

        SourceInfo sourceInfo = clientSource.getSourceInfo();
        UUID sourceId = sourceInfo.getId();

        ClientSignalTracker.SignalInfo signal = ClientSignalTracker.getSignal(sourceId);
        if (signal == null)
            return;

        byte[] samples = event.getSamples();
        short[] audio = AudioUtil.bytesToShorts(samples);
        CommonAudioProcessor.processAudio(audio, sourceId, signal);
        event.setSamples(AudioUtil.shortsToBytes(audio));
    }
}
