package br.com.immersionhub.generator.desktop.workspace;

public record TemporalBounds(long startMs, long endMs) {
    public TemporalBounds {
        if (startMs < 0) throw new IllegalArgumentException("IN temporal não pode ser negativo.");
        if (endMs <= startMs) throw new IllegalArgumentException("OUT temporal deve ser maior que IN.");
    }

    public long durationMs() {
        return endMs - startMs;
    }

    public void requireWithin(long mediaDurationMs) {
        if (mediaDurationMs <= 0) throw new IllegalArgumentException("Duração de mídia inválida.");
        if (endMs > mediaDurationMs) {
            throw new IllegalArgumentException("Timing ultrapassa a duração da mídia.");
        }
    }
}
