package br.com.immersionhub.generator.desktop.timing;

public record TimingRange(long startMs, long endMs) {
    public TimingRange {
        if (startMs < 0 || endMs <= startMs) {
            throw new IllegalArgumentException("Intervalo de timing inválido.");
        }
    }

    public long durationMs() {
        return endMs - startMs;
    }
}
