package br.com.immersionhub.generator.desktop.preparation;

import br.com.immersionhub.generator.desktop.model.MediaCut;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

public record PreparedMaterial(
    String id, String pipelineVersion, String asrVersion, MediaCut mediaCut,
    Path technicalAudio, AsrResult transcription, Instant createdAt
) {
    public PreparedMaterial {
        id = Objects.requireNonNull(id, "id").trim();
        pipelineVersion = Objects.requireNonNull(pipelineVersion, "pipelineVersion").trim();
        asrVersion = Objects.requireNonNull(asrVersion, "asrVersion").trim();
        mediaCut = Objects.requireNonNull(mediaCut, "mediaCut");
        technicalAudio = Objects.requireNonNull(technicalAudio, "technicalAudio").toAbsolutePath().normalize();
        transcription = Objects.requireNonNull(transcription, "transcription");
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
        if (id.isEmpty() || pipelineVersion.isEmpty() || asrVersion.isEmpty()) throw new IllegalArgumentException("Identidade incompleta.");
        if (!Files.isRegularFile(technicalAudio)) throw new IllegalArgumentException("Áudio técnico ausente.");
        transcription.validate(mediaCut.durationMs());
    }
}
