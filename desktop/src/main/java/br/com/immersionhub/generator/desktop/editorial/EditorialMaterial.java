package br.com.immersionhub.generator.desktop.editorial;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record EditorialMaterial(
    String id,
    String translationMaterialId,
    String schemaVersion,
    List<EditorialCue> cues,
    List<EditorialReconciliation> reconciliations,
    Instant createdAt
) {
    public static final String SCHEMA_VERSION = "1.1";

    public EditorialMaterial(
        String id,
        String translationMaterialId,
        String schemaVersion,
        List<EditorialCue> cues,
        Instant createdAt
    ) {
        this(id, translationMaterialId, schemaVersion, cues, List.of(), createdAt);
    }

    public EditorialMaterial {
        translationMaterialId = requireText(translationMaterialId, "translationMaterialId");
        schemaVersion = requireText(schemaVersion, "schemaVersion");
        cues = List.copyOf(Objects.requireNonNull(cues, "cues"));
        reconciliations = reconciliations == null ? List.of() : List.copyOf(reconciliations);
        createdAt = Objects.requireNonNull(createdAt, "createdAt");

        if (cues.isEmpty()) throw new IllegalArgumentException("Material editorial sem cues.");

        Set<Integer> orders = new HashSet<>();
        for (int index = 0; index < cues.size(); index++) {
            EditorialCue cue = cues.get(index);
            if (cue.order() != index + 1 || !orders.add(cue.order())) {
                throw new IllegalArgumentException("Ordem de cues editoriais inválida.");
            }
        }

        for (EditorialReconciliation reconciliation : reconciliations) {
            if (reconciliation.cueOrder() > cues.size()) {
                throw new IllegalArgumentException("Reconciliação aponta para cue inexistente.");
            }
        }

        String expectedId = EditorialIds.materialId(
            translationMaterialId,
            schemaVersion,
            cues,
            reconciliations
        );
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

    public EditorialMaterial upgradeToCurrentSchema() {
        if (SCHEMA_VERSION.equals(schemaVersion)) return this;
        if (!"1.0".equals(schemaVersion)) {
            throw new IllegalArgumentException("Versão editorial não suportada para atualização.");
        }
        String nextId = EditorialIds.materialId(
            translationMaterialId,
            SCHEMA_VERSION,
            cues,
            reconciliations
        );
        return new EditorialMaterial(
            nextId,
            translationMaterialId,
            SCHEMA_VERSION,
            cues,
            reconciliations,
            createdAt
        );
    }

    public EditorialMaterial withCues(List<EditorialCue> updatedCues) {
        List<EditorialCue> copy = List.copyOf(Objects.requireNonNull(updatedCues, "updatedCues"));
        String nextId = EditorialIds.materialId(
            translationMaterialId,
            schemaVersion,
            copy,
            reconciliations
        );
        return new EditorialMaterial(
            nextId,
            translationMaterialId,
            schemaVersion,
            copy,
            reconciliations,
            createdAt
        );
    }

    public EditorialMaterial withReconciliation(
        List<EditorialCue> updatedCues,
        EditorialReconciliation reconciliation
    ) {
        List<EditorialCue> cueCopy = List.copyOf(Objects.requireNonNull(updatedCues, "updatedCues"));
        List<EditorialReconciliation> history = new ArrayList<>(reconciliations);
        history.add(Objects.requireNonNull(reconciliation, "reconciliation"));
        List<EditorialReconciliation> historyCopy = List.copyOf(history);

        String nextId = EditorialIds.materialId(
            translationMaterialId,
            schemaVersion,
            cueCopy,
            historyCopy
        );
        return new EditorialMaterial(
            nextId,
            translationMaterialId,
            schemaVersion,
            cueCopy,
            historyCopy,
            createdAt
        );
    }

    public static EditorialMaterial create(
        String translationMaterialId,
        List<EditorialCue> cues,
        Instant createdAt
    ) {
        String id = EditorialIds.materialId(translationMaterialId, SCHEMA_VERSION, cues, List.of());
        return new EditorialMaterial(
            id,
            translationMaterialId,
            SCHEMA_VERSION,
            cues,
            List.of(),
            createdAt
        );
    }

    private static String requireText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " não pode ser vazio.");
        return normalized;
    }
}
