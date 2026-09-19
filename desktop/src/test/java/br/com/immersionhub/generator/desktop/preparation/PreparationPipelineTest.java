package br.com.immersionhub.generator.desktop.preparation;

import br.com.immersionhub.generator.desktop.model.MediaCut;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PreparationPipelineTest {
    @Test
    void completesWithAsrTimingsWhenDtwFails() throws Exception {
        Path dir = Files.createTempDirectory("prep-pipeline-fallback");
        Path source = Files.writeString(dir.resolve("source.mp4"), "s");
        Path cutPath = Files.writeString(dir.resolve("cut.mp4"), "c");
        Path audio = Files.writeString(dir.resolve("audio.wav"), "a");
        MediaCut cut = new MediaCut("source", source, cutPath, 0, 1000, 1000, Instant.EPOCH);

        PreparedMaterialRepository preparedRepository = new PreparedMaterialRepository() {
            private PreparedMaterial stored;
            public Optional<PreparedMaterial> load(String id) { return Optional.ofNullable(stored); }
            public void save(PreparedMaterial material) { stored = material; }
        };

        MaterialPreparationService preparation = new MaterialPreparationService(
            (mediaCut, output) -> audio,
            new AsrEngine() {
                public AsrResult transcribeEnglish(Path path) {
                    return new AsrResult(
                        "en",
                        "hello world",
                        List.of(new TimedText("hello world", 0, 1000)),
                        List.of(
                            new TimedText("hello", 0, 400),
                            new TimedText("world", 400, 1000)
                        )
                    );
                }
                public String version() { return "asr-test"; }
            },
            preparedRepository,
            dir,
            "pipeline-test"
        );

        Map<String, AlignedMaterial> alignedCache = new HashMap<>();
        AlignedMaterialRepository alignedRepository = new AlignedMaterialRepository() {
            public Optional<AlignedMaterial> load(String id, PreparedMaterial ignored) {
                return Optional.ofNullable(alignedCache.get(id));
            }
            public void save(AlignedMaterial material) { alignedCache.put(material.id(), material); }
        };

        WordAlignmentService alignment = new WordAlignmentService(
            new WordAligner() {
                public List<TimedText> align(Path technicalAudio, AsrResult transcription, long durationMs) {
                    throw new IllegalStateException("dtw failed");
                }
                public String version() { return "dtw-test"; }
            },
            alignedRepository
        );

        Path log = dir.resolve("preparation.log");
        PreparationPipeline pipeline = new PreparationPipeline(
            preparation,
            alignment,
            new PreparationLogger(log)
        );

        List<PreparationStage> stages = new ArrayList<>();
        AlignedMaterial result = pipeline.prepare(cut, stages::add);

        assertEquals(TimingSource.ASR_BASE, result.timingSource());
        assertEquals(List.of("hello", "world"), result.words().stream().map(TimedText::text).toList());
        assertTrue(stages.contains(PreparationStage.TRANSCRIBING));
        assertTrue(stages.contains(PreparationStage.USING_BASE_TIMINGS));
        assertEquals(PreparationStage.READY, stages.getLast());
        assertTrue(Files.readString(log).contains("prepare.alignment.fallback"));

        List<PreparationStage> secondStages = new ArrayList<>();
        AlignedMaterial cached = pipeline.prepare(cut, secondStages::add);
        assertEquals(TimingSource.ASR_BASE, cached.timingSource());
        assertTrue(secondStages.contains(PreparationStage.USING_BASE_TIMINGS));
    }

    @Test
    void keepsRefinedTimingsWhenDtwSucceeds() throws Exception {
        Path dir = Files.createTempDirectory("prep-pipeline-refined");
        Path source = Files.writeString(dir.resolve("source.mp4"), "s");
        Path cutPath = Files.writeString(dir.resolve("cut.mp4"), "c");
        Path audio = Files.writeString(dir.resolve("audio.wav"), "a");
        MediaCut cut = new MediaCut("source", source, cutPath, 0, 1000, 1000, Instant.EPOCH);

        MaterialPreparationService preparation = new MaterialPreparationService(
            (mediaCut, output) -> audio,
            new AsrEngine() {
                public AsrResult transcribeEnglish(Path path) {
                    return new AsrResult(
                        "en",
                        "hello world",
                        List.of(new TimedText("hello world", 0, 1000)),
                        List.of(new TimedText("hello", 0, 500), new TimedText("world", 500, 1000))
                    );
                }
                public String version() { return "asr-test"; }
            },
            new PreparedMaterialRepository() {
                public Optional<PreparedMaterial> load(String id) { return Optional.empty(); }
                public void save(PreparedMaterial material) {}
            },
            dir,
            "pipeline-test"
        );

        WordAlignmentService alignment = new WordAlignmentService(
            new WordAligner() {
                public List<TimedText> align(Path technicalAudio, AsrResult transcription, long durationMs) {
                    return List.of(
                        new TimedText("hello", 0, 450),
                        new TimedText("world", 450, 1000)
                    );
                }
                public String version() { return "dtw-test"; }
            },
            new AlignedMaterialRepository() {
                public Optional<AlignedMaterial> load(String id, PreparedMaterial ignored) { return Optional.empty(); }
                public void save(AlignedMaterial material) {}
            }
        );

        List<PreparationStage> stages = new ArrayList<>();
        AlignedMaterial result = new PreparationPipeline(
            preparation,
            alignment,
            new PreparationLogger(dir.resolve("preparation.log"))
        ).prepare(cut, stages::add);

        assertEquals(TimingSource.DTW_REFINED, result.timingSource());
        assertTrue(stages.contains(PreparationStage.ALIGNMENT_READY));
        assertFalse(stages.contains(PreparationStage.USING_BASE_TIMINGS));
    }
}
