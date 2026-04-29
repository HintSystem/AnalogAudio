package com.palm1.analogaudio.client.audio.api;

public interface IRadioStreamer {
    void setSettings(float volume, boolean looping);
    void updatePosition(double x, double y, double z, double playerX, double playerY, double playerZ);
    boolean isPlaying();
    String getCurrentUUID();
    void setCurrentUUID(String uuid);
    float getAmplitude();
    void stop();
    void start();
    void playTrack(String url);
}
