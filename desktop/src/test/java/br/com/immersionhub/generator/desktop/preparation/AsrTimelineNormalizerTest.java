package br.com.immersionhub.generator.desktop.preparation;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AsrTimelineNormalizerTest {
    @Test
    void clampsRealWhisperBoundaryOvershootWithoutChangingTranscript() {
        AsrResult raw = new AsrResult(
            "en",
            "Oh, come my left ball off if she's in sign.",
            List.of(new TimedText("Oh, come my left ball off if she's in sign.", 132120, 134200)),
            List.of(
                new TimedText("sign", 133760, 133840, 0.552303),
                new TimedText(".", 134000, 134190, 0.643781)
            )
        );

        AsrResult normalized = AsrTimelineNormalizer.normalize(raw, 134182);

        assertEquals(raw.text(), normalized.text());
        assertEquals(134182, normalized.segments().getFirst().endMs());
        assertEquals(134182, normalized.words().getLast().endMs());
        assertEquals(0.643781, normalized.words().getLast().confidence());
        assertDoesNotThrow(() -> normalized.validate(134182));
    }

    @Test
    void dropsResidualItemThatStartsAtOrAfterCutBoundary() {
        AsrResult raw = new AsrResult(
            "en",
            "hello",
            List.of(new TimedText("hello", 0, 1000)),
            List.of(
                new TimedText("hello", 0, 990),
                new TimedText(".", 1000, 1010)
            )
        );

        AsrResult normalized = AsrTimelineNormalizer.normalize(raw, 1000);

        assertEquals(1, normalized.words().size());
        assertEquals("hello", normalized.words().getFirst().text());
    }

    @Test
    void rejectsMaterialOvershootAndReportsExactItem() {
        AsrResult raw = new AsrResult(
            "en",
            "hello",
            List.of(new TimedText("hello", 0, 1100)),
            List.of()
        );

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> AsrTimelineNormalizer.normalize(raw, 1000)
        );

        assertTrue(error.getMessage().contains("segment[0]"));
        assertTrue(error.getMessage().contains("endMs=1100"));
        assertTrue(error.getMessage().contains("durationMs=1000"));
    }
}
