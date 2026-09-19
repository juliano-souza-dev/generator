package br.com.immersionhub.generator.desktop.editorial;

import br.com.immersionhub.generator.desktop.preparation.TimedText;
import br.com.immersionhub.generator.desktop.translation.TranslationCue;
import br.com.immersionhub.generator.desktop.translation.TranslationMaterial;
import br.com.immersionhub.generator.desktop.translation.TranslationSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WordReviewServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void partialReviewPersistsAndResumesExactWord() throws Exception {
        TranslationMaterial translation = translation();
        FileEditorialMaterialRepository repository = new FileEditorialMaterialRepository(tempDir);
        EditorialMaterial material = approvedCueMaterial(translation);
        repository.save(material);

        WordReviewService first = service(repository);
        WordReviewPosition firstWord = new WordReviewPosition(1, 1);
        WordReviewPosition secondWord = new WordReviewPosition(1, 2);

        material = first.approve(material, firstWord, "I", "eu");
        first.saveCursor(material, secondWord);

        EditorialMaterial reopened = repository.load(translation).orElseThrow();
        WordReviewService second = service(repository);

        assertEquals(EditorialReviewStatus.APPROVED, reopened.cues().getFirst().words().getFirst().reviewStatus());
        assertEquals("eu", reopened.cues().getFirst().words().getFirst().pt());
        assertEquals(secondWord, second.loadCursor(reopened));
        assertEquals(1, second.approvedCount(reopened));
        assertEquals(3, second.totalCount(reopened));
    }

    @Test
    void editingApprovedWordReturnsOnlyThatWordToPending() throws Exception {
        TranslationMaterial translation = translation();
        FileEditorialMaterialRepository repository = new FileEditorialMaterialRepository(tempDir);
        EditorialMaterial material = approvedCueMaterial(translation);
        repository.save(material);
        WordReviewService service = service(repository);

        WordReviewPosition first = new WordReviewPosition(1, 1);
        WordReviewPosition second = new WordReviewPosition(1, 2);

        material = service.approve(material, first, "I", "eu");
        material = service.approve(material, second, "really", "realmente");
        material = service.saveDraft(material, first, "I", "eu mesmo");

        assertEquals(EditorialReviewStatus.PENDING, material.cues().getFirst().words().getFirst().reviewStatus());
        assertEquals(EditorialReviewStatus.APPROVED, material.cues().getFirst().words().get(1).reviewStatus());
        assertEquals(new WordReviewPosition(1, 3), service.nextPending(material, second).orElseThrow());
    }

    @Test
    void structuralEnglishChangeMustReturnToCueReview() throws Exception {
        TranslationMaterial translation = translation();
        FileEditorialMaterialRepository repository = new FileEditorialMaterialRepository(tempDir);
        EditorialMaterial material = approvedCueMaterial(translation);
        repository.save(material);
        WordReviewService service = service(repository);

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> service.saveDraft(
                material,
                new WordReviewPosition(1, 3),
                "love",
                "gosto"
            )
        );

        assertTrue(error.getMessage().contains("revisão da cue"));
    }

    @Test
    void compatibleCaseChangeUpdatesCueWithoutBreakingStructure() throws Exception {
        TranslationMaterial translation = translation();
        FileEditorialMaterialRepository repository = new FileEditorialMaterialRepository(tempDir);
        EditorialMaterial material = approvedCueMaterial(translation);
        repository.save(material);
        WordReviewService service = service(repository);

        material = service.approve(
            material,
            new WordReviewPosition(1, 2),
            "REALLY",
            "realmente"
        );

        assertEquals("I REALLY like", material.cues().getFirst().approvedEn());
        assertEquals("REALLY", material.cues().getFirst().words().get(1).approvedEn());
        assertEquals(EditorialReviewStatus.APPROVED, material.cues().getFirst().reviewStatus());
    }

    @Test
    void wordReviewRequiresApprovedCues() {
        EditorialMaterial pending = EditorialMaterialFactory.fromTranslation(translation());
        WordReviewService service = service(new FileEditorialMaterialRepository(tempDir));

        assertThrows(IllegalStateException.class, () -> service.loadCursor(pending));
    }

    private WordReviewService service(FileEditorialMaterialRepository repository) {
        return new WordReviewService(repository, new WordReviewCursorRepository(tempDir));
    }

    private static EditorialMaterial approvedCueMaterial(TranslationMaterial translation) {
        EditorialMaterial pending = EditorialMaterialFactory.fromTranslation(translation);
        List<EditorialCue> cues = new ArrayList<>();

        for (EditorialCue cue : pending.cues()) {
            cues.add(cue.withReview(
                cue.approvedEn(),
                cue.pt(),
                EditorialReviewStatus.APPROVED
            ));
        }
        return pending.withCues(cues);
    }

    private static TranslationMaterial translation() {
        return new TranslationMaterial(
            "translation-wbw",
            "aligned-wbw",
            TranslationMaterial.SCHEMA_VERSION,
            List.of(new TranslationCue(
                1,
                100,
                900,
                100,
                900,
                "",
                "I really like",
                "I really like",
                "Eu gosto muito",
                List.of(
                    new TimedText("I", 100, 220, 0.95),
                    new TimedText("really", 230, 520, 0.92),
                    new TimedText("like", 530, 850, 0.91)
                )
            )),
            TranslationSource.GROQ,
            Instant.parse("2026-09-19T12:00:00Z")
        );
    }
}
