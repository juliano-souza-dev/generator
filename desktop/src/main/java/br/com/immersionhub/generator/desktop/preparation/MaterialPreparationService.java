package br.com.immersionhub.generator.desktop.preparation;

import br.com.immersionhub.generator.desktop.model.MediaCut;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Consumer;

public final class MaterialPreparationService {
    private final TechnicalAudioExtractor audio;
    private final AsrEngine asr;
    private final PreparedMaterialRepository repository;
    private final Path workspace;
    private final String pipelineVersion;

    public MaterialPreparationService(TechnicalAudioExtractor audio, AsrEngine asr, PreparedMaterialRepository repository, Path workspace, String pipelineVersion) {
        this.audio=Objects.requireNonNull(audio); this.asr=Objects.requireNonNull(asr);
        this.repository=Objects.requireNonNull(repository); this.workspace=Objects.requireNonNull(workspace);
        this.pipelineVersion=Objects.requireNonNull(pipelineVersion);
    }

    public PreparedMaterial prepare(MediaCut cut) throws Exception {
        return prepare(cut, ignored -> {});
    }

    public PreparedMaterial prepare(MediaCut cut, Consumer<PreparationStage> progress) throws Exception {
        Consumer<PreparationStage> listener = progress == null ? ignored -> {} : progress;
        String id=PreparationIds.from(cut,pipelineVersion,asr.version());
        var cached=repository.load(id);
        if(cached.isPresent()) {
            PreparedMaterial stored = cached.get();
            PreparedMaterial rebound = new PreparedMaterial(
                stored.id(),
                stored.pipelineVersion(),
                stored.asrVersion(),
                cut,
                stored.technicalAudio(),
                stored.transcription(),
                stored.createdAt()
            );
            listener.accept(PreparationStage.TRANSCRIPTION_READY);
            return rebound;
        }

        listener.accept(PreparationStage.PREPARING_AUDIO);
        Path dir=workspace.resolve("prepared").resolve(id);
        Path technicalAudio=audio.extract(cut,dir);

        listener.accept(PreparationStage.TRANSCRIBING);
        AsrResult result=AsrTimelineNormalizer.normalize(
            asr.transcribeEnglish(technicalAudio),
            cut.durationMs()
        );
        result.validate(cut.durationMs());

        PreparedMaterial material=new PreparedMaterial(id,pipelineVersion,asr.version(),cut,technicalAudio,result,Instant.now());
        repository.save(material);
        listener.accept(PreparationStage.TRANSCRIPTION_READY);
        return material;
    }
}
