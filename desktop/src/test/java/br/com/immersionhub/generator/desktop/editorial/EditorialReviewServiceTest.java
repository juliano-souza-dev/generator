package br.com.immersionhub.generator.desktop.editorial;

import br.com.immersionhub.generator.desktop.preparation.TimedText;
import br.com.immersionhub.generator.desktop.translation.TranslationCue;
import br.com.immersionhub.generator.desktop.translation.TranslationMaterial;
import br.com.immersionhub.generator.desktop.translation.TranslationSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EditorialReviewServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void editApproveAndEditAgainRespectExplicitApproval() throws Exception {
        TranslationMaterial translation = translation();
        EditorialReviewService service = service(tempDir);
        EditorialMaterial material = service.loadOrCreate(translation);

        EditorialCue original = material.cues().getFirst();
        EditorialMaterial edited = service.saveDraft(
            material,
            1,
            "Human corrected line",
            "Linha corrigida"
        );

        EditorialCue draft = edited.cues().getFirst();
        assertEquals(EditorialReviewStatus.PENDING, draft.reviewStatus());
        assertEquals(original.originalEn(), draft.originalEn());
        assertEquals(original.speechStartMs(), draft.speechStartMs());
        assertEquals(original.speechEndMs(), draft.speechEndMs());
        assertTrue(draft.words().stream().noneMatch(EditorialWord::timed));
        assertEquals(1, edited.reconciliations().size());
        EditorialReconciliation firstReconciliation = edited.reconciliations().getFirst();
        assertEquals(0, firstReconciliation.preservedWords());
        assertEquals(3, firstReconciliation.insertedWords());
        assertEquals(3, firstReconciliation.removedWords());

        EditorialMaterial approved = service.approve(
            edited,
            1,
            draft.approvedEn(),
            draft.pt()
        );
        assertEquals(EditorialReviewStatus.APPROVED, approved.cues().getFirst().reviewStatus());
        assertEquals(1, approved.reconciliations().size());

        EditorialMaterial changedAgain = service.saveDraft(
            approved,
            1,
            "Human corrected line again",
            "Linha corrigida de novo"
        );
        assertEquals(EditorialReviewStatus.PENDING, changedAgain.cues().getFirst().reviewStatus());
        assertEquals(2, changedAgain.reconciliations().size());
        EditorialReconciliation secondReconciliation = changedAgain.reconciliations().getLast();
        assertEquals(3, secondReconciliation.preservedWords());
        assertEquals(1, secondReconciliation.insertedWords());
        assertEquals(0, secondReconciliation.removedWords());
    }

    @Test
    void restoreReturnsToAutomaticSuggestionAndLeavesCuePending() throws Exception {
        TranslationMaterial translation = translation();
        EditorialReviewService service = service(tempDir);
        EditorialMaterial material = service.loadOrCreate(translation);

        material = service.approve(
            material,
            1,
            "Different English",
            "Português diferente"
        );
        material = service.restoreSuggestion(material, translation, 1);

        EditorialCue restored = material.cues().getFirst();
        assertEquals(translation.cues().getFirst().approvedEn(), restored.approvedEn());
        assertEquals(translation.cues().getFirst().pt(), restored.pt());
        assertEquals(EditorialReviewStatus.PENDING, restored.reviewStatus());
        assertTrue(restored.words().stream().allMatch(EditorialWord::timed));
        assertEquals(
            EditorialReconciliationReason.RESTORE_TRANSLATION_SUGGESTION,
            material.reconciliations().getLast().reason()
        );
    }

    @Test
    void reopeningPreservesReviewedContentAndCurrentCue() throws Exception {
        TranslationMaterial translation = translation();
        EditorialReviewService firstSession = service(tempDir);
        EditorialMaterial material = firstSession.loadOrCreate(translation);

        material = firstSession.approve(material, 1, "Approved one", "Aprovada um");
        firstSession.saveCursor(material, 2);

        EditorialReviewService secondSession = service(tempDir);
        EditorialMaterial reopened = secondSession.loadOrCreate(translation);

        assertEquals("Approved one", reopened.cues().getFirst().approvedEn());
        assertEquals("Aprovada um", reopened.cues().getFirst().pt());
        assertEquals(EditorialReviewStatus.APPROVED, reopened.cues().getFirst().reviewStatus());
        assertEquals(2, secondSession.loadCursor(reopened));
        assertEquals(2, secondSession.nextPending(reopened, 1).orElseThrow());
    }

    @Test
    void protectedCueFieldsAreRejectedWhenPersistedFileIsTampered() throws Exception {
        TranslationMaterial translation = translation();
        EditorialReviewService firstSession = service(tempDir);
        firstSession.loadOrCreate(translation);

        Path materialFile = tempDir.resolve("editorial-material.json");
        String tampered = Files.readString(materialFile)
            .replace("Machine original one", "Tampered original");
        Files.writeString(materialFile, tampered);

        EditorialReviewService secondSession = service(tempDir);
        assertThrows(Exception.class, () -> secondSession.loadOrCreate(translation));
    }

    private static EditorialReviewService service(Path root) {
        return new EditorialReviewService(
            new FileEditorialMaterialRepository(root),
            new EditorialReviewCursorRepository(root)
        );
    }

    private static TranslationMaterial translation() {
        return new TranslationMaterial(
            "translation-review",
            "aligned-review",
            TranslationMaterial.SCHEMA_VERSION,
            List.of(
                new TranslationCue(
                    1,
                    100,
                    900,
                    100,
                    900,
                    "",
                    "Machine original one",
                    "Approved base one",
                    "Tradução um",
                    List.of(
                        new TimedText("Approved", 100, 400, 0.95),
                        new TimedText("base", 410, 650, 0.91),
                        new TimedText("one", 660, 880, 0.89)
                    )
                ),
                new TranslationCue(
                    2,
                    1000,
                    1800,
                    1000,
                    1800,
                    "",
                    "Machine original two",
                    "Approved base two",
                    "Tradução dois",
                    List.of(
                        new TimedText("Approved", 1000, 1250, 0.94),
                        new TimedText("base", 1260, 1500, 0.90),
                        new TimedText("two", 1510, 1780, 0.88)
                    )
                )
            ),
            TranslationSource.GROQ,
            Instant.parse("2026-09-19T12:00:00Z")
        );
    }
}
