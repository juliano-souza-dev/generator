package br.com.immersionhub.generator.desktop.translation;

import br.com.immersionhub.generator.desktop.model.MediaCut;
import br.com.immersionhub.generator.desktop.preparation.AlignedMaterial;
import br.com.immersionhub.generator.desktop.preparation.AsrResult;
import br.com.immersionhub.generator.desktop.preparation.PreparedMaterial;
import br.com.immersionhub.generator.desktop.preparation.TimedText;
import br.com.immersionhub.generator.desktop.preparation.TimingSource;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TranslationServiceTest {
    @Test
    void groqSuccessStillBuildsExternalPackageAndPersistsValidatedTranslation() throws Exception {
        Fixture fixture = fixture("translation-success");
        List<TranslationStage> stages = new ArrayList<>();

        GroqClient groq = new GroqClient() {
            public GroqModelInfo modelInfo() {
                return new GroqModelInfo("test-model", 8192, 2048);
            }

            public GroqCompletion translate(List<TranslationCue> cues) {
                StringBuilder json = new StringBuilder("{\"translations\":[");
                for (int i = 0; i < cues.size(); i++) {
                    if (i > 0) json.append(',');
                    json.append("{\"order\":").append(cues.get(i).order())
                        .append(",\"pt\":\"tradução ").append(cues.get(i).order()).append("\"}");
                }
                json.append("]}");
                return new GroqCompletion(json.toString(), GroqRateSnapshot.empty());
            }
        };

        TranslationService service = service(fixture, groq);
        TranslationOutcome outcome = service.translate(fixture.aligned, stages::add);

        assertTrue(outcome.translated());
        assertEquals(TranslationSource.GROQ, outcome.translatedMaterial().source());
        assertTrue(Files.isRegularFile(outcome.externalPackage().zipFile()));
        assertTrue(Files.isRegularFile(outcome.externalPackage().audioFile()));
        assertTrue(Files.isRegularFile(outcome.externalPackage().instructionsFile()));
        assertTrue(stages.contains(TranslationStage.PREPARING_PACKAGE));
        assertTrue(stages.contains(TranslationStage.TRANSLATING));
        assertEquals(TranslationStage.READY, stages.getLast());

        TranslationMaterial base = TranslationMaterialFactory.base(fixture.aligned);
        TranslationMaterial loaded = new FileTranslationMaterialRepository(fixture.translationDir)
            .load(base)
            .orElseThrow();
        assertTrue(loaded.complete());
        assertEquals(TranslationSource.GROQ, loaded.source());
    }

    @Test
    void groqFailureDoesNotDestroyFallbackAndDoesNotPublishPartialTranslation() throws Exception {
        Fixture fixture = fixture("translation-fallback");

        GroqClient groq = new GroqClient() {
            public GroqModelInfo modelInfo() {
                return new GroqModelInfo("test-model", 8192, 2048);
            }

            public GroqCompletion translate(List<TranslationCue> cues) throws Exception {
                throw new GroqApiException(429, "rate limited", GroqRateSnapshot.empty());
            }
        };

        TranslationOutcome outcome = service(fixture, groq).translate(fixture.aligned, ignored -> {});

        assertFalse(outcome.translated());
        assertTrue(outcome.externalRequired());
        assertTrue(Files.isRegularFile(outcome.externalPackage().zipFile()));
        assertTrue(new FileTranslationMaterialRepository(fixture.translationDir)
            .load(TranslationMaterialFactory.base(fixture.aligned))
            .isEmpty());
        assertTrue(Files.readString(fixture.log).contains("translation.groq.fallback"));
    }

    @Test
    void externalReturnMayOnlyFillPtAndPreservesProtectedFields() throws Exception {
        Fixture fixture = fixture("translation-external");
        TranslationMaterial base = TranslationMaterialFactory.base(fixture.aligned);
        TranslationDocumentCodec codec = new TranslationDocumentCodec();

        String valid = codec.toJson(TranslationMaterialFactory.apply(
            base,
            List.of(
                new TranslatedCue(1, "Olá mundo."),
                new TranslatedCue(2, "Como você está?")
            ),
            TranslationSource.EXTERNAL
        ));

        Path returned = Files.writeString(fixture.root.resolve("returned.json"), valid);
        TranslationMaterial imported = service(fixture, null).importExternal(returned, base);
        assertEquals(TranslationSource.EXTERNAL, imported.source());

        String tampered = valid.replace("\"speechStartMs\" : 0", "\"speechStartMs\" : 10");
        Path invalid = Files.writeString(fixture.root.resolve("tampered.json"), tampered);
        assertThrows(
            IllegalArgumentException.class,
            () -> service(fixture, null).importExternal(invalid, base)
        );
    }

    private static TranslationService service(Fixture fixture, GroqClient groq) {
        return new TranslationService(
            groq,
            new TranslationChunker(new TokenEstimator()),
            new GroqTranslationResponseCodec(),
            new ExternalAiPackageService(),
            new FileTranslationMaterialRepository(fixture.translationDir),
            new TranslationLogger(fixture.log),
            fixture.translationDir
        );
    }

    private static Fixture fixture(String name) throws Exception {
        Path root = Files.createTempDirectory(name);
        Path source = Files.writeString(root.resolve("source.mp4"), "source");
        Path cutPath = Files.writeString(root.resolve("cut.mp4"), "cut");
        Path audio = Files.writeString(root.resolve("scene_audio_16k_mono.wav"), "audio");

        MediaCut cut = new MediaCut("source", source, cutPath, 0, 2000, 2000, Instant.EPOCH);
        AsrResult transcript = new AsrResult(
            "en",
            "Hello world. How are you?",
            List.of(
                new TimedText("Hello world.", 0, 900),
                new TimedText("How are you?", 1000, 2000)
            ),
            List.of(
                new TimedText("Hello", 0, 400),
                new TimedText("world.", 400, 900),
                new TimedText("How", 1000, 1250),
                new TimedText("are", 1250, 1500),
                new TimedText("you?", 1500, 2000)
            )
        );
        PreparedMaterial prepared = new PreparedMaterial(
            "prepared", "pipeline", "asr", cut, audio, transcript, Instant.EPOCH
        );
        AlignedMaterial aligned = new AlignedMaterial(
            "aligned",
            prepared,
            "dtw",
            transcript.words(),
            Instant.EPOCH,
            TimingSource.DTW_REFINED
        );

        return new Fixture(
            root,
            root.resolve("translation"),
            root.resolve("translation.log"),
            aligned
        );
    }

    private record Fixture(
        Path root,
        Path translationDir,
        Path log,
        AlignedMaterial aligned
    ) {}
}
