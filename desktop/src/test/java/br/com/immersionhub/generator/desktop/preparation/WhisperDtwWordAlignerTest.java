package br.com.immersionhub.generator.desktop.preparation;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class WhisperDtwWordAlignerTest {
    private WhisperDtwWordAligner aligner(Path dir) throws Exception {
        Path model = Files.writeString(dir.resolve("model.bin"), "model");
        return new WhisperDtwWordAligner(() -> dir.resolve("unused.exe"), model, "base.en", "dtw-test");
    }

    @Test
    void parsesWordSegmentsAndPreservesAcceptedTranscript() throws Exception {
        Path dir = Files.createTempDirectory("dtw-json");
        Path json = dir.resolve("aligned.json");
        Files.writeString(json, """
            {
              "transcription": [
                {"offsets":{"from":100,"to":240},"text":"What"},
                {"offsets":{"from":240,"to":360},"text":"are"},
                {"offsets":{"from":360,"to":600},"text":"you?"}
              ]
            }
            """);

        WhisperDtwWordAligner aligner = aligner(dir);
        List<TimedText> candidates = aligner.parseWordSegments(json, 1000);
        List<TimedText> result = aligner.preserveTranscript("What are you?", candidates);

        assertEquals(List.of("What", "are", "you?"), result.stream().map(TimedText::text).toList());
        assertEquals(100, result.getFirst().startMs());
        assertEquals(600, result.getLast().endMs());
    }

    @Test
    void rejectsAlignmentThatChangesTranscript() throws Exception {
        Path dir = Files.createTempDirectory("dtw-mismatch");
        WhisperDtwWordAligner aligner = aligner(dir);
        assertThrows(IOException.class, () -> aligner.preserveTranscript(
            "What are you?",
            List.of(new TimedText("Where", 0, 100), new TimedText("are", 100, 200), new TimedText("you?", 200, 300))
        ));
    }
}
