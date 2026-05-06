package com.palm1.analogaudio.client.audio.api;

public interface IRadioStreamer {
    void setSettings(float volume, boolean looping);
    void updatePosition(double x, double y, double z, double pX, double pY, double pZ, double vX, double vY, double vZ);
    boolean isPlaying();
    String getCurrentUUID();
    void setCurrentUUID(String uuid);
    float getAmplitude();
    default void setOnTrackEnd(Runnable callback) {
    }
    void stop();
    void start();
    void playTrack(String url, long offsetMs);
    default void fetchDuration(String url, java.util.function.Consumer<Long> callback) {
    }
}
