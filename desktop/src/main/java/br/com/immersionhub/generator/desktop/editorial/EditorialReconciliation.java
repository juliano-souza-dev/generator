package br.com.immersionhub.generator.desktop.editorial;

import java.util.Objects;

public record EditorialReconciliation(
    String sourceMaterialId,
    int cueOrder,
    EditorialReconciliationReason reason,
    int preservedWords,
    int insertedWords,
    int removedWords,
    int invalidatedGroups
) {
    public EditorialReconciliation {
        sourceMaterialId = Objects.requireNonNull(sourceMaterialId, "sourceMaterialId").trim();
        if (sourceMaterialId.isEmpty()) throw new IllegalArgumentException("sourceMaterialId vazio.");
        if (cueOrder <= 0) throw new IllegalArgumentException("cueOrder inválido.");
        reason = Objects.requireNonNull(reason, "reason");
        if (preservedWords < 0 || insertedWords < 0 || removedWords < 0 || invalidatedGroups < 0) {
            throw new IllegalArgumentException("Contadores de reconciliação inválidos.");
        }
    }
}
