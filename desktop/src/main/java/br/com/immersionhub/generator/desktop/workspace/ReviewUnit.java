package br.com.immersionhub.generator.desktop.workspace;

import java.util.List;
import java.util.Objects;

public record ReviewUnit(
    String id,
    List<Integer> wordIndexes,
    String approvedEn,
    String pt,
    TemporalReview timing
) {
    public ReviewUnit {
        id = require(id, "id");
        wordIndexes = List.copyOf(Objects.requireNonNull(wordIndexes, "wordIndexes"));
        approvedEn = require(approvedEn, "approvedEn");
        pt = pt == null ? "" : pt.trim();
        timing = Objects.requireNonNull(timing, "timing");

        if (wordIndexes.isEmpty()) throw new IllegalArgumentException("Unidade sem words.");
        for (int i = 0; i < wordIndexes.size(); i++) {
            int index = wordIndexes.get(i);
            if (index <= 0) throw new IllegalArgumentException("Índice de word inválido.");
            if (i > 0 && index != wordIndexes.get(i - 1) + 1) {
                throw new IllegalArgumentException("Unidade deve conter words contíguas.");
            }
        }
    }

    public boolean grouped() {
        return wordIndexes.size() > 1;
    }

    private static String require(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " não pode ser vazio.");
        return normalized;
    }
}
