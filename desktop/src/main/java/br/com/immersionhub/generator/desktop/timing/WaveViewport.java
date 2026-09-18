package br.com.immersionhub.generator.desktop.timing;

public final class WaveViewport {
    private final long durationMs;
    private double zoom = 1.0;
    private long viewStartMs = 0;

    public WaveViewport(long durationMs) {
        if (durationMs <= 0) {
            throw new IllegalArgumentException("A duração da viewport deve ser positiva.");
        }
        this.durationMs = durationMs;
    }

    public long durationMs() { return durationMs; }
    public double zoom() { return zoom; }
    public long viewStartMs() { return viewStartMs; }
    public long visibleDurationMs() {
        return Math.max(1, Math.round(durationMs / zoom));
    }

    public long viewEndMs() {
        return Math.min(durationMs, viewStartMs + visibleDurationMs());
    }

    public void setZoom(double value, long anchorMs) {
        zoom = Math.max(1.0, Math.min(8.0, value));
        long visible = visibleDurationMs();
        viewStartMs = clamp(anchorMs - visible / 2, 0, Math.max(0, durationMs - visible));
    }

    public void panByFraction(double fraction) {
        if (zoom <= 1.0) {
            viewStartMs = 0;
            return;
        }
        long visible = visibleDurationMs();
        long maxStart = Math.max(0, durationMs - visible);
        long delta = Math.round(visible * fraction);
        viewStartMs = clamp(viewStartMs + delta, 0, maxStart);
    }

    public void keepVisible(long playheadMs) {
        long playhead = clamp(playheadMs, 0, durationMs);
        long visible = visibleDurationMs();
        if (playhead < viewStartMs) viewStartMs = playhead;
        if (playhead > viewStartMs + visible) viewStartMs = playhead - visible;
        viewStartMs = clamp(viewStartMs, 0, Math.max(0, durationMs - visible));
    }

    public long xToMs(double x, double width) {
        double safeWidth = Math.max(1.0, width);
        double ratio = Math.max(0.0, Math.min(1.0, x / safeWidth));
        return clamp(viewStartMs + Math.round(ratio * visibleDurationMs()), 0, durationMs);
    }

    public double msToX(long ms, double width) {
        double safeWidth = Math.max(1.0, width);
        return ((ms - viewStartMs) / (double) visibleDurationMs()) * safeWidth;
    }

    public long clampIn(long candidate, long outMs) {
        return clamp(candidate, 0, Math.max(0, outMs - 1));
    }

    public long clampOut(long candidate, long inMs) {
        return clamp(candidate, Math.min(durationMs, inMs + 1), durationMs);
    }

    private static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }
}
