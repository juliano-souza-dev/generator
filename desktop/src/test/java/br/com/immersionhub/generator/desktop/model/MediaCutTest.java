package br.com.immersionhub.generator.desktop.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MediaCutTest {
    @Test
    void requiresDistinctExistingOutputAndConsistentDuration(@TempDir Path tempDir) throws Exception {
        Path source = Files.writeString(tempDir.resolve("source.mp4"), "source");
        Path output = Files.writeString(tempDir.resolve("cut.mp4"), "cut");

        MediaCut cut = new MediaCut(
            "source-1", source, output, 1_000, 4_000, 3_000, Instant.now()
        );

        assertEquals(3_000, cut.durationMs());
        assertThrows(IllegalArgumentException.class, () ->
            new MediaCut("source-1", source, source, 1_000, 4_000, 3_000, Instant.now())
        );
        assertThrows(IllegalArgumentException.class, () ->
            new MediaCut("source-1", source, output, 4_000, 1_000, -3_000, Instant.now())
        );
        assertThrows(IllegalArgumentException.class, () ->
            new MediaCut("source-1", source, output, 1_000, 4_000, 2_999, Instant.now())
        );
    }

    @Test
    void rejectsMissingOutput(@TempDir Path tempDir) throws Exception {
        Path source = Files.writeString(tempDir.resolve("source.mp4"), "source");
        Path missing = tempDir.resolve("missing.mp4");

        assertThrows(IllegalArgumentException.class, () ->
            new MediaCut("source-1", source, missing, 0, 1_000, 1_000, Instant.now())
        );
    }
}
