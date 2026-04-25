package com.palm1.analogaudio.integration.voicechat;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface SpeakerInstance {
    int getFrequency();

    Vec3 getPosition();

    Level getLevel();

    Object getIdentity();

    void onVoicePacketReceived();
}
