package br.com.immersionhub.generator.desktop.editorial;

import java.util.List;
import java.util.Objects;

public record EditorialCue(
    int order,
    long speechStartMs,
    long speechEndMs,
    long subtitleStartMs,
    long subtitleEndMs,
    String speaker,
    String originalEn,
    String approvedEn,
    String pt,
    EditorialReviewStatus reviewStatus,
    List<EditorialWord> words
) {
    public EditorialCue {
        if (order <= 0) throw new IllegalArgumentException("order inválido.");
        if (speechStartMs < 0 || speechEndMs <= speechStartMs) {
            throw new IllegalArgumentException("Timing de fala inválido.");
        }
        if (subtitleStartMs < 0 || subtitleEndMs <= subtitleStartMs) {
            throw new IllegalArgumentException("Timing de legenda inválido.");
        }

        speaker = speaker == null ? "" : speaker.trim();
        originalEn = requireText(originalEn, "originalEn");
        approvedEn = requireText(approvedEn, "approvedEn");
        pt = requireText(pt, "pt");
        reviewStatus = Objects.requireNonNull(reviewStatus, "reviewStatus");
        words = List.copyOf(Objects.requireNonNull(words, "words"));

        for (int index = 0; index < words.size(); index++) {
            if (words.get(index).index() != index + 1) {
                throw new IllegalArgumentException("Ordem de words inválida na cue " + order + ".");
            }
        }
    }

    public EditorialCue withReview(String nextApprovedEn, String nextPt, EditorialReviewStatus nextStatus) {
        return withReviewAndWords(nextApprovedEn, nextPt, nextStatus, words);
    }

    public EditorialCue withReviewAndWords(
        String nextApprovedEn,
        String nextPt,
        EditorialReviewStatus nextStatus,
        List<EditorialWord> nextWords
    ) {
        return new EditorialCue(
            order,
            speechStartMs,
            speechEndMs,
            subtitleStartMs,
            subtitleEndMs,
            speaker,
            originalEn,
            nextApprovedEn,
            nextPt,
            Objects.requireNonNull(nextStatus, "nextStatus"),
            nextWords
        );
    }

    private static String requireText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " não pode ser vazio.");
        return normalized;
    }
}
