package br.com.immersionhub.generator.desktop.editorial;

public record WordReviewPosition(int cueOrder, int wordIndex) {
    public WordReviewPosition {
        if (cueOrder <= 0) throw new IllegalArgumentException("cueOrder inválido.");
        if (wordIndex <= 0) throw new IllegalArgumentException("wordIndex inválido.");
    }
}
