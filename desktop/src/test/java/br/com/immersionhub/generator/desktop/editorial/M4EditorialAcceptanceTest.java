package br.com.immersionhub.generator.desktop.editorial;

import br.com.immersionhub.generator.desktop.preparation.TimedText;
import br.com.immersionhub.generator.desktop.translation.TranslationCue;
import br.com.immersionhub.generator.desktop.translation.TranslationMaterial;
import br.com.immersionhub.generator.desktop.translation.TranslationSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class M4EditorialAcceptanceTest {
    @TempDir
    Path tempDir;

    @Test
    void fullEditorialJourneySurvivesReopenWithoutReprocessingTranslation() throws Exception {
        TranslationMaterial translation = translation();
        FileEditorialMaterialRepository repository = new FileEditorialMaterialRepository(tempDir);

        EditorialReviewService cueReview = new EditorialReviewService(
            repository,
            new EditorialReviewCursorRepository(tempDir)
        );

        EditorialMaterial material = cueReview.loadOrCreate(translation);
        assertEquals(translation.id(), material.translationMaterialId());
        assertFalse(material.cuesApproved());

        material = cueReview.approve(
            material,
            1,
            "I really do like this",
            "Eu realmente gosto disso"
        );
        EditorialWord inserted = material.cues().getFirst().words().get(2);
        assertEquals("do", inserted.approvedEn());
        assertFalse(inserted.timed());
        assertNull(inserted.confidence());

        material = cueReview.approve(
            material,
            2,
            "It works",
            "Isso funciona"
        );
        assertTrue(material.cuesApproved());

        WordReviewService words = new WordReviewService(
            repository,
            new WordReviewCursorRepository(tempDir)
        );

        WordReviewMutation grouped = words.groupWithNext(
            material,
            new WordReviewPosition(1, 2)
        );
        material = grouped.material();

        assertTrue(words.isGrouped(material, grouped.position()));
        assertEquals(2, words.unitSize(material, grouped.position()));

        WordReviewPosition position = words.loadCursor(material);
        while (!material.fullyApproved()) {
            String english = words.unitEnglish(material, position);
            String pt = words.unitPt(material, position);
            if (pt.isBlank()) {
                pt = "pt-" + words.ordinal(material, position);
            }

            material = words.approve(material, position, english, pt);

            Optional<WordReviewPosition> pending = words.nextPending(material, position);
            if (pending.isEmpty()) break;
            position = pending.get();
        }

        assertTrue(material.fullyApproved());
        assertTrue(material.cuesApproved());
        assertEquals(translation.id(), material.translationMaterialId());
        assertFalse(material.reconciliations().isEmpty());

        EditorialMaterial reopened = repository.load(translation).orElseThrow();

        assertEquals(material.id(), reopened.id());
        assertTrue(reopened.fullyApproved());
        assertEquals(translation.id(), reopened.translationMaterialId());

        EditorialCue reopenedFirstCue = reopened.cues().getFirst();
        assertEquals("I really do like this", reopenedFirstCue.approvedEn());
        assertEquals("Eu realmente gosto disso", reopenedFirstCue.pt());
        assertFalse(reopenedFirstCue.words().get(2).timed());

        String groupId = reopenedFirstCue.words().get(1).semanticGroupId();
        assertFalse(groupId.isBlank());
        assertEquals(groupId, reopenedFirstCue.words().get(2).semanticGroupId());
        assertEquals(SemanticGroupRole.LEAD, reopenedFirstCue.words().get(1).semanticGroupRole());
        assertEquals(SemanticGroupRole.MEMBER, reopenedFirstCue.words().get(2).semanticGroupRole());

        EditorialMaterial secondReopen = repository.load(translation).orElseThrow();
        assertEquals(reopened.id(), secondReopen.id());
        assertEquals(reopened, secondReopen);
    }

    private static TranslationMaterial translation() {
        return new TranslationMaterial(
            "translation-m4-acceptance",
            "aligned-m4-acceptance",
            TranslationMaterial.SCHEMA_VERSION,
            List.of(
                new TranslationCue(
                    1,
                    100,
                    1100,
                    100,
                    1100,
                    "",
                    "I really like this",
                    "I really like this",
                    "Eu gosto muito disso",
                    List.of(
                        new TimedText("I", 100, 220, 0.96),
                        new TimedText("really", 230, 430, 0.94),
                        new TimedText("like", 440, 720, 0.93),
                        new TimedText("this", 730, 1050, 0.92)
                    )
                ),
                new TranslationCue(
                    2,
                    1200,
                    1900,
                    1200,
                    1900,
                    "",
                    "It works",
                    "It works",
                    "Isso funciona",
                    List.of(
                        new TimedText("It", 1200, 1450, 0.95),
                        new TimedText("works", 1460, 1860, 0.94)
                    )
                )
            ),
            TranslationSource.GROQ,
            Instant.parse("2026-09-19T14:00:00Z")
        );
    }
}
