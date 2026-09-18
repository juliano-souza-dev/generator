package br.com.immersionhub.generator.desktop.preparation;

import br.com.immersionhub.generator.desktop.model.MediaCut;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WordAlignmentServiceTest {
    @Test
    void reusesAlignedSnapshotWithoutRunningAlignerAgain() throws Exception {
        Path dir = Files.createTempDirectory("alignment-service");
        Path source = Files.writeString(dir.resolve("source.mp4"), "s");
        Path cut = Files.writeString(dir.resolve("cut.mp4"), "c");
        Path audio = Files.writeString(dir.resolve("audio.wav"), "a");
        MediaCut mediaCut = new MediaCut("source", source, cut, 0, 1000, 1000, Instant.EPOCH);
        PreparedMaterial prepared = new PreparedMaterial(
            "prepared", "pipeline", "asr", mediaCut, audio,
            new AsrResult("en", "hello world", List.of(new TimedText("hello world", 0, 1000)), List.of()),
            Instant.EPOCH
        );

        Map<String, AlignedMaterial> cache = new HashMap<>();
        AlignedMaterialRepository repository = new AlignedMaterialRepository() {
            public Optional<AlignedMaterial> load(String id, PreparedMaterial ignored) { return Optional.ofNullable(cache.get(id)); }
            public void save(AlignedMaterial material) { cache.put(material.id(), material); }
        };
        int[] calls = {0};
        WordAligner aligner = new WordAligner() {
            public List<TimedText> align(Path a, AsrResult t, long d) {
                calls[0]++;
                return List.of(new TimedText("hello", 0, 400), new TimedText("world", 400, 1000));
            }
            public String version() { return "dtw-1"; }
        };

        WordAlignmentService service = new WordAlignmentService(aligner, repository);
        assertEquals(service.align(prepared).id(), service.align(prepared).id());
        assertEquals(1, calls[0]);
    }

    @Test
    void failedAlignmentIsNotPublished() throws Exception {
        Path dir = Files.createTempDirectory("alignment-fail");
        Path source = Files.writeString(dir.resolve("source.mp4"), "s");
        Path cut = Files.writeString(dir.resolve("cut.mp4"), "c");
        Path audio = Files.writeString(dir.resolve("audio.wav"), "a");
        MediaCut mediaCut = new MediaCut("source", source, cut, 0, 1000, 1000, Instant.EPOCH);
        PreparedMaterial prepared = new PreparedMaterial(
            "prepared", "pipeline", "asr", mediaCut, audio,
            new AsrResult("en", "hello", List.of(new TimedText("hello", 0, 1000)), List.of()),
            Instant.EPOCH
        );
        int[] saves = {0};
        AlignedMaterialRepository repository = new AlignedMaterialRepository() {
            public Optional<AlignedMaterial> load(String id, PreparedMaterial ignored) { return Optional.empty(); }
            public void save(AlignedMaterial material) { saves[0]++; }
        };
        WordAligner aligner = new WordAligner() {
            public List<TimedText> align(Path a, AsrResult t, long d) { throw new IllegalStateException("alignment failed"); }
            public String version() { return "dtw-1"; }
        };

        assertThrows(IllegalStateException.class, () -> new WordAlignmentService(aligner, repository).align(prepared));
        assertEquals(0, saves[0]);
    }
}
