package com.palm1.analogaudio.integration.voicechat;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatClientApi;
import de.maxhenkel.voicechat.api.VoicechatServerApi;

import java.util.Optional;
import java.util.function.Consumer;

public class VoicechatApiHandle {
    private static VoicechatApi api;
    private static VoicechatServerApi serverApi;
    private static VoicechatClientApi clientApi;

    public static void setApi(VoicechatApi api) {
        VoicechatApiHandle.api = api;
    }

    public static void setServerApi(VoicechatServerApi serverApi) {
        VoicechatApiHandle.serverApi = serverApi;
    }

    public static void setClientApi(VoicechatClientApi clientApi) {
        VoicechatApiHandle.clientApi = clientApi;
    }

    public static Optional<VoicechatApi> getApi() {
        return Optional.ofNullable(api);
    }

    public static Optional<VoicechatServerApi> getServerApi() {
        return Optional.ofNullable(serverApi);
    }

    public static Optional<VoicechatClientApi> getClientApi() {
        return Optional.ofNullable(clientApi);
    }

    public static void ifServerPresent(Consumer<VoicechatServerApi> action) {
        getServerApi().ifPresent(action);
    }

    public static void ifClientPresent(Consumer<VoicechatClientApi> action) {
        getClientApi().ifPresent(action);
    }
}
