package com.palm1.analogaudio.integration.plasmovoice;

import su.plo.voice.api.client.PlasmoVoiceClient;
import su.plo.voice.api.server.PlasmoVoiceServer;

import java.util.Optional;
import java.util.function.Consumer;

public class PlasmoVoiceApiHandle {
    private static PlasmoVoiceServer serverApi;
    private static PlasmoVoiceClient clientApi;

    public static void setServerApi(PlasmoVoiceServer serverApi) {
        PlasmoVoiceApiHandle.serverApi = serverApi;
    }

    public static void setClientApi(PlasmoVoiceClient clientApi) {
        PlasmoVoiceApiHandle.clientApi = clientApi;
    }

    public static Optional<PlasmoVoiceServer> getServerApi() {
        return Optional.ofNullable(serverApi);
    }

    public static Optional<PlasmoVoiceClient> getClientApi() {
        return Optional.ofNullable(clientApi);
    }

    public static void ifServerPresent(Consumer<PlasmoVoiceServer> action) {
        getServerApi().ifPresent(action);
    }

    public static void ifClientPresent(Consumer<PlasmoVoiceClient> action) {
        getClientApi().ifPresent(action);
    }
}
