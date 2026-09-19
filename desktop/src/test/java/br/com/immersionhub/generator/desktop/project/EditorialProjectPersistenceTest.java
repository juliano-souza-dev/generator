package br.com.immersionhub.generator.desktop.project;

import br.com.immersionhub.generator.desktop.editorial.EditorialCue;
import br.com.immersionhub.generator.desktop.editorial.EditorialMaterial;
import br.com.immersionhub.generator.desktop.editorial.EditorialMaterialFactory;
import br.com.immersionhub.generator.desktop.editorial.EditorialReviewStatus;
import br.com.immersionhub.generator.desktop.editorial.EditorialWord;
import br.com.immersionhub.generator.desktop.editorial.FileEditorialMaterialRepository;
import br.com.immersionhub.generator.desktop.editorial.SemanticGroupRole;
import br.com.immersionhub.generator.desktop.editorial.WordReviewPosition;
import br.com.immersionhub.generator.desktop.model.MediaCut;
import br.com.immersionhub.generator.desktop.model.SourceMedia;
import br.com.immersionhub.generator.desktop.preparation.AlignedMaterial;
import br.com.immersionhub.generator.desktop.preparation.AsrResult;
import br.com.immersionhub.generator.desktop.preparation.PreparedMaterial;
import br.com.immersionhub.generator.desktop.preparation.TimedText;
import br.com.immersionhub.generator.desktop.preparation.TimingSource;
import br.com.immersionhub.generator.desktop.translation.TranslationCue;
import br.com.immersionhub.generator.desktop.translation.TranslationMaterial;
import br.com.immersionhub.generator.desktop.translation.TranslationSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EditorialProjectPersistenceTest {
    @TempDir
    Path tempDir;

    @Test
    void persistsPartialCueReviewWithDependencySnapshotAndCursor() throws Exception {
        Fixture fixture = fixture();
        EditorialMaterial material = EditorialMaterialFactory.fromTranslation(fixture.translation());
        EditorialCue first = material.cues().getFirst().withReview(
            material.cues().getFirst().approvedEn(),
            "PT revisado",
            EditorialReviewStatus.APPROVED
        );
        material = material.withCues(List.of(first, material.cues().get(1)));

        ProjectState state = fixture.project().withEditorialProgress(material, 2, null);
        ProjectState loaded = saveAndReload(state);

        assertEquals(EditorialProjectStatus.IN_PROGRESS, loaded.editorialStatus());
        assertEquals(EditorialProjectPhase.CUE_REVIEW, loaded.editorialPhase());
        assertEquals(material.id(), loaded.editorialMaterialId());
        assertEquals(fixture.translation().id(), loaded.editorialTranslationMaterialId());
        assertEquals(material.schemaVersion(), loaded.editorialSchemaVersion());
        assertEquals(2, loaded.editorialCueOrder());
        assertNull(loaded.editorialWordCueOrder());
        assertNotNull(loaded.editorialUpdatedAt());
        assertTrue(loaded.translationCompleted());
    }

    @Test
    void persistsPartialWordReviewAndExactWordCursor() throws Exception {
        Fixture fixture = fixture();
        EditorialMaterial material = approveAllCues(
            EditorialMaterialFactory.fromTranslation(fixture.translation())
        );

        EditorialCue firstCue = material.cues().getFirst();
        List<EditorialWord> words = new ArrayList<>(firstCue.words());
        EditorialWord firstWord = words.getFirst();
        words.set(0, firstWord.withReview(firstWord.approvedEn(), "um", EditorialReviewStatus.APPROVED));
        material = replaceCue(
            material,
            firstCue.withReviewAndWords(
                firstCue.approvedEn(),
                firstCue.pt(),
                firstCue.reviewStatus(),
                words
            )
        );

        WordReviewPosition cursor = new WordReviewPosition(1, 2);
        ProjectState state = fixture.project().withEditorialProgress(material, 1, cursor);
        ProjectState loaded = saveAndReload(state);

        assertEquals(EditorialProjectStatus.IN_PROGRESS, loaded.editorialStatus());
        assertEquals(EditorialProjectPhase.WORD_REVIEW, loaded.editorialPhase());
        assertEquals(cursor, loaded.editorialWordPosition().orElseThrow());
        assertFalse(loaded.completedStages().contains(ProjectStage.EDITORIAL_REVIEW));
    }

    @Test
    void completedEditorialReviewMarksStageCompleted() throws Exception {
        Fixture fixture = fixture();
        EditorialMaterial material = approveAllWords(
            approveAllCues(EditorialMaterialFactory.fromTranslation(fixture.translation()))
        );

        ProjectState state = fixture.project().withEditorialProgress(
            material,
            material.cues().size(),
            new WordReviewPosition(2, material.cues().get(1).words().size())
        );

        assertEquals(EditorialProjectStatus.COMPLETED, state.editorialStatus());
        assertEquals(EditorialProjectPhase.COMPLETE, state.editorialPhase());
        assertEquals(ProjectStageStatus.COMPLETED, state.stageStatus(ProjectStage.EDITORIAL_REVIEW));
        assertTrue(state.completedStages().contains(ProjectStage.EDITORIAL_REVIEW));
    }

    @Test
    void invalidEditorialStateKeepsTranslationAndRollsBackOnlyEditorial() throws Exception {
        Fixture fixture = fixture();
        EditorialMaterial material = EditorialMaterialFactory.fromTranslation(fixture.translation());
        ProjectState inProgress = fixture.project().withEditorialProgress(material, 1, null);

        ProjectState invalid = inProgress.withEditorialInvalid();

        assertEquals(EditorialProjectStatus.INVALID, invalid.editorialStatus());
        assertEquals(ProjectStage.EDITORIAL_REVIEW, invalid.minimumResumeStage());
        assertEquals(ProjectStageStatus.NEEDS_REPROCESSING, invalid.stageStatus(ProjectStage.EDITORIAL_REVIEW));
        assertTrue(invalid.translationCompleted());
        assertEquals(fixture.translation().id(), invalid.translationMaterialId());
        assertTrue(invalid.completedStages().contains(ProjectStage.TRANSLATION));
        assertFalse(invalid.completedStages().contains(ProjectStage.EDITORIAL_REVIEW));
        assertTrue(invalid.recoveryMessage().contains("etapas anteriores"));
    }

    @Test
    void groupedEditorialArtifactAndProjectSnapshotSurviveReopen() throws Exception {
        Fixture fixture = fixture();
        EditorialMaterial material = approveAllCues(
            EditorialMaterialFactory.fromTranslation(fixture.translation())
        );

        EditorialCue first = material.cues().getFirst();
        List<EditorialWord> words = new ArrayList<>(first.words());
        EditorialWord one = words.get(0);
        EditorialWord two = words.get(1);
        String group = "group-resume";

        words.set(0, new EditorialWord(
            one.index(), one.originalEn(), one.approvedEn(),
            "unidade", "um",
            one.startMs(), one.endMs(), one.confidence(),
            group, SemanticGroupRole.LEAD, EditorialReviewStatus.PENDING
        ));
        words.set(1, new EditorialWord(
            two.index(), two.originalEn(), two.approvedEn(),
            "", "dois",
            two.startMs(), two.endMs(), two.confidence(),
            group, SemanticGroupRole.MEMBER, EditorialReviewStatus.PENDING
        ));
        material = replaceCue(
            material,
            first.withReviewAndWords(first.approvedEn(), first.pt(), first.reviewStatus(), words)
        );

        Path editorialDir = tempDir.resolve("editorial");
        FileEditorialMaterialRepository editorialRepository =
            new FileEditorialMaterialRepository(editorialDir);
        editorialRepository.save(material);

        ProjectState state = fixture.project().withEditorialProgress(
            material,
            1,
            new WordReviewPosition(1, 1)
        );
        ProjectState loadedState = saveAndReload(state);
        EditorialMaterial loadedMaterial =
            editorialRepository.load(fixture.translation()).orElseThrow();

        assertEquals(group, loadedMaterial.cues().getFirst().words().get(0).semanticGroupId());
        assertEquals("um", loadedMaterial.cues().getFirst().words().get(0).individualPt());
        assertEquals("dois", loadedMaterial.cues().getFirst().words().get(1).individualPt());
        assertEquals(loadedMaterial.id(), loadedState.editorialMaterialId());
        assertEquals(new WordReviewPosition(1, 1), loadedState.editorialWordPosition().orElseThrow());
    }

    @Test
    void v3ProjectMigratesWithoutPretendingEditorialWasCompleted() throws Exception {
        Fixture fixture = fixture();
        ProjectRepository repository = new ProjectRepository(tempDir.resolve("migration-projects"));
        repository.save(fixture.project());

        Path json = repository.projectDirectory(fixture.project().projectId()).resolve("project.json");
        String legacy = Files.readString(json)
            .replace("\"schemaVersion\" : " + ProjectState.CURRENT_SCHEMA_VERSION, "\"schemaVersion\" : 3");
        Files.writeString(json, legacy);

        ProjectState loaded = repository.list().projects().getFirst();

        assertEquals(ProjectState.CURRENT_SCHEMA_VERSION, loaded.schemaVersion());
        assertEquals(EditorialProjectStatus.NOT_STARTED, loaded.editorialStatus());
        assertEquals(EditorialProjectPhase.CUE_REVIEW, loaded.editorialPhase());
        assertTrue(loaded.translationCompleted());
        assertEquals(ProjectStage.EDITORIAL_REVIEW, loaded.minimumResumeStage());
    }

    private ProjectState saveAndReload(ProjectState state) throws Exception {
        ProjectRepository repository = new ProjectRepository(tempDir.resolve("projects"));
        repository.save(state);
        return repository.list().projects().stream()
            .filter(value -> value.projectId().equals(state.projectId()))
            .findFirst()
            .orElseThrow();
    }

    private static EditorialMaterial approveAllCues(EditorialMaterial material) {
        List<EditorialCue> cues = material.cues().stream()
            .map(cue -> cue.withReview(cue.approvedEn(), cue.pt(), EditorialReviewStatus.APPROVED))
            .toList();
        return material.withCues(cues);
    }

    private static EditorialMaterial approveAllWords(EditorialMaterial material) {
        List<EditorialCue> cues = new ArrayList<>();
        for (EditorialCue cue : material.cues()) {
            List<EditorialWord> words = cue.words().stream()
                .map(word -> word.withReview(
                    word.approvedEn(),
                    word.pt().isBlank() ? "pt-" + word.index() : word.pt(),
                    EditorialReviewStatus.APPROVED
                ))
                .toList();
            cues.add(cue.withReviewAndWords(
                cue.approvedEn(), cue.pt(), EditorialReviewStatus.APPROVED, words
            ));
        }
        return material.withCues(cues);
    }

    private static EditorialMaterial replaceCue(EditorialMaterial material, EditorialCue replacement) {
        List<EditorialCue> cues = new ArrayList<>(material.cues());
        cues.set(replacement.order() - 1, replacement);
        return material.withCues(cues);
    }

    private Fixture fixture() throws Exception {
        SourceMedia source = source();
        MediaCut cut = cut(source);

        List<TimedText> cueOneWords = List.of(
            new TimedText("one", 0, 300, 0.95),
            new TimedText("two", 310, 700, 0.92)
        );
        List<TimedText> cueTwoWords = List.of(
            new TimedText("three", 800, 1200, 0.94)
        );
        List<TimedText> words = List.of(
            cueOneWords.get(0), cueOneWords.get(1), cueTwoWords.get(0)
        );

        AsrResult transcript = new AsrResult(
            "en",
            "one two three",
            List.of(
                new TimedText("one two", 0, 700),
                new TimedText("three", 800, 1200)
            ),
            words
        );

        Path audio = Files.writeString(tempDir.resolve("audio.wav"), "audio");
        PreparedMaterial prepared = new PreparedMaterial(
            "prepared-persist",
            "pipeline",
            "asr",
            cut,
            audio,
            transcript,
            Instant.parse("2026-09-19T12:00:00Z")
        );
        AlignedMaterial aligned = new AlignedMaterial(
            "aligned-persist",
            prepared,
            "dtw",
            words,
            Instant.parse("2026-09-19T12:01:00Z"),
            TimingSource.DTW_REFINED
        );

        TranslationMaterial translation = new TranslationMaterial(
            "translation-persist",
            aligned.id(),
            TranslationMaterial.SCHEMA_VERSION,
            List.of(
                new TranslationCue(
                    1, 0, 700, 0, 700, "",
                    "one two", "one two", "um dois", cueOneWords
                ),
                new TranslationCue(
                    2, 800, 1200, 800, 1200, "",
                    "three", "three", "três", cueTwoWords
                )
            ),
            TranslationSource.GROQ,
            Instant.parse("2026-09-19T12:02:00Z")
        );

        ProjectState project = ProjectState.start(source)
            .withCut(cut)
            .withPrepared(aligned)
            .withTranslation(translation);

        return new Fixture(project, translation);
    }

    private SourceMedia source() throws Exception {
        Path media = Files.writeString(tempDir.resolve("source.mp4"), "video");
        return new SourceMedia(
            "source-persist",
            "https://www.youtube.com/watch?v=source-persist",
            media,
            "Projeto editorial",
            2000,
            Instant.parse("2026-09-19T11:00:00Z"),
            true
        );
    }

    private MediaCut cut(SourceMedia source) throws Exception {
        Path output = Files.writeString(tempDir.resolve("cut.mp4"), "cut");
        return new MediaCut(
            source.sourceId(),
            source.localPath(),
            output,
            0,
            1500,
            1500,
            Instant.parse("2026-09-19T11:30:00Z")
        );
    }

    private record Fixture(ProjectState project, TranslationMaterial translation) {}
}
