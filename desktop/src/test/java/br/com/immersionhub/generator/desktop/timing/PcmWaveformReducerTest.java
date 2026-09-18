package br.com.immersionhub.generator.desktop.timing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PcmWaveformReducerTest {
    @Test
    void reducesPcmIntoNormalizedPeaks() {
        byte[] pcm = new byte[] {
            0, 0,
            (byte) 0xff, 0x7f,
            0, 0,
            0, (byte) 0x80
        };

        var peaks = new PcmWaveformReducer().reduce(pcm, 2);
        assertEquals(2, peaks.size());
        assertTrue(peaks.get(0) > 0.99);
        assertTrue(peaks.get(1) >= 1.0 - 1e-6);
    }
}
