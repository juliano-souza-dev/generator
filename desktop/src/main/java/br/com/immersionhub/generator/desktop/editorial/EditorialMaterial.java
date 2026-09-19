package br.com.immersionhub.generator.desktop.editorial;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record EditorialMaterial(
    String id,
    String translationMaterialId,
    String schemaVersion,
    List<EditorialCue> cues,
    Instant createdAt
) {
    public static final String SCHEMA_VERSION = "1.0";

    public EditorialMaterial {
        translationMaterialId = requireText(translationMaterialId, "translationMaterialId");
        schemaVersion = requireText(schemaVersion, "schemaVersion");
        cues = List.copyOf(Objects.requireNonNull(cues, "cues"));
        createdAt = Objects.requireNonNull(createdAt, "createdAt");

        if (cues.isEmpty()) throw new IllegalArgumentException("Material editorial sem cues.");

        Set<Integer> orders = new HashSet<>();
        for (int index = 0; index < cues.size(); index++) {
            EditorialCue cue = cues.get(index);
            if (cue.order() != index + 1 || !orders.add(cue.order())) {
                throw new IllegalArgumentException("Ordem de cues editoriais inválida.");
            }
        }

        String expectedId = EditorialIds.materialId(translationMaterialId, schemaVersion, cues);
        id = requireText(id, "id");
        if (!expectedId.equals(id)) {
            throw new IllegalArgumentException("Identidade do material editorial não corresponde ao conteúdo.");
        }
    }

    public boolean cuesApproved() {
        return cues.stream().allMatch(cue -> cue.reviewStatus() == EditorialReviewStatus.APPROVED);
    }

    public boolean fullyApproved() {
        return cuesApproved()
            && cues.stream().allMatch(cue ->
                cue.words().stream().allMatch(word -> word.reviewStatus() == EditorialReviewStatus.APPROVED)
            );
    }

    public EditorialMaterial withCues(List<EditorialCue> updatedCues) {
        List<EditorialCue> copy = List.copyOf(Objects.requireNonNull(updatedCues, "updatedCues"));
        String nextId = EditorialIds.materialId(translationMaterialId, schemaVersion, copy);
        return new EditorialMaterial(nextId, translationMaterialId, schemaVersion, copy, createdAt);
    }

    public static EditorialMaterial create(
        String translationMaterialId,
        List<EditorialCue> cues,
        Instant createdAt
    ) {
        String id = EditorialIds.materialId(translationMaterialId, SCHEMA_VERSION, cues);
        return new EditorialMaterial(id, translationMaterialId, SCHEMA_VERSION, cues, createdAt);
    }

    private static String requireText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " não pode ser vazio.");
        return normalized;
    }
}
