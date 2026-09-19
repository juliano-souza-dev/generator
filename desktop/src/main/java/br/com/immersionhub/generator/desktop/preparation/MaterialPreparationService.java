package br.com.immersionhub.generator.desktop.preparation;

import br.com.immersionhub.generator.desktop.model.MediaCut;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

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
        String id=PreparationIds.from(cut,pipelineVersion,asr.version());
        var cached=repository.load(id);
        if(cached.isPresent()) return cached.get();

        Path dir=workspace.resolve("prepared").resolve(id);
        Path technicalAudio=audio.extract(cut,dir);
        AsrResult result=AsrTimelineNormalizer.normalize(
            asr.transcribeEnglish(technicalAudio),
            cut.durationMs()
        );
        result.validate(cut.durationMs());
        PreparedMaterial material=new PreparedMaterial(id,pipelineVersion,asr.version(),cut,technicalAudio,result,Instant.now());
        repository.save(material);
        return material;
    }
}
