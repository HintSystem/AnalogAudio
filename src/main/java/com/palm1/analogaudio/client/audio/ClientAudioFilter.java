package com.palm1.analogaudio.client.audio;

import java.util.Random;

public class ClientAudioFilter {
    private static final Random RANDOM = new Random();

    private float lastHpfOutput = 0;
    private float lastHpfInput = 0;
    private float lastLpf = 0;

    private float bqX1 = 0, bqX2 = 0;
    private float bqY1 = 0, bqY2 = 0;

    public void apply(short[] audio, double distance, int frequency) {
        // Easter egg frequency
        if (frequency == 14) {
            applySmoothPitchShift(audio, 1.25f);
        }

        float signalStrength = calculateSignalStrength(distance);
        float noiseProbability = Math.max(0.05f, (1.0f - signalStrength) * 0.15f);
        float noiseLevel = (1.0f - signalStrength) * 1500f + 200f;

        float alphaHpf = 0.9622f;
        float alphaLpf = 0.3080f;

        for (int i = 0; i < audio.length; i++) {
            float sample = audio[i];

            // High-Pass Filter
            float filteredHpf = alphaHpf * (lastHpfOutput + sample - lastHpfInput);
            lastHpfInput = sample;
            lastHpfOutput = filteredHpf;
            sample = filteredHpf;

            // Low-Pass Filter
            sample = lastLpf + alphaLpf * (sample - lastLpf);
            lastLpf = sample;

            // Noise Floor (static)
            sample += (RANDOM.nextFloat() - 0.5f) * 800f;

            // Resonant band-pass
            float bqOutput = 0.07036f * sample - 0.07036f * bqX2 - (-1.81071f * bqY1) - (0.85928f * bqY2);
            bqX2 = bqX1;
            bqX1 = sample;
            bqY2 = bqY1;
            bqY1 = bqOutput;
            sample = bqOutput;

            sample = (float) (Math.tanh(sample / 32768.0 * 2.0) * 32768.0);

            // Crunchiness
            if (RANDOM.nextFloat() < noiseProbability) {
                sample += (RANDOM.nextFloat() - 0.5f) * noiseLevel;
            }

            audio[i] = (short) clamp(sample, -32768, 32767);
        }
    }

    private void applySmoothPitchShift(short[] audio, float pitch) {
        short[] original = audio.clone();
        for (int i = 0; i < audio.length; i++) {
            float pointer = i * pitch;
            int idx1 = (int) pointer % original.length;
            int idx2 = (idx1 + 1) % original.length;
            float frac = pointer - (int) pointer;

            audio[i] = (short) (original[idx1] * (1.0f - frac) + original[idx2] * frac);
        }
    }

    private float calculateSignalStrength(double distance) {
        if (distance <= 512)
            return 1.0f;
        if (distance >= 1024)
            return 0.1f;
        return 1.0f - (float) ((distance - 512) / (1024 - 512)) * 0.9f;
    }

    private float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }
}
