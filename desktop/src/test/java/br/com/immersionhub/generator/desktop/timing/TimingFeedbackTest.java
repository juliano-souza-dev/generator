package br.com.immersionhub.generator.desktop.timing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimingFeedbackTest {
    @Test
    void productMessagesDoNotExposeImplementationNames() {
        String all = String.join(" ",
            TimingFeedback.playbackFailure(),
            TimingFeedback.waveformFailure(),
            TimingFeedback.cutFailure()
        );

        for (String forbidden : new String[] {"ffmpeg", "codec", "javafx", "maven", ".exe", "\\"}) {
            assertFalse(all.toLowerCase().contains(forbidden.toLowerCase()));
        }

        assertTrue(TimingFeedback.cutFailure().contains("IN"));
        assertTrue(TimingFeedback.cutFailure().contains("OUT"));
    }
}
