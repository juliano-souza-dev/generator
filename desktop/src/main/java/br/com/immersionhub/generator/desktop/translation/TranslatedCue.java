package br.com.immersionhub.generator.desktop.translation;

import java.util.Objects;

public record TranslatedCue(int order, String pt) {
    public TranslatedCue {
        if (order <= 0) throw new IllegalArgumentException("order inválido.");
        pt = Objects.requireNonNull(pt, "pt").trim();
        if (pt.isEmpty()) throw new IllegalArgumentException("Tradução vazia.");
    }
}
