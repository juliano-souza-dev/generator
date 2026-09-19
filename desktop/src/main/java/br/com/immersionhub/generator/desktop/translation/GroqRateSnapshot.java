package br.com.immersionhub.generator.desktop.translation;

import java.time.Duration;

public record GroqRateSnapshot(
    Long remainingRequests,
    Long remainingTokens,
    Duration resetRequests,
    Duration resetTokens,
    Duration retryAfter
) {
    public static GroqRateSnapshot empty() {
        return new GroqRateSnapshot(null, null, null, null, null);
    }
}
