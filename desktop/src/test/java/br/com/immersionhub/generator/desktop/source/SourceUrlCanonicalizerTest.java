package br.com.immersionhub.generator.desktop.source;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SourceUrlCanonicalizerTest {
    private final SourceUrlCanonicalizer canonicalizer = new SourceUrlCanonicalizer();

    @Test
    void normalizesEquivalentYoutubeUrls() {
        String expected = "https://www.youtube.com/watch?v=YuUeNsZgmyc";
        assertEquals(expected, canonicalizer.canonicalize("https://youtu.be/YuUeNsZgmyc?t=20"));
        assertEquals(expected, canonicalizer.canonicalize("https://www.youtube.com/watch?v=YuUeNsZgmyc&list=abc"));
        assertEquals(expected, canonicalizer.canonicalize("https://youtube.com/shorts/YuUeNsZgmyc"));
    }

    @Test
    void rejectsNonYoutubeUrls() {
        assertThrows(IllegalArgumentException.class, () -> canonicalizer.canonicalize("https://example.com/video"));
    }
}
