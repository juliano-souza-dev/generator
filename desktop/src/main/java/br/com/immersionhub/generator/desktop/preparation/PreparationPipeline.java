package br.com.immersionhub.generator.desktop.preparation;

import br.com.immersionhub.generator.desktop.model.MediaCut;
import java.util.Objects;
import java.util.function.Consumer;

public final class PreparationPipeline {
    private final MaterialPreparationService preparation;
    private final WordAlignmentService alignment;
    private final PreparationLogger logger;

    public PreparationPipeline(
        MaterialPreparationService preparation,
        WordAlignmentService alignment,
        PreparationLogger logger
    ) {
        this.preparation = Objects.requireNonNull(preparation);
        this.alignment = Objects.requireNonNull(alignment);
        this.logger = Objects.requireNonNull(logger);
    }

    public AlignedMaterial prepare(MediaCut cut, Consumer<PreparationStage> progress) throws Exception {
        Objects.requireNonNull(cut);
        Consumer<PreparationStage> listener = progress == null ? ignored -> {} : progress;

        logger.info("prepare.start sourceId=" + cut.sourceId() + " startMs=" + cut.startMs() + " endMs=" + cut.endMs());
        try {
            listener.accept(PreparationStage.PREPARING);
            PreparedMaterial prepared = preparation.prepare(cut);
            logger.info("prepare.asr.ready materialId=" + prepared.id());

            listener.accept(PreparationStage.ALIGNING);
            AlignedMaterial aligned = alignment.align(prepared);
            logger.info("prepare.alignment.ready alignedId=" + aligned.id() + " words=" + aligned.words().size());

            listener.accept(PreparationStage.READY);
            return aligned;
        } catch (Exception exception) {
            logger.error("prepare.failed sourceId=" + cut.sourceId(), exception);
            throw exception;
        }
    }
}
