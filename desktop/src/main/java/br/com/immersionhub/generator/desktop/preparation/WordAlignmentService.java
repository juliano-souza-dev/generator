package br.com.immersionhub.generator.desktop.preparation;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class WordAlignmentService {
    private final WordAligner aligner;
    private final AlignedMaterialRepository repository;

    public WordAlignmentService(WordAligner aligner, AlignedMaterialRepository repository) {
        this.aligner = Objects.requireNonNull(aligner);
        this.repository = Objects.requireNonNull(repository);
    }

    public AlignedMaterial align(PreparedMaterial preparedMaterial) throws Exception {
        String id = AlignmentIds.from(preparedMaterial, aligner.version());
        var cached = repository.load(id, preparedMaterial);
        if (cached.isPresent()) return cached.get();

        var words = aligner.align(
            preparedMaterial.technicalAudio(),
            preparedMaterial.transcription(),
            preparedMaterial.mediaCut().durationMs()
        );
        AlignedMaterial.validate(words, preparedMaterial.mediaCut().durationMs());

        AlignedMaterial aligned = new AlignedMaterial(
            id,
            preparedMaterial,
            aligner.version(),
            words,
            Instant.now()
        );
        repository.save(aligned);
        return aligned;
    }

    public AlignedMaterial fallbackToAsr(PreparedMaterial preparedMaterial) throws Exception {
        String id = AlignmentIds.from(preparedMaterial, aligner.version());
        var cached = repository.load(id, preparedMaterial);
        if (cached.isPresent()) return cached.get();

        List<TimedText> words = preparedMaterial.transcription().words();
        if (words.isEmpty()) {
            throw new IllegalStateException("Transcrição base sem timings utilizáveis.");
        }
        AlignedMaterial.validate(words, preparedMaterial.mediaCut().durationMs());

        AlignedMaterial fallback = new AlignedMaterial(
            id,
            preparedMaterial,
            aligner.version(),
            words,
            Instant.now(),
            TimingSource.ASR_BASE
        );
        repository.save(fallback);
        return fallback;
    }
}
