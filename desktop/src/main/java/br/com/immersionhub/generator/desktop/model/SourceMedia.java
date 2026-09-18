package br.com.immersionhub.generator.desktop.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

public record SourceMedia(
    String sourceId,
    String canonicalUrl,
    Path localPath,
    String title,
    long durationMs,
    Instant fetchedAt,
    boolean cacheHit
) {
    public SourceMedia {
        sourceId = requireText(sourceId, "sourceId");
        canonicalUrl = requireText(canonicalUrl, "canonicalUrl");
        title = requireText(title, "title");
        localPath = Objects.requireNonNull(localPath, "localPath").toAbsolutePath().normalize();
        fetchedAt = Objects.requireNonNull(fetchedAt, "fetchedAt");
        if (durationMs <= 0) {
            throw new IllegalArgumentException("durationMs deve ser maior que zero.");
        }
        if (!Files.isRegularFile(localPath)) {
            throw new IllegalArgumentException("A mídia local não existe: " + localPath);
        }
    }

    public SourceMedia asCacheHit() {
        return new SourceMedia(sourceId, canonicalUrl, localPath, title, durationMs, fetchedAt, true);
    }

    private static String requireText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " não pode ser vazio.");
        }
        return normalized;
    }
}
