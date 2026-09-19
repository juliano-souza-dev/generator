package br.com.immersionhub.generator.desktop.project;

import br.com.immersionhub.generator.desktop.model.MediaCut;
import br.com.immersionhub.generator.desktop.model.SourceMedia;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ProjectRepositoryTest {
    @Test
    void persistsAndReloadsProjectAtStageAfterCut() throws Exception {
        Path dir = Files.createTempDirectory("project-repo");
        SourceMedia source = source(dir, "source-a", "Projeto A");
        MediaCut cut = cut(dir, source, 100, 900);

        ProjectState state = ProjectState.start(source).withCut(cut);
        ProjectRepository repository = new ProjectRepository(dir.resolve("projects"));
        repository.save(state);

        ProjectListing listing = repository.list();
        assertEquals(1, listing.projects().size());
        assertEquals(0, listing.unreadableCount());

        ProjectState loaded = listing.projects().getFirst();
        assertEquals(ProjectStage.PREPARATION, loaded.currentStage());
        assertEquals(ProjectStage.PREPARATION, loaded.minimumResumeStage());
        assertTrue(loaded.completedStages().contains(ProjectStage.SOURCE));
        assertTrue(loaded.completedStages().contains(ProjectStage.WAVE));
        assertEquals(ProjectStageStatus.COMPLETED, loaded.stageStatus(ProjectStage.SOURCE));
        assertEquals(ProjectStageStatus.COMPLETED, loaded.stageStatus(ProjectStage.WAVE));
        assertEquals(ProjectStageStatus.IN_PROGRESS, loaded.stageStatus(ProjectStage.PREPARATION));
        assertEquals(100, loaded.mediaCut().orElseThrow().startMs());
        assertEquals(900, loaded.mediaCut().orElseThrow().endMs());
    }

    @Test
    void missingCutRollsBackOnlyToWave() throws Exception {
        Path dir = Files.createTempDirectory("project-recovery");
        SourceMedia source = source(dir, "source-b", "Projeto B");
        MediaCut cut = cut(dir, source, 0, 1000);
        ProjectState state = ProjectState.start(source).withCut(cut);

        Files.delete(cut.outputPath());

        assertEquals(ProjectStage.WAVE, state.minimumResumeStage());
        assertEquals(ProjectStageStatus.NEEDS_REPROCESSING, state.stageStatus(ProjectStage.WAVE));
        assertTrue(state.recoveryMessage().contains("Wave"));
        assertTrue(state.sourceMedia().isPresent());
        assertTrue(state.mediaCut().isEmpty());
    }

    @Test
    void unreadableProjectDoesNotHideValidProjects() throws Exception {
        Path dir = Files.createTempDirectory("project-corrupt");
        ProjectRepository repository = new ProjectRepository(dir.resolve("projects"));
        repository.save(ProjectState.start(source(dir, "source-c", "Projeto C")));

        Path broken = dir.resolve("projects").resolve("broken");
        Files.createDirectories(broken);
        Files.writeString(broken.resolve("project.json"), "{not-json");

        ProjectListing listing = repository.list();
        assertEquals(1, listing.projects().size());
        assertEquals(1, listing.unreadableCount());
    }

    @Test
    void migratesLegacyZeroSchemaToCurrentVersion() throws Exception {
        Path dir = Files.createTempDirectory("project-migration");
        ProjectRepository repository = new ProjectRepository(dir.resolve("projects"));
        ProjectState state = ProjectState.start(source(dir, "source-d", "Projeto D"));
        repository.save(state);

        Path json = repository.projectDirectory(state.projectId()).resolve("project.json");
        String legacy = Files.readString(json).replace(
            "\"schemaVersion\" : " + ProjectState.CURRENT_SCHEMA_VERSION,
            "\"schemaVersion\" : 0"
        );
        Files.writeString(json, legacy);

        ProjectState loaded = repository.list().projects().getFirst();
        assertEquals(ProjectState.CURRENT_SCHEMA_VERSION, loaded.schemaVersion());
        assertEquals(ProjectStage.WAVE, loaded.minimumResumeStage());
    }

    @Test
    void migratesCompletedPreparationFromV1ToTranslationStage() throws Exception {
        Path dir = Files.createTempDirectory("project-v1-translation");
        SourceMedia source = source(dir, "source-v1", "Projeto V1");
        MediaCut cut = cut(dir, source, 0, 1000);

        Path audio = Files.writeString(dir.resolve("audio.wav"), "a");
        br.com.immersionhub.generator.desktop.preparation.AsrResult transcript =
            new br.com.immersionhub.generator.desktop.preparation.AsrResult(
                "en",
                "hello",
                java.util.List.of(new br.com.immersionhub.generator.desktop.preparation.TimedText("hello", 0, 1000)),
                java.util.List.of(new br.com.immersionhub.generator.desktop.preparation.TimedText("hello", 0, 1000))
            );
        br.com.immersionhub.generator.desktop.preparation.PreparedMaterial prepared =
            new br.com.immersionhub.generator.desktop.preparation.PreparedMaterial(
                "prepared-v1", "pipeline", "asr", cut, audio, transcript, Instant.EPOCH
            );
        br.com.immersionhub.generator.desktop.preparation.AlignedMaterial aligned =
            new br.com.immersionhub.generator.desktop.preparation.AlignedMaterial(
                "aligned-v1",
                prepared,
                "dtw",
                transcript.words(),
                Instant.EPOCH,
                br.com.immersionhub.generator.desktop.preparation.TimingSource.DTW_REFINED
            );

        ProjectRepository repository = new ProjectRepository(dir.resolve("projects"));
        ProjectState current = ProjectState.start(source).withCut(cut).withPrepared(aligned);
        repository.save(current);

        Path json = repository.projectDirectory(current.projectId()).resolve("project.json");
        String legacy = Files.readString(json)
            .replace("\"schemaVersion\" : " + ProjectState.CURRENT_SCHEMA_VERSION, "\"schemaVersion\" : 1")
            .replace("\"currentStage\" : \"TRANSLATION\"", "\"currentStage\" : \"PREPARATION\"");
        Files.writeString(json, legacy);

        ProjectState loaded = repository.list().projects().getFirst();
        assertEquals(ProjectStage.TRANSLATION, loaded.currentStage());
        assertEquals(ProjectStage.TRANSLATION, loaded.minimumResumeStage());
        assertTrue(loaded.preparationCompleted());
        assertFalse(loaded.translationCompleted());
    }

    @Test
    void migratesTranslatedV2ProjectDirectlyToEditorialReview() throws Exception {
        Path dir = Files.createTempDirectory("project-v2-review");
        SourceMedia source = source(dir, "source-v2", "Projeto V2");
        MediaCut cut = cut(dir, source, 0, 1000);

        Path audio = Files.writeString(dir.resolve("audio-v2.wav"), "a");
        br.com.immersionhub.generator.desktop.preparation.AsrResult transcript =
            new br.com.immersionhub.generator.desktop.preparation.AsrResult(
                "en",
                "hello",
                java.util.List.of(new br.com.immersionhub.generator.desktop.preparation.TimedText("hello", 0, 1000)),
                java.util.List.of(new br.com.immersionhub.generator.desktop.preparation.TimedText("hello", 0, 1000))
            );
        br.com.immersionhub.generator.desktop.preparation.PreparedMaterial prepared =
            new br.com.immersionhub.generator.desktop.preparation.PreparedMaterial(
                "prepared-v2", "pipeline", "asr", cut, audio, transcript, Instant.EPOCH
            );
        br.com.immersionhub.generator.desktop.preparation.AlignedMaterial aligned =
            new br.com.immersionhub.generator.desktop.preparation.AlignedMaterial(
                "aligned-v2", prepared, "dtw", transcript.words(), Instant.EPOCH,
                br.com.immersionhub.generator.desktop.preparation.TimingSource.DTW_REFINED
            );
        br.com.immersionhub.generator.desktop.translation.TranslationMaterial translation =
            new br.com.immersionhub.generator.desktop.translation.TranslationMaterial(
                "translation-v2",
                aligned.id(),
                br.com.immersionhub.generator.desktop.translation.TranslationMaterial.SCHEMA_VERSION,
                java.util.List.of(new br.com.immersionhub.generator.desktop.translation.TranslationCue(
                    1, 0, 1000, 0, 1000, "", "hello", "hello", "olá", transcript.words()
                )),
                br.com.immersionhub.generator.desktop.translation.TranslationSource.GROQ,
                Instant.EPOCH
            );

        ProjectRepository repository = new ProjectRepository(dir.resolve("projects"));
        ProjectState current = ProjectState.start(source)
            .withCut(cut)
            .withPrepared(aligned)
            .withTranslation(translation);
        repository.save(current);

        Path json = repository.projectDirectory(current.projectId()).resolve("project.json");
        String legacy = Files.readString(json)
            .replace("\"schemaVersion\" : " + ProjectState.CURRENT_SCHEMA_VERSION, "\"schemaVersion\" : 2")
            .replace("\"currentStage\" : \"EDITORIAL_REVIEW\"", "\"currentStage\" : \"TRANSLATION\"");
        Files.writeString(json, legacy);

        ProjectState loaded = repository.list().projects().getFirst();

        assertEquals(ProjectStage.EDITORIAL_REVIEW, loaded.currentStage());
        assertEquals(ProjectStage.EDITORIAL_REVIEW, loaded.minimumResumeStage());
        assertTrue(loaded.translationCompleted());
    }

    @Test
    void keepsMultipleProjectsIndependent() throws Exception {
        Path dir = Files.createTempDirectory("project-multiple");
        ProjectRepository repository = new ProjectRepository(dir.resolve("projects"));

        ProjectState first = ProjectState.start(source(dir, "source-e1", "Projeto E1"));
        ProjectState second = ProjectState.start(source(dir, "source-e2", "Projeto E2"));

        repository.save(first);
        repository.save(second);

        ProjectListing listing = repository.list();
        assertEquals(2, listing.projects().size());
        assertNotEquals(listing.projects().get(0).projectId(), listing.projects().get(1).projectId());
    }

    private static SourceMedia source(Path dir, String id, String title) throws Exception {
        Path media = Files.writeString(dir.resolve(id + ".mp4"), "video");
        return new SourceMedia(
            id,
            "https://www.youtube.com/watch?v=" + id,
            media,
            title,
            1000,
            Instant.parse("2026-09-19T00:00:00Z"),
            true
        );
    }

    private static MediaCut cut(Path dir, SourceMedia source, long start, long end) throws Exception {
        Path output = Files.writeString(dir.resolve(source.sourceId() + "-cut.mp4"), "cut");
        return new MediaCut(
            source.sourceId(),
            source.localPath(),
            output,
            start,
            end,
            end - start,
            Instant.parse("2026-09-19T00:01:00Z")
        );
    }
}
