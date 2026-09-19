package br.com.immersionhub.generator.desktop.translation;

public record GroqModelInfo(
    String id,
    long contextWindow,
    long maxCompletionTokens
) {
    public GroqModelInfo {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Modelo sem id.");
        if (contextWindow <= 0) throw new IllegalArgumentException("contextWindow inválido.");
        if (maxCompletionTokens <= 0) maxCompletionTokens = Math.max(1024, contextWindow / 4);
    }
}
