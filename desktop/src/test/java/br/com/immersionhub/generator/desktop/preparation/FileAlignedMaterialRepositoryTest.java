package br.com.immersionhub.generator.desktop.preparation;

import br.com.immersionhub.generator.desktop.model.MediaCut;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FileAlignedMaterialRepositoryTest {
    @Test
    void persistsAndReloadsAlignedWords() throws Exception {
        Path dir = Files.createTempDirectory("aligned-repo");
        Path source = Files.writeString(dir.resolve("source.mp4"), "s");
        Path cut = Files.writeString(dir.resolve("cut.mp4"), "c");
        Path audio = Files.writeString(dir.resolve("audio.wav"), "a");
        MediaCut mediaCut = new MediaCut("source", source, cut, 0, 1000, 1000, Instant.EPOCH);
        PreparedMaterial prepared = new PreparedMaterial(
            "prepared", "pipeline", "asr", mediaCut, audio,
            new AsrResult("en", "hello world", List.of(new TimedText("hello world", 0, 1000)), List.of()),
            Instant.EPOCH
        );
        AlignedMaterial material = new AlignedMaterial(
            "aligned", prepared, "dtw-1",
            List.of(new TimedText("hello", 0, 400), new TimedText("world", 400, 1000)),
            Instant.EPOCH
        );

        FileAlignedMaterialRepository repository = new FileAlignedMaterialRepository(dir.resolve("aligned"));
        repository.save(material);
        AlignedMaterial loaded = repository.load("aligned", prepared).orElseThrow();

        assertEquals(2, loaded.words().size());
        assertEquals("world", loaded.words().getLast().text());
        assertEquals("dtw-1", loaded.alignerVersion());
    }
}
