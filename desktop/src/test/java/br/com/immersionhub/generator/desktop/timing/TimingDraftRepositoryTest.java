package br.com.immersionhub.generator.desktop.timing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimingDraftRepositoryTest {
    @Test
    void restoresAutosavedTimingForSameSource(@TempDir Path tempDir) throws Exception {
        TimingDraftRepository repository = new TimingDraftRepository(tempDir);
        repository.save("source-a", 1_250, 8_750);

        assertEquals(
            new TimingRange(1_250, 8_750),
            repository.load("source-a", 10_000).orElseThrow()
        );
        assertTrue(repository.load("source-b", 10_000).isEmpty());
    }

    @Test
    void ignoresCorruptOrOutOfBoundsDraft(@TempDir Path tempDir) throws Exception {
        TimingDraftRepository repository = new TimingDraftRepository(tempDir);
        repository.save("source-a", 1_000, 9_000);

        assertTrue(repository.load("source-a", 8_000).isEmpty());

        Files.writeString(repository.pathFor("source-a"), "{broken-json");
        assertTrue(repository.load("source-a", 10_000).isEmpty());
    }
}
