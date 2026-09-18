package br.com.immersionhub.generator.desktop.ui;

import br.com.immersionhub.generator.desktop.timing.Boundary;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

public final class WaveformPane extends Region {
    private final Canvas canvas = new Canvas();
    private List<Double> waveform = List.of();
    private long durationMs = 1;
    private long startMs = 0;
    private long endMs = 1;
    private long playheadMs = 0;
    private double zoom = 1.0;
    private long viewStartMs = 0;
    private Boundary dragging;
    private BiConsumer<Long, Long> rangeListener = (start, end) -> {};
    private LongConsumer seekListener = value -> {};
    private Consumer<Boundary> boundaryListener = boundary -> {};

    public WaveformPane() {
        getChildren().add(canvas);
        setMinHeight(220);
        setPrefHeight(220);
        setFocusTraversable(true);

        widthProperty().addListener((obs, oldValue, newValue) -> redraw());
        heightProperty().addListener((obs, oldValue, newValue) -> redraw());

        setOnMousePressed(event -> {
            requestFocus();
            double inX = msToX(startMs);
            double outX = msToX(endMs);
            if (Math.abs(event.getX() - inX) <= 12) {
                dragging = Boundary.IN;
                boundaryListener.accept(Boundary.IN);
            } else if (Math.abs(event.getX() - outX) <= 12) {
                dragging = Boundary.OUT;
                boundaryListener.accept(Boundary.OUT);
            } else {
                long value = xToMs(event.getX());
                playheadMs = value;
                seekListener.accept(value);
                redraw();
            }
        });

        setOnMouseDragged(event -> {
            if (dragging == null) return;
            long value = xToMs(event.getX());
            if (dragging == Boundary.IN) {
                startMs = Math.max(0, Math.min(value, endMs - 1));
            } else {
                endMs = Math.max(startMs + 1, Math.min(value, durationMs));
            }
            rangeListener.accept(startMs, endMs);
            redraw();
        });

        setOnMouseReleased(event -> dragging = null);

        setOnScroll(event -> {
            if (zoom <= 1.0) return;
            long visible = visibleDurationMs();
            long maxStart = Math.max(0, durationMs - visible);
            long delta = Math.round(visible * 0.12 * Math.signum(event.getDeltaY()));
            viewStartMs = clamp(viewStartMs - delta, 0, maxStart);
            redraw();
            event.consume();
        });
    }

    public void setWaveform(List<Double> waveform, long durationMs) {
        this.waveform = waveform == null ? List.of() : List.copyOf(waveform);
        this.durationMs = Math.max(1, durationMs);
        this.endMs = Math.min(this.endMs, this.durationMs);
        redraw();
    }

    public void setRange(long startMs, long endMs) {
        this.startMs = clamp(startMs, 0, durationMs - 1);
        this.endMs = clamp(endMs, this.startMs + 1, durationMs);
        redraw();
    }

    public void setPlayheadMs(long playheadMs) {
        this.playheadMs = clamp(playheadMs, 0, durationMs);
        keepPlayheadVisible();
        redraw();
    }

    public void setZoom(double zoom) {
        this.zoom = Math.max(1.0, Math.min(8.0, zoom));
        long visible = visibleDurationMs();
        viewStartMs = clamp(playheadMs - visible / 2, 0, Math.max(0, durationMs - visible));
        redraw();
    }

    public void onRangeChanged(BiConsumer<Long, Long> listener) {
        this.rangeListener = listener == null ? (start, end) -> {} : listener;
    }

    public void onSeek(LongConsumer listener) {
        this.seekListener = listener == null ? value -> {} : listener;
    }

    public void onBoundarySelected(Consumer<Boundary> listener) {
        this.boundaryListener = listener == null ? boundary -> {} : listener;
    }

    @Override
    protected void layoutChildren() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        redraw();
    }

    private void redraw() {
        double width = Math.max(1, getWidth());
        double height = Math.max(1, getHeight());
        canvas.setWidth(width);
        canvas.setHeight(height);

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#10151d"));
        gc.fillRect(0, 0, width, height);

        long viewEnd = viewStartMs + visibleDurationMs();
        if (!waveform.isEmpty()) {
            int first = (int) Math.floor((viewStartMs / (double) durationMs) * waveform.size());
            int last = (int) Math.ceil((viewEnd / (double) durationMs) * waveform.size());
            first = Math.max(0, Math.min(waveform.size() - 1, first));
            last = Math.max(first + 1, Math.min(waveform.size(), last));
            int count = last - first;

            gc.setStroke(Color.web("#6e8fb9"));
            gc.setLineWidth(1.0);
            double centerY = height / 2.0;
            for (int i = 0; i < count; i++) {
                double x = i / (double) Math.max(1, count - 1) * width;
                double amplitude = waveform.get(first + i);
                double bar = amplitude * height * 0.44;
                gc.strokeLine(x, centerY - bar, x, centerY + bar);
            }
        }

        drawMarker(gc, msToX(startMs), "#44e5a0", "IN", height);
        drawMarker(gc, msToX(endMs), "#ff9e64", "OUT", height);

        double playX = msToX(playheadMs);
        if (playX >= 0 && playX <= width) {
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1.0);
            gc.strokeLine(playX, 0, playX, height);
        }
    }

    private void drawMarker(GraphicsContext gc, double x, String color, String label, double height) {
        if (x < 0 || x > getWidth()) return;
        gc.setStroke(Color.web(color));
        gc.setFill(Color.web(color));
        gc.setLineWidth(2.0);
        gc.strokeLine(x, 0, x, height);
        gc.fillText(label, Math.max(4, Math.min(getWidth() - 28, x + 4)), 16);
    }

    private double msToX(long ms) {
        long visible = visibleDurationMs();
        return ((ms - viewStartMs) / (double) visible) * Math.max(1, getWidth());
    }

    private long xToMs(double x) {
        double ratio = Math.max(0.0, Math.min(1.0, x / Math.max(1, getWidth())));
        return clamp(viewStartMs + Math.round(ratio * visibleDurationMs()), 0, durationMs);
    }

    private long visibleDurationMs() {
        return Math.max(1, Math.round(durationMs / zoom));
    }

    private void keepPlayheadVisible() {
        long visible = visibleDurationMs();
        if (playheadMs < viewStartMs) viewStartMs = playheadMs;
        if (playheadMs > viewStartMs + visible) viewStartMs = playheadMs - visible;
        viewStartMs = clamp(viewStartMs, 0, Math.max(0, durationMs - visible));
    }

    private static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }
}
