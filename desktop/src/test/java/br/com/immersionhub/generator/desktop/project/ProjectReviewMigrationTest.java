package br.com.immersionhub.generator.desktop.project;

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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProjectReviewMigrationTest {
    @TempDir
    Path tempDir;

    @Test
    void completedTranslationResumesInReviewAndLegacyV2MigratesWithoutRetranslation() throws Exception {
        SourceMedia source = source();
        MediaCut cut = cut(source);
        AlignedMaterial aligned = aligned(cut);
        TranslationMaterial translation = translation(aligned);

        ProjectState current = ProjectState.start(source)
            .withCut(cut)
            .withPrepared(aligned)
            .withTranslation(translation);

        assertEquals(ProjectStage.EDITORIAL_REVIEW, current.currentStage());
        assertEquals(ProjectStage.EDITORIAL_REVIEW, current.minimumResumeStage());
        assertTrue(current.translationCompleted());

        ProjectRepository repository = new ProjectRepository(tempDir.resolve("projects"));
        repository.save(current);

        Path json = repository.projectDirectory(current.projectId()).resolve("project.json");
        String legacyV2 = Files.readString(json)
            .replace("\\\"schemaVersion\\\" : 3", "\\\"schemaVersion\\\" : 2")
            .replace("\\\"currentStage\\\" : \\\"EDITORIAL_REVIEW\\\"", "\\\"currentStage\\\" : \\\"TRANSLATION\\\"");
        Files.writeString(json, legacyV2);

        ProjectState migrated = repository.list().projects().getFirst();

        assertEquals(ProjectState.CURRENT_SCHEMA_VERSION, migrated.schemaVersion());
        assertTrue(migrated.translationCompleted());
        assertEquals(ProjectStage.EDITORIAL_REVIEW, migrated.currentStage());
        assertEquals(ProjectStage.EDITORIAL_REVIEW, migrated.minimumResumeStage());
    }

    private SourceMedia source() throws Exception {
        Path media = Files.writeString(tempDir.resolve("source.mp4"), "video");
        return new SourceMedia(
            "source-review",
            "https://www.youtube.com/watch?v=source-review",
            media,
            "Projeto Review",
            2_000,
            Instant.parse("2026-09-19T00:00:00Z"),
            true
        );
    }

    private MediaCut cut(SourceMedia source) throws Exception {
        Path output = Files.writeString(tempDir.resolve("scene_video.mp4"), "cut");
        return new MediaCut(
            source.sourceId(),
            source.localPath(),
            output,
            0,
            2_000,
            2_000,
            Instant.parse("2026-09-19T00:01:00Z")
        );
    }

    private AlignedMaterial aligned(MediaCut cut) throws Exception {
        Path audio = Files.writeString(tempDir.resolve("scene_audio.wav"), "audio");
        List<TimedText> words = List.of(
            new TimedText("hello", 100, 500, 0.9),
            new TimedText("world", 510, 900, 0.9)
        );
        AsrResult asr = new AsrResult(
            "en",
            "hello world",
            List.of(new TimedText("hello world", 100, 900)),
            words
        );
        PreparedMaterial prepared = new PreparedMaterial(
            "prepared-review",
            "pipeline",
            "asr",
            cut,
            audio,
            asr,
            Instant.EPOCH
        );
        return new AlignedMaterial(
            "aligned-review",
            prepared,
            "dtw",
            words,
            Instant.EPOCH,
            TimingSource.DTW_REFINED
        );
    }

    private static TranslationMaterial translation(AlignedMaterial aligned) {
        return new TranslationMaterial(
            "translation-review",
            aligned.id(),
            TranslationMaterial.SCHEMA_VERSION,
            List.of(new TranslationCue(
                1,
                100,
                900,
                100,
                900,
                "",
                "hello world",
                "hello world",
                "olá mundo",
                aligned.words()
            )),
            TranslationSource.GROQ,
            Instant.EPOCH
        );
    }
}
