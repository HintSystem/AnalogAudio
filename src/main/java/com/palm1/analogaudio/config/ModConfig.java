package com.palm1.analogaudio.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class ModConfig {
    public static class Server {
        public final ModConfigSpec.BooleanValue enableWalkieFiltering;

        public Server(ModConfigSpec.Builder builder) {
            builder.push("general");
            enableWalkieFiltering = builder
                    .comment("Enables the filter and static for walkie talkies and speakers.")
                    .define("enableWalkieFiltering", true);
            builder.pop();
        }
    }

    public static class Client {
        public final ModConfigSpec.BooleanValue enableCassetteAnimation;
        public final ModConfigSpec.BooleanValue enableSpeakerAnimation;

        public Client(ModConfigSpec.Builder builder) {
            builder.push("visuals");
            enableCassetteAnimation = builder
                    .comment("Enables the cassette block (cassette deck + radio) animation.")
                    .define("enableCassetteAnimation", true);
            enableSpeakerAnimation = builder
                    .comment("Enables the speaker block animation.")
                    .define("enableSpeakerAnimation", true);
            builder.pop();
        }
    }

    public static final ModConfigSpec SERVER_SPEC;
    public static final Server SERVER_CONFIG;
    static {
        final Pair<Server, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Server::new);
        SERVER_SPEC = specPair.getRight();
        SERVER_CONFIG = specPair.getLeft();
    }

    public static final ModConfigSpec CLIENT_SPEC;
    public static final Client CLIENT_CONFIG;
    static {
        final Pair<Client, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Client::new);
        CLIENT_SPEC = specPair.getRight();
        CLIENT_CONFIG = specPair.getLeft();
    }
}
