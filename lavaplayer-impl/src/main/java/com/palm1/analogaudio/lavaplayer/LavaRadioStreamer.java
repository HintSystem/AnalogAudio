package com.palm1.analogaudio.lavaplayer;

import com.palm1.analogaudio.client.audio.api.IRadioStreamer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import dev.lavalink.youtube.YoutubeAudioSourceManager;

import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.Queue;

public class LavaRadioStreamer extends AudioEventAdapter implements IRadioStreamer {
    private static final AudioPlayerManager PLAYER_MANAGER;
    static {
        PLAYER_MANAGER = new DefaultAudioPlayerManager();
        PLAYER_MANAGER.registerSourceManager(new YoutubeAudioSourceManager());
        AudioSourceManagers.registerRemoteSources(PLAYER_MANAGER);
        PLAYER_MANAGER.getConfiguration()
                .setOutputFormat(com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats.COMMON_PCM_S16_BE);
    }

    private final AudioPlayer player;
    private int sourceId = -1;
    private final Queue<Integer> buffers = new LinkedList<>();
    private String currentUUID;
    private boolean playing = false;
    private boolean looping = false;
    private float volume = 1.0f;
    private double lastX, lastY, lastZ;
    private Runnable trackEndCallback;

    public LavaRadioStreamer() {
        this.player = PLAYER_MANAGER.createPlayer();
        this.player.addListener(this);
    }

    @Override
    public void setOnTrackEnd(Runnable callback) {
        this.trackEndCallback = callback;
    }

    @Override
    public void setSettings(float volume, boolean looping) {
        this.volume = volume;
        this.looping = looping;
    }

    @Override
    public void updatePosition(double x, double y, double z, double pX, double pY, double pZ, double vX, double vY,
            double vZ) {
        this.lastX = x;
        this.lastY = y;
        this.lastZ = z;

        if (sourceId == -1)
            return;

        double dx = x - pX;
        double dy = y - pY;
        double dz = z - pZ;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        float maxDist = 64.0f;
        float fade = 1.0f - (float) (dist / maxDist);
        if (fade < 0)
            fade = 0;

        AL10.alSourcef(sourceId, AL10.AL_GAIN, this.volume * fade);
        AL10.alSource3f(sourceId, AL10.AL_POSITION, (float) x, (float) y, (float) z);
        AL10.alSource3f(sourceId, AL11.AL_VELOCITY, (float) vX, (float) vY, (float) vZ);
        AL10.alSourcef(sourceId, AL10.AL_PITCH, 1.0f);
        AL10.alSourcei(sourceId, AL10.AL_SOURCE_RELATIVE, AL10.AL_FALSE);
        AL10.alSourcef(sourceId, AL10.AL_ROLLOFF_FACTOR, 0.0f);
        AL10.alSourcef(sourceId, AL10.AL_REFERENCE_DISTANCE, 0.0f);

        streamAudio();
    }

    private void streamAudio() {
        if (sourceId == -1 || !playing)
            return;

        int processed = AL10.alGetSourcei(sourceId, AL10.AL_BUFFERS_PROCESSED);
        while (processed-- > 0) {
            int buffer = AL10.alSourceUnqueueBuffers(sourceId);
            if (buffer != 0) {
                buffers.add(buffer);
            }
        }

        while (!buffers.isEmpty()) {
            AudioFrame frame = player.provide();
            if (frame == null)
                break;

            int buffer = buffers.poll();
            byte[] data = frame.getData();

            int monoLength = data.length / 2;
            ByteBuffer monoBuffer = ByteBuffer.allocateDirect(monoLength);
            monoBuffer.order(ByteOrder.nativeOrder());

            for (int i = 0; i < data.length; i += 4) {
                short left = (short) (((data[i] & 0xFF) << 8) | (data[i + 1] & 0xFF));
                short right = (short) (((data[i + 2] & 0xFF) << 8) | (data[i + 3] & 0xFF));
                short mono = (short) ((left + right) / 2);
                monoBuffer.putShort(mono);
            }
            monoBuffer.flip();

            AL10.alBufferData(buffer, AL10.AL_FORMAT_MONO16, monoBuffer, 44100);
            AL10.alSourceQueueBuffers(sourceId, buffer);
        }

        int state = AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE);
        if (state != AL10.AL_PLAYING && AL10.alGetSourcei(sourceId, AL10.AL_BUFFERS_QUEUED) > 0) {
            AL10.alSourcePlay(sourceId);
        }
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) {
            if (looping) {
                player.playTrack(track.makeClone());
            } else if (trackEndCallback != null) {
                trackEndCallback.run();
            }
        }
    }

    @Override
    public void start() {
        if (sourceId != -1)
            return;
        sourceId = AL10.alGenSources();
        int err = AL10.alGetError();
        if (err != AL10.AL_NO_ERROR || sourceId == -1) {
            System.err.println("Failed to generate OpenAL source for LavaRadioStreamer: " + err);
            sourceId = -1;
            return;
        }

        buffers.clear();
        for (int i = 0; i < 8; i++) {
            int buffer = AL10.alGenBuffers();
            if (buffer != 0)
                buffers.add(buffer);
        }
        playing = true;
        AL10.alSourcef(sourceId, AL10.AL_MAX_GAIN, 1.5f);
    }

    @Override
    public void playTrack(String url, long offsetMs) {
        PLAYER_MANAGER.loadItem(url, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                if (offsetMs > 0) {
                    long duration = track.getDuration();
                    if (duration > 0) {
                        track.setPosition(offsetMs % duration);
                    } else {
                        track.setPosition(offsetMs);
                    }
                }
                player.playTrack(track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (!playlist.getTracks().isEmpty()) {
                    AudioTrack track = playlist.getTracks().get(0);
                    if (offsetMs > 0) {
                        long duration = track.getDuration();
                        if (duration > 0) {
                            track.setPosition(offsetMs % duration);
                        } else {
                            track.setPosition(offsetMs);
                        }
                    }
                    player.playTrack(track);
                }
            }

            @Override
            public void noMatches() {
                System.err.println("No matches for URL: " + url);
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                System.err.println("Load failed for URL: " + url + " - " + exception.getMessage());
            }
        });
    }

    @Override
    public boolean isPlaying() {
        return playing;
    }

    @Override
    public String getCurrentUUID() {
        return currentUUID;
    }

    @Override
    public void setCurrentUUID(String uuid) {
        this.currentUUID = uuid;
    }

    @Override
    public float getAmplitude() {
        return 0;
    }

    @Override
    public void stop() {
        playing = false;
        if (sourceId != -1) {
            AL10.alSourceStop(sourceId);
            int queued = AL10.alGetSourcei(sourceId, AL10.AL_BUFFERS_QUEUED);
            while (queued-- > 0) {
                AL10.alSourceUnqueueBuffers(sourceId);
            }
            AL10.alDeleteSources(sourceId);
            sourceId = -1;
        }
        while (!buffers.isEmpty()) {
            int buf = buffers.poll();
            if (buf != 0)
                AL10.alDeleteBuffers(buf);
        }
        player.stopTrack();
    }
}
