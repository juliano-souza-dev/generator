package br.com.immersionhub.generator.desktop.timing;

public final class TimingSelection {
    private final long mediaDurationMs;
    private long startMs;
    private long endMs;

    public TimingSelection(long mediaDurationMs) {
        if (mediaDurationMs <= 1) {
            throw new IllegalArgumentException("A mídia precisa ter duração válida.");
        }
        this.mediaDurationMs = mediaDurationMs;
        this.startMs = 0;
        this.endMs = Math.min(mediaDurationMs, 30_000);
        if (this.endMs <= this.startMs) this.endMs = mediaDurationMs;
    }

    public long mediaDurationMs() { return mediaDurationMs; }
    public long startMs() { return startMs; }
    public long endMs() { return endMs; }
    public long durationMs() { return endMs - startMs; }

    public void setStartMs(long value) {
        startMs = clamp(value, 0, endMs - 1);
    }

    public void setEndMs(long value) {
        endMs = clamp(value, startMs + 1, mediaDurationMs);
    }

    public void setRange(long start, long end) {
        if (start < 0 || end <= start || end > mediaDurationMs) {
            throw new IllegalArgumentException("Use 0 <= IN < OUT <= duração da mídia.");
        }
        startMs = start;
        endMs = end;
    }

    public void mark(Boundary boundary, long playheadMs) {
        if (boundary == Boundary.IN) setStartMs(playheadMs);
        else setEndMs(playheadMs);
    }

    public void nudge(Boundary boundary, long deltaMs) {
        if (boundary == Boundary.IN) setStartMs(startMs + deltaMs);
        else setEndMs(endMs + deltaMs);
    }

    private static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }
}
