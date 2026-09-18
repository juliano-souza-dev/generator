package br.com.immersionhub.generator.desktop.preparation;

import java.util.Objects;

public record TimedText(String text, long startMs, long endMs) {
    public TimedText {
        text = Objects.requireNonNull(text, "text").trim();
        if (text.isEmpty()) throw new IllegalArgumentException("text não pode ser vazio.");
        if (startMs < 0 || endMs <= startMs) throw new IllegalArgumentException("Timing inválido.");
    }
}
