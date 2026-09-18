package br.com.immersionhub.generator.desktop.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

public record MediaCut(
    String sourceId,
    Path sourcePath,
    Path outputPath,
    long startMs,
    long endMs,
    long durationMs,
    Instant createdAt
) {
    public MediaCut {
        sourceId = Objects.requireNonNull(sourceId, "sourceId").trim();
        sourcePath = Objects.requireNonNull(sourcePath, "sourcePath").toAbsolutePath().normalize();
        outputPath = Objects.requireNonNull(outputPath, "outputPath").toAbsolutePath().normalize();
        createdAt = Objects.requireNonNull(createdAt, "createdAt");

        if (sourceId.isEmpty()) throw new IllegalArgumentException("sourceId não pode ser vazio.");
        if (!Files.isRegularFile(sourcePath)) throw new IllegalArgumentException("Fonte local ausente.");
        if (!Files.isRegularFile(outputPath)) throw new IllegalArgumentException("Recorte local ausente.");
        if (sourcePath.equals(outputPath)) throw new IllegalArgumentException("O recorte não pode sobrescrever a fonte.");
        if (startMs < 0 || endMs <= startMs) throw new IllegalArgumentException("Intervalo de recorte inválido.");
        if (durationMs != endMs - startMs) throw new IllegalArgumentException("Duração do recorte inconsistente.");
    }
}
