package br.com.immersionhub.generator.desktop.translation;

import br.com.immersionhub.generator.desktop.preparation.TimedText;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TranslationChunkerTest {
    @Test
    void preservesCueBoundariesAndOrderAcrossChunks() {
        List<TranslationCue> cues = new ArrayList<>();
        for (int i = 1; i <= 40; i++) {
            cues.add(new TranslationCue(
                i,
                (i - 1) * 1000L,
                i * 1000L,
                (i - 1) * 1000L,
                i * 1000L,
                "",
                "This is a deliberately longer sentence number " + i + " for semantic chunk testing.",
                "This is a deliberately longer sentence number " + i + " for semantic chunk testing.",
                "",
                List.of(new TimedText("word", (i - 1) * 1000L, i * 1000L))
            ));
        }

        TranslationMaterial material = new TranslationMaterial(
            "id",
            "aligned",
            TranslationMaterial.SCHEMA_VERSION,
            cues,
            TranslationSource.GROQ,
            Instant.EPOCH
        );

        List<List<TranslationCue>> chunks = new TranslationChunker(new TokenEstimator())
            .chunk(material, new GroqModelInfo("small-context", 2048, 700));

        assertTrue(chunks.size() >= 2);
        List<Integer> flattened = chunks.stream()
            .flatMap(List::stream)
            .map(TranslationCue::order)
            .toList();
        assertEquals(cues.stream().map(TranslationCue::order).toList(), flattened);
        assertTrue(chunks.stream().noneMatch(List::isEmpty));
    }

    @Test
    void refusesSingleCueThatCannotFitSafely() {
        String huge = "x".repeat(20_000);
        TranslationCue cue = new TranslationCue(
            1, 0, 1000, 0, 1000, "", huge, huge, "",
            List.of(new TimedText("x", 0, 1000))
        );
        TranslationMaterial material = new TranslationMaterial(
            "id", "aligned", TranslationMaterial.SCHEMA_VERSION,
            List.of(cue), TranslationSource.GROQ, Instant.EPOCH
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> new TranslationChunker(new TokenEstimator())
                .chunk(material, new GroqModelInfo("tiny", 2048, 512))
        );
    }
}
