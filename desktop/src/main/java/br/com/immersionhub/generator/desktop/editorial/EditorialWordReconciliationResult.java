package br.com.immersionhub.generator.desktop.editorial;

import java.util.List;
import java.util.Objects;

public record EditorialWordReconciliationResult(
    List<EditorialWord> words,
    int preservedWords,
    int insertedWords,
    int removedWords,
    int invalidatedGroups
) {
    public EditorialWordReconciliationResult {
        words = List.copyOf(Objects.requireNonNull(words, "words"));
        if (preservedWords < 0 || insertedWords < 0 || removedWords < 0 || invalidatedGroups < 0) {
            throw new IllegalArgumentException("Contadores de reconciliação inválidos.");
        }
    }
}
