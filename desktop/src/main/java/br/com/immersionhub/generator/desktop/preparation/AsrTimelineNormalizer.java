package br.com.immersionhub.generator.desktop.preparation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class AsrTimelineNormalizer {
    public static final long END_BOUNDARY_TOLERANCE_MS = 25L;

    private AsrTimelineNormalizer() {}

    public static AsrResult normalize(AsrResult result, long durationMs) {
        Objects.requireNonNull(result, "result");
        if (durationMs <= 0) throw new IllegalArgumentException("durationMs deve ser positivo.");

        return new AsrResult(
            result.language(),
            result.text(),
            normalizeItems(result.segments(), durationMs, "segment"),
            normalizeItems(result.words(), durationMs, "word")
        );
    }

    private static List<TimedText> normalizeItems(
        List<TimedText> items,
        long durationMs,
        String kind
    ) {
        List<TimedText> normalized = new ArrayList<>(items.size());

        for (int index = 0; index < items.size(); index++) {
            TimedText item = items.get(index);
            long start = item.startMs();
            long end = item.endMs();

            if (start >= durationMs) {
                long overshoot = Math.max(start, end) - durationMs;
                if (overshoot <= END_BOUNDARY_TOLERANCE_MS) {
                    continue;
                }
                throw invalid(kind, index, start, end, durationMs);
            }

            if (end > durationMs) {
                long overshoot = end - durationMs;
                if (overshoot > END_BOUNDARY_TOLERANCE_MS) {
                    throw invalid(kind, index, start, end, durationMs);
                }
                end = durationMs;
            }

            if (end <= start) {
                throw invalid(kind, index, start, end, durationMs);
            }

            normalized.add(new TimedText(item.text(), start, end, item.confidence()));
        }

        return List.copyOf(normalized);
    }

    private static IllegalArgumentException invalid(
        String kind,
        int index,
        long start,
        long end,
        long durationMs
    ) {
        return new IllegalArgumentException(
            "Timing " + kind + "[" + index + "] inválido: startMs=" + start
                + ", endMs=" + end + ", durationMs=" + durationMs + "."
        );
    }
}
