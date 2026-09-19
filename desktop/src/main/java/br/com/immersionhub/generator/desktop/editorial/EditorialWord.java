package br.com.immersionhub.generator.desktop.editorial;

import java.util.Objects;

public record EditorialWord(
    int index,
    String originalEn,
    String approvedEn,
    String pt,
    Long startMs,
    Long endMs,
    Double confidence,
    String semanticGroupId,
    SemanticGroupRole semanticGroupRole,
    EditorialReviewStatus reviewStatus
) {
    public EditorialWord {
        if (index <= 0) throw new IllegalArgumentException("index inválido.");

        originalEn = originalEn == null ? "" : originalEn.trim();
        approvedEn = requireText(approvedEn, "approvedEn");
        pt = pt == null ? "" : pt.trim();

        if ((startMs == null) != (endMs == null)) {
            throw new IllegalArgumentException("Timing de word deve estar completo ou ausente.");
        }
        if (startMs != null && (startMs < 0 || endMs <= startMs)) {
            throw new IllegalArgumentException("Timing de word inválido.");
        }
        if (startMs == null && confidence != null) {
            throw new IllegalArgumentException("Word sem timing não pode herdar confidence.");
        }
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

    public boolean timed() {
        return startMs != null && endMs != null;
    }

    public EditorialWord withReview(
        String nextApprovedEn,
        String nextPt,
        EditorialReviewStatus nextStatus
    ) {
        return new EditorialWord(
            index,
            originalEn,
            nextApprovedEn,
            nextPt,
            startMs,
            endMs,
            confidence,
            semanticGroupId,
            semanticGroupRole,
            Objects.requireNonNull(nextStatus, "nextStatus")
        );
    }

    private static String requireText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " não pode ser vazio.");
        return normalized;
    }
}
