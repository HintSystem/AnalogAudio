package com.palm1.analogaudio.client.audio;

import com.palm1.analogaudio.AnalogAudio;
import com.palm1.analogaudio.client.audio.api.IRadioStreamer;
import net.minecraft.client.Minecraft;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Path;

public class RadioStreamer implements IRadioStreamer {
    private int sourceId = -1;
    private int bufferId = -1;
    private boolean playing = false;
    private boolean looping = false;
    private float baseVolume = 1.0f;
    private long lastStartTime = 0;
    private Path currentFile = null;
    private String currentUUID = null;
    private float durationSeconds = 0.0f;
    private ShortBuffer pcm;
    private int sampleRate = 48000;
    private volatile boolean stopped = false;
    private Runnable trackEndCallback;

    @Override
    public void setOnTrackEnd(Runnable callback) {
        this.trackEndCallback = callback;
    }

    public void play(Path oggFile, long startTimeTicks) {
        if (playing && oggFile.equals(currentFile) && startTimeTicks == lastStartTime)
            return;

        boolean restartNeeded = playing && oggFile.equals(currentFile) && startTimeTicks != lastStartTime;
        boolean newFileNeeded = !oggFile.equals(currentFile);

        this.currentFile = oggFile;
        this.lastStartTime = startTimeTicks;
        this.stopped = false;

        Minecraft.getInstance().execute(() -> {
            if (stopped)
                return;

            if (restartNeeded || newFileNeeded) {
                if (sourceId != -1) {
                    AL10.alSourceStop(sourceId);
                    AL10.alDeleteSources(sourceId);
                    sourceId = -1;
                }
                if (newFileNeeded && bufferId != -1) {
                    AL10.alDeleteBuffers(bufferId);
                    bufferId = -1;
                    durationSeconds = 0.0f;
                    this.pcm = null;
                }
            }

            if (playing && !restartNeeded && !newFileNeeded)
                return;

            try (MemoryStack stack = MemoryStack.stackPush()) {
                if (bufferId == -1) {
                    AnalogAudio.LOGGER.info("Decoding OGG: {}", oggFile);
                    IntBuffer channels = stack.mallocInt(1);
                    IntBuffer sampleRate = stack.mallocInt(1);

                    ShortBuffer decodedPcm = STBVorbis.stb_vorbis_decode_filename(oggFile.toString(), channels,
                            sampleRate);
                    if (decodedPcm == null) {
                        AnalogAudio.LOGGER.error("Failed to decode: {}", oggFile);
                        return;
                    }

                    int numChannels = channels.get(0);
                    int format = (numChannels == 1) ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_MONO16;

                    if (numChannels == 2) {
                        ShortBuffer monoPcm = org.lwjgl.BufferUtils.createShortBuffer(decodedPcm.remaining() / 2);
                        while (decodedPcm.hasRemaining()) {
                            short left = decodedPcm.get();
                            short right = decodedPcm.hasRemaining() ? decodedPcm.get() : left;
                            monoPcm.put((short) ((left + right) / 2));
                        }
                        monoPcm.flip();
                        this.pcm = monoPcm;
                    } else {
                        this.pcm = decodedPcm;
                    }

                    this.sampleRate = sampleRate.get(0);

                    bufferId = AL10.alGenBuffers();
                    AL10.alBufferData(bufferId, format, this.pcm, this.sampleRate);

                    this.durationSeconds = (float) this.pcm.remaining() / this.sampleRate;
                }

                sourceId = AL10.alGenSources();
                AL10.alSourcei(sourceId, AL10.AL_BUFFER, bufferId);
                AL10.alSourcef(sourceId, AL10.AL_REFERENCE_DISTANCE, 16.0f);
                AL10.alSourcef(sourceId, AL10.AL_MAX_DISTANCE, 64.0f);
                AL10.alSourcef(sourceId, AL10.AL_MAX_GAIN, 1.5f);

                if (Minecraft.getInstance().level != null) {
                    float currentTicks = Minecraft.getInstance().level.getGameTime();
                    float offsetSeconds = (currentTicks - startTimeTicks) / 20.0f;

                    AnalogAudio.LOGGER.info(
                            "AnalogAudio: Streamer Play requested. currentTicks: {}, startTimeTicks: {}, rawOffset: {}",
                            currentTicks, startTimeTicks, offsetSeconds);

                    if (offsetSeconds < 0 && offsetSeconds > -0.5f) {
                        offsetSeconds = 0;
                    }

                    if (durationSeconds > 0) {
                        if (!looping && offsetSeconds > durationSeconds) {
                            AnalogAudio.LOGGER.info(
                                    "AnalogAudio: Offset {} exceeds duration {} and loop is off. Playback skipped.",
                                    offsetSeconds, durationSeconds);
                            playing = false;
                            return;
                        }
                        offsetSeconds %= durationSeconds;
                        if (offsetSeconds < 0)
                            offsetSeconds += durationSeconds;
                    }

                    if (offsetSeconds > 0) {
                        AL10.alSourcef(sourceId, AL11.AL_SEC_OFFSET, offsetSeconds);
                    }
                }

                AL10.alSourcePlay(sourceId);
                playing = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void setSettings(float volume, boolean looping) {
        this.baseVolume = volume;
        this.looping = looping;
        if (sourceId != -1) {
            Minecraft.getInstance().execute(() -> {
                AL10.alSourcei(sourceId, AL10.AL_LOOPING, looping ? AL10.AL_TRUE : AL10.AL_FALSE);
            });
        }
    }

    @Override
    public void updatePosition(double x, double y, double z, double pX, double pY, double pZ, double vX, double vY,
            double vZ) {
        if (!playing || sourceId == -1)
            return;

        runOnMainThread(() -> {
            if (!playing || sourceId == -1)
                return;
            int state = 0;
            try {
                state = AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE);
            } catch (Exception e) {
                return;
            }

            if (state == AL10.AL_STOPPED) {
                playing = false;
                if (trackEndCallback != null && !stopped) {
                    trackEndCallback.run();
                }
                return;
            }

            double dx = x - pX;
            double dy = y - pY;
            double dz = z - pZ;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            float maxDist = 64.0f;
            float fade = 1.0f - (float) (dist / maxDist);
            if (fade < 0)
                fade = 0;

            AL10.alSourcef(sourceId, AL10.AL_GAIN, fade * baseVolume);
            AL10.alSource3f(sourceId, AL10.AL_POSITION, (float) x, (float) y, (float) z);
            AL10.alSource3f(sourceId, AL11.AL_VELOCITY, (float) vX, (float) vY, (float) vZ);

            AL10.alSourcef(sourceId, AL10.AL_ROLLOFF_FACTOR, 0.0f);
            AL10.alSourcef(sourceId, AL10.AL_REFERENCE_DISTANCE, 0.0f);
        });
    }

    @Override
    public void start() {
    }

    @Override
    public void playTrack(String url, long offsetMs) {
    }

    @Override
    public boolean isPlaying() {
        return playing;
    }

    public Path getCurrentFile() {
        return currentFile;
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
        if (!playing || this.pcm == null || sourceId == -1)
            return 0;

        float offset = org.lwjgl.openal.AL10.alGetSourcef(sourceId, org.lwjgl.openal.AL11.AL_SEC_OFFSET);
        int sampleIndex = (int) (offset * this.sampleRate);

        int windowSize = 512;
        float peak = 0;

        for (int i = 0; i < windowSize; i++) {
            int idx = sampleIndex - i;
            if (idx < 0)
                idx += this.pcm.capacity();
            if (idx >= 0 && idx < this.pcm.capacity()) {
                float val = Math.abs(this.pcm.get(idx)) / 32768f;
                if (val > peak)
                    peak = val;
            }
        }

        return peak;
    }

    @Override
    public void stop() {
        this.playing = false;
        this.currentFile = null;
        this.stopped = true;
        this.pcm = null;

        runOnMainThread(() -> {
            if (sourceId != -1) {
                try {
                    AL10.alSourceStop(sourceId);
                    AL10.alDeleteSources(sourceId);
                } catch (Exception e) {
                }
                sourceId = -1;
            }
            if (bufferId != -1) {
                try {
                    AL10.alDeleteBuffers(bufferId);
                } catch (Exception e) {
                }
                bufferId = -1;
            }
        });
    }

    private void runOnMainThread(Runnable task) {
        if (Minecraft.getInstance().isSameThread()) {
            task.run();
        } else {
            Minecraft.getInstance().execute(task);
        }
    }
}
