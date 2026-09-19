package br.com.immersionhub.generator.desktop.translation;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record TranslationMaterial(
    String id,
    String alignedMaterialId,
    String schemaVersion,
    List<TranslationCue> cues,
    TranslationSource source,
    Instant createdAt
) {
    public static final String SCHEMA_VERSION = "1.0";

    public TranslationMaterial {
        id = requireText(id, "id");
        alignedMaterialId = requireText(alignedMaterialId, "alignedMaterialId");
        schemaVersion = requireText(schemaVersion, "schemaVersion");
        cues = List.copyOf(Objects.requireNonNull(cues, "cues"));
        source = Objects.requireNonNull(source, "source");
        createdAt = Objects.requireNonNull(createdAt, "createdAt");

        if (cues.isEmpty()) throw new IllegalArgumentException("Material sem cues.");
        Set<Integer> orders = new HashSet<>();
        for (int index = 0; index < cues.size(); index++) {
            TranslationCue cue = cues.get(index);
            if (cue.order() != index + 1 || !orders.add(cue.order())) {
                throw new IllegalArgumentException("Ordem de cues inválida.");
            }
        }
    }

    public boolean complete() {
        return cues.stream().allMatch(TranslationCue::translated);
    }

    private static String requireText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " não pode ser vazio.");
        return normalized;
    }
}
