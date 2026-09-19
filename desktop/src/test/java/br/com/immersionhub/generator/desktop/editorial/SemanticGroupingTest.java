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

class SemanticGroupingTest {
    @TempDir
    Path tempDir;

    @Test
    void groupWithNextCreatesSinglePendingUnitAndPreservesOrder() throws Exception {
        TranslationMaterial translation = translation();
        FileEditorialMaterialRepository repository = new FileEditorialMaterialRepository(tempDir);
        EditorialMaterial material = reviewedMaterial(translation);
        repository.save(material);
        WordReviewService service = service(repository);

        WordReviewMutation mutation = service.groupWithNext(
            material,
            new WordReviewPosition(1, 2)
        );
        material = mutation.material();

        List<EditorialWord> words = material.cues().getFirst().words();
        assertEquals(List.of("I", "really", "like", "this"),
            words.stream().map(EditorialWord::approvedEn).toList());

        EditorialWord lead = words.get(1);
        EditorialWord member = words.get(2);

        assertEquals(SemanticGroupRole.LEAD, lead.semanticGroupRole());
        assertEquals(SemanticGroupRole.MEMBER, member.semanticGroupRole());
        assertEquals(lead.semanticGroupId(), member.semanticGroupId());
        assertFalse(lead.semanticGroupId().isBlank());
        assertEquals("realmente gosto", lead.pt());
        assertEquals("", member.pt());
        assertEquals("realmente", lead.individualPt());
        assertEquals("gosto", member.individualPt());
        assertEquals(EditorialReviewStatus.PENDING, lead.reviewStatus());
        assertEquals(EditorialReviewStatus.PENDING, member.reviewStatus());
        assertEquals(3, service.totalCount(material));
        assertEquals(new WordReviewPosition(1, 2), mutation.position());
    }

    @Test
    void ungroupRestoresKnownIndividualTranslationsAndLeavesUnitsPending() throws Exception {
        TranslationMaterial translation = translation();
        FileEditorialMaterialRepository repository = new FileEditorialMaterialRepository(tempDir);
        EditorialMaterial material = reviewedMaterial(translation);
        repository.save(material);
        WordReviewService service = service(repository);

        material = service.groupWithNext(material, new WordReviewPosition(1, 2)).material();
        material = service.approve(
            material,
            new WordReviewPosition(1, 2),
            "really like",
            "gosto muito"
        );

        WordReviewMutation ungrouped = service.ungroup(
            material,
            new WordReviewPosition(1, 2)
        );

        List<EditorialWord> words = ungrouped.material().cues().getFirst().words();
        assertEquals("realmente", words.get(1).pt());
        assertEquals("gosto", words.get(2).pt());
        assertEquals("", words.get(1).semanticGroupId());
        assertEquals("", words.get(2).semanticGroupId());
        assertEquals(SemanticGroupRole.NONE, words.get(1).semanticGroupRole());
        assertEquals(SemanticGroupRole.NONE, words.get(2).semanticGroupRole());
        assertEquals(EditorialReviewStatus.PENDING, words.get(1).reviewStatus());
        assertEquals(EditorialReviewStatus.PENDING, words.get(2).reviewStatus());
        assertEquals(4, service.totalCount(ungrouped.material()));
    }

    @Test
    void unknownIndividualTranslationRemainsUnknownAfterRoundTrip() throws Exception {
        TranslationMaterial translation = translation();
        FileEditorialMaterialRepository repository = new FileEditorialMaterialRepository(tempDir);
        EditorialMaterial material = reviewedMaterial(translation);

        List<EditorialCue> cues = new ArrayList<>(material.cues());
        EditorialCue cue = cues.getFirst();
        List<EditorialWord> words = new ArrayList<>(cue.words());
        EditorialWord unknown = words.get(2);
        words.set(2, new EditorialWord(
            unknown.index(),
            unknown.originalEn(),
            unknown.approvedEn(),
            "",
            "",
            unknown.startMs(),
            unknown.endMs(),
            unknown.confidence(),
            "",
            SemanticGroupRole.NONE,
            EditorialReviewStatus.PENDING
        ));
        cues.set(0, cue.withReviewAndWords(
            cue.approvedEn(), cue.pt(), cue.reviewStatus(), words
        ));
        material = material.withCues(cues);
        repository.save(material);

        WordReviewService service = service(repository);
        material = service.groupWithNext(material, new WordReviewPosition(1, 2)).material();

        assertEquals("", service.unitPt(material, new WordReviewPosition(1, 2)));

        material = service.ungroup(material, new WordReviewPosition(1, 2)).material();
        assertEquals("", material.cues().getFirst().words().get(2).pt());
        assertEquals(EditorialReviewStatus.PENDING,
            material.cues().getFirst().words().get(2).reviewStatus());
    }

    @Test
    void groupingExistingGroupWithPreviousMergesWholeUnits() throws Exception {
        TranslationMaterial translation = translation();
        FileEditorialMaterialRepository repository = new FileEditorialMaterialRepository(tempDir);
        EditorialMaterial material = reviewedMaterial(translation);
        repository.save(material);
        WordReviewService service = service(repository);

        material = service.groupWithNext(material, new WordReviewPosition(1, 2)).material();
        material = service.groupWithPrevious(material, new WordReviewPosition(1, 2)).material();

        List<EditorialWord> words = material.cues().getFirst().words();
        assertEquals(SemanticGroupRole.LEAD, words.get(0).semanticGroupRole());
        assertEquals(SemanticGroupRole.MEMBER, words.get(1).semanticGroupRole());
        assertEquals(SemanticGroupRole.MEMBER, words.get(2).semanticGroupRole());
        assertEquals(words.get(0).semanticGroupId(), words.get(2).semanticGroupId());
        assertEquals(SemanticGroupRole.NONE, words.get(3).semanticGroupRole());
        assertEquals(2, service.totalCount(material));
    }

    @Test
    void groupedMaterialPersistsAndReloadsWithIndividualBackups() throws Exception {
        TranslationMaterial translation = translation();
        FileEditorialMaterialRepository repository = new FileEditorialMaterialRepository(tempDir);
        EditorialMaterial material = reviewedMaterial(translation);
        repository.save(material);
        WordReviewService service = service(repository);

        material = service.groupWithNext(material, new WordReviewPosition(1, 2)).material();

        EditorialMaterial reopened = repository.load(translation).orElseThrow();
        assertEquals(EditorialMaterial.SCHEMA_VERSION, reopened.schemaVersion());
        assertEquals("realmente", reopened.cues().getFirst().words().get(1).individualPt());
        assertEquals("gosto", reopened.cues().getFirst().words().get(2).individualPt());
        assertEquals(3, service.totalCount(reopened));
    }

    @Test
    void groupingUpgradesLegacyEditorialMaterialWithoutLosingReview() throws Exception {
        TranslationMaterial translation = translation();
        EditorialMaterial current = reviewedMaterial(translation);

        String legacyId = EditorialIds.materialId(
            current.translationMaterialId(),
            "1.0",
            current.cues(),
            current.reconciliations()
        );
        EditorialMaterial legacy = new EditorialMaterial(
            legacyId,
            current.translationMaterialId(),
            "1.0",
            current.cues(),
            current.reconciliations(),
            current.createdAt()
        );

        FileEditorialMaterialRepository repository = new FileEditorialMaterialRepository(tempDir);
        repository.save(legacy);
        WordReviewService service = service(repository);

        EditorialMaterial grouped = service.groupWithNext(
            legacy,
            new WordReviewPosition(1, 1)
        ).material();

        assertEquals("1.1", grouped.schemaVersion());
        assertEquals("eu", grouped.cues().getFirst().words().get(0).individualPt());
        assertEquals("realmente", grouped.cues().getFirst().words().get(1).individualPt());
    }

    private WordReviewService service(FileEditorialMaterialRepository repository) {
        return new WordReviewService(repository, new WordReviewCursorRepository(tempDir));
    }

    private static EditorialMaterial reviewedMaterial(TranslationMaterial translation) {
        EditorialMaterial base = EditorialMaterialFactory.fromTranslation(translation);
        EditorialCue cue = base.cues().getFirst();
        List<EditorialWord> words = List.of(
            reviewed(cue.words().get(0), "eu"),
            reviewed(cue.words().get(1), "realmente"),
            reviewed(cue.words().get(2), "gosto"),
            reviewed(cue.words().get(3), "disso")
        );
        EditorialCue approvedCue = cue.withReviewAndWords(
            cue.approvedEn(),
            cue.pt(),
            EditorialReviewStatus.APPROVED,
            words
        );
        return base.withCues(List.of(approvedCue));
    }

    private static EditorialWord reviewed(EditorialWord word, String pt) {
        return new EditorialWord(
            word.index(),
            word.originalEn(),
            word.approvedEn(),
            pt,
            pt,
            word.startMs(),
            word.endMs(),
            word.confidence(),
            "",
            SemanticGroupRole.NONE,
            EditorialReviewStatus.APPROVED
        );
    }

    private static TranslationMaterial translation() {
        return new TranslationMaterial(
            "translation-groups",
            "aligned-groups",
            TranslationMaterial.SCHEMA_VERSION,
            List.of(new TranslationCue(
                1,
                100,
                1000,
                100,
                1000,
                "",
                "I really like this",
                "I really like this",
                "Eu gosto muito disso",
                List.of(
                    new TimedText("I", 100, 220, 0.95),
                    new TimedText("really", 230, 420, 0.93),
                    new TimedText("like", 430, 680, 0.92),
                    new TimedText("this", 690, 950, 0.91)
                )
            )),
            TranslationSource.GROQ,
            Instant.parse("2026-09-19T12:00:00Z")
        );
    }
}
