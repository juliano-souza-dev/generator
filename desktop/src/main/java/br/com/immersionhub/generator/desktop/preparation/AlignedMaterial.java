package br.com.immersionhub.generator.desktop.preparation;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record AlignedMaterial(
    String id,
    PreparedMaterial preparedMaterial,
    String alignerVersion,
    List<TimedText> words,
    Instant createdAt,
    TimingSource timingSource
) {
    public AlignedMaterial {
        id = Objects.requireNonNull(id, "id").trim();
        preparedMaterial = Objects.requireNonNull(preparedMaterial, "preparedMaterial");
        alignerVersion = Objects.requireNonNull(alignerVersion, "alignerVersion").trim();
        words = List.copyOf(Objects.requireNonNull(words, "words"));
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
        timingSource = Objects.requireNonNull(timingSource, "timingSource");

        if (id.isEmpty() || alignerVersion.isEmpty()) throw new IllegalArgumentException("Identidade de alinhamento incompleta.");
        if (words.isEmpty()) throw new IllegalArgumentException("Alinhamento sem palavras.");
        validate(words, preparedMaterial.mediaCut().durationMs());
    }

    public AlignedMaterial(
        String id,
        PreparedMaterial preparedMaterial,
        String alignerVersion,
        List<TimedText> words,
        Instant createdAt
    ) {
        this(id, preparedMaterial, alignerVersion, words, createdAt, TimingSource.DTW_REFINED);
    }

    public AsrResult transcription() {
        AsrResult base = preparedMaterial.transcription();
        return new AsrResult(base.language(), base.text(), base.segments(), words);
    }

    static void validate(List<TimedText> words, long durationMs) {
        long previousStart = -1;
        long previousEnd = -1;
        for (TimedText word : words) {
            if (word.endMs() > durationMs || word.startMs() < previousStart || word.startMs() < previousEnd) {
                throw new IllegalArgumentException("Alinhamento temporal inválido.");
            }
            previousStart = word.startMs();
            previousEnd = word.endMs();
        }
    }
}
