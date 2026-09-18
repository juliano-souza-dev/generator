package br.com.immersionhub.generator.desktop.timing;

import java.util.ArrayList;
import java.util.List;

public final class PcmWaveformReducer {
    public List<Double> reduce(byte[] pcm16le, int points) {
        if (points <= 0) throw new IllegalArgumentException("points deve ser maior que zero.");
        int samples = pcm16le.length / 2;
        if (samples == 0) return List.of();

        int bucketSize = Math.max(1, (int) Math.ceil(samples / (double) points));
        List<Double> output = new ArrayList<>(Math.min(points, samples));

        for (int bucketStart = 0; bucketStart < samples; bucketStart += bucketSize) {
            int bucketEnd = Math.min(samples, bucketStart + bucketSize);
            int peak = 0;
            for (int sampleIndex = bucketStart; sampleIndex < bucketEnd; sampleIndex++) {
                int byteIndex = sampleIndex * 2;
                int low = pcm16le[byteIndex] & 0xff;
                int high = pcm16le[byteIndex + 1];
                short sample = (short) ((high << 8) | low);
                peak = Math.max(peak, Math.abs((int) sample));
            }
            output.add(Math.min(1.0, peak / 32768.0));
        }
        return List.copyOf(output);
    }
}
