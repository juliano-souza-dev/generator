package br.com.immersionhub.generator.desktop.preparation;

import java.util.Objects;

public record TimedText(String text, long startMs, long endMs, Double confidence) {
    public TimedText(String text, long startMs, long endMs) {
        this(text, startMs, endMs, null);
    }

    public TimedText {
        text = Objects.requireNonNull(text, "text").trim();
        if (text.isEmpty()) throw new IllegalArgumentException("text não pode ser vazio.");
        if (startMs < 0 || endMs <= startMs) throw new IllegalArgumentException("Timing inválido.");
        if (confidence != null && (confidence < 0.0 || confidence > 1.0 || confidence.isNaN())) {
            throw new IllegalArgumentException("Confidence inválida.");
        }
    }
}
