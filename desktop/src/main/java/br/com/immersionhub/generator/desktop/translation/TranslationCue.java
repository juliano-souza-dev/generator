package br.com.immersionhub.generator.desktop.translation;

import br.com.immersionhub.generator.desktop.preparation.TimedText;

import java.util.List;
import java.util.Objects;

public record TranslationCue(
    int order,
    long speechStartMs,
    long speechEndMs,
    long subtitleStartMs,
    long subtitleEndMs,
    String speaker,
    String originalEn,
    String approvedEn,
    String pt,
    List<TimedText> words
) {
    public TranslationCue {
        if (order <= 0) throw new IllegalArgumentException("order inválido.");
        if (speechStartMs < 0 || speechEndMs <= speechStartMs) {
            throw new IllegalArgumentException("Timing de fala inválido.");
        }
        if (subtitleStartMs < 0 || subtitleEndMs <= subtitleStartMs) {
            throw new IllegalArgumentException("Timing de legenda inválido.");
        }
        speaker = speaker == null ? "" : speaker;
        originalEn = requireText(originalEn, "originalEn");
        approvedEn = requireText(approvedEn, "approvedEn");
        pt = pt == null ? "" : pt.trim();
        words = List.copyOf(Objects.requireNonNull(words, "words"));
    }

    public TranslationCue withPt(String translation) {
        return new TranslationCue(
            order,
            speechStartMs,
            speechEndMs,
            subtitleStartMs,
            subtitleEndMs,
            speaker,
            originalEn,
            approvedEn,
            requireText(translation, "pt"),
            words
        );
    }

    public boolean translated() {
        return !pt.isBlank();
    }

    private static String requireText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " não pode ser vazio.");
        return normalized;
    }
}
