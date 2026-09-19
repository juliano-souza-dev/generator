package br.com.immersionhub.generator.desktop.preparation;

import java.util.List;
import java.util.Objects;

public record AsrResult(String language, String text, List<TimedText> segments, List<TimedText> words) {
    public AsrResult {
        language = Objects.requireNonNull(language, "language").trim();
        text = Objects.requireNonNull(text, "text").trim();
        segments = List.copyOf(Objects.requireNonNull(segments, "segments"));
        words = List.copyOf(Objects.requireNonNull(words, "words"));
        if (!"en".equalsIgnoreCase(language)) throw new IllegalArgumentException("ASR deve produzir inglês.");
        if (text.isEmpty()) throw new IllegalArgumentException("Transcrição vazia.");
    }

    public void validate(long durationMs) {
        validateTimeline("segment", segments, durationMs);
        validateTimeline("word", words, durationMs);
    }

    private static void validateTimeline(String kind, List<TimedText> items, long durationMs) {
        long previousStart = -1;
        for (int index = 0; index < items.size(); index++) {
            TimedText item = items.get(index);
            if (item.endMs() > durationMs || item.startMs() < previousStart) {
                throw new IllegalArgumentException(
                    "Timing " + kind + "[" + index + "] inválido: startMs=" + item.startMs()
                        + ", endMs=" + item.endMs() + ", durationMs=" + durationMs
                        + ", previousStartMs=" + previousStart + "."
                );
            }
            previousStart = item.startMs();
        }
    }
}
