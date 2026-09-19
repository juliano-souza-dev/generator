package br.com.immersionhub.generator.desktop.editorial;

import java.util.Objects;

public record EditorialWord(
    int index,
    String originalEn,
    String approvedEn,
    String pt,
    long startMs,
    long endMs,
    Double confidence,
    String semanticGroupId,
    SemanticGroupRole semanticGroupRole,
    EditorialReviewStatus reviewStatus
) {
    public EditorialWord {
        if (index <= 0) throw new IllegalArgumentException("index inválido.");
        originalEn = requireText(originalEn, "originalEn");
        approvedEn = requireText(approvedEn, "approvedEn");
        pt = pt == null ? "" : pt.trim();
        if (startMs < 0 || endMs <= startMs) throw new IllegalArgumentException("Timing de word inválido.");
        if (confidence != null && (confidence.isNaN() || confidence < 0d || confidence > 1d)) {
            throw new IllegalArgumentException("Confidence inválida.");
        }

        semanticGroupId = semanticGroupId == null ? "" : semanticGroupId.trim();
        semanticGroupRole = Objects.requireNonNull(semanticGroupRole, "semanticGroupRole");
        reviewStatus = Objects.requireNonNull(reviewStatus, "reviewStatus");

        if (semanticGroupRole == SemanticGroupRole.NONE && !semanticGroupId.isEmpty()) {
            throw new IllegalArgumentException("Word sem grupo não pode possuir semanticGroupId.");
        }
        if (semanticGroupRole != SemanticGroupRole.NONE && semanticGroupId.isEmpty()) {
            throw new IllegalArgumentException("Word agrupada deve possuir semanticGroupId.");
        }
    }

    private static String requireText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " não pode ser vazio.");
        return normalized;
    }
}
