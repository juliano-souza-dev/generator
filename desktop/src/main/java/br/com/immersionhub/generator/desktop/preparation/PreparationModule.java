package br.com.immersionhub.generator.desktop.preparation;

import br.com.immersionhub.generator.desktop.infrastructure.AppDirectories;
import br.com.immersionhub.generator.desktop.infrastructure.BundledTools;
import br.com.immersionhub.generator.desktop.model.MediaCut;
import java.nio.file.Path;
import java.util.Optional;

public final class PreparationModule {
    public static final String PIPELINE_VERSION = "preparation-v2";
    public static final String ASR_VERSION = "whisper.cpp-b5130-base.en";
    public static final String ALIGNMENT_VERSION = "whisper.cpp-b5130-dtw-base.en";

    private PreparationModule() {}

    public static Optional<AlignedMaterial> loadAligned(String preparedMaterialId, String alignedMaterialId) {
        return loadAligned(preparedMaterialId, alignedMaterialId, null);
    }

    public static Optional<AlignedMaterial> loadAligned(
        String preparedMaterialId,
        String alignedMaterialId,
        MediaCut currentCut
    ) {
        if (preparedMaterialId == null || preparedMaterialId.isBlank()
            || alignedMaterialId == null || alignedMaterialId.isBlank()) {
            return Optional.empty();
        }

        Path root = AppDirectories.workspaceDir().resolve("preparation");
        FilePreparedMaterialRepository preparedRepository =
            new FilePreparedMaterialRepository(root.resolve("prepared"));
        FileAlignedMaterialRepository alignedRepository =
            new FileAlignedMaterialRepository(root.resolve("aligned"));

        return preparedRepository.load(preparedMaterialId)
            .map(prepared -> currentCut == null ? prepared : new PreparedMaterial(
                prepared.id(),
                prepared.pipelineVersion(),
                prepared.asrVersion(),
                currentCut,
                prepared.technicalAudio(),
                prepared.transcription(),
                prepared.createdAt()
            ))
            .flatMap(prepared -> alignedRepository.load(alignedMaterialId, prepared));
    }

    public static PreparationPipeline createPipeline() {
        Path root = AppDirectories.workspaceDir().resolve("preparation");
        Path model = BundledTools.whisperModel();

        MaterialPreparationService preparation = new MaterialPreparationService(
            new FfmpegTechnicalAudioExtractor(),
            new WhisperCppAsrEngine(model, ASR_VERSION),
            new FilePreparedMaterialRepository(root.resolve("prepared")),
            root,
            PIPELINE_VERSION
        );

        WordAlignmentService alignment = new WordAlignmentService(
            new WhisperDtwWordAligner(model, "base.en", ALIGNMENT_VERSION),
            new FileAlignedMaterialRepository(root.resolve("aligned"))
        );

        return new PreparationPipeline(
            preparation,
            alignment,
            new PreparationLogger(AppDirectories.logsDir().resolve("preparation.log"))
        );
    }
}
