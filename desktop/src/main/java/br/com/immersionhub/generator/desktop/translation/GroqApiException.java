package br.com.immersionhub.generator.desktop.translation;

import java.io.IOException;

public final class GroqApiException extends IOException {
    private final int statusCode;
    private final GroqRateSnapshot rate;

    public GroqApiException(int statusCode, String message, GroqRateSnapshot rate) {
        super(message);
        this.statusCode = statusCode;
        this.rate = rate == null ? GroqRateSnapshot.empty() : rate;
    }

    public int statusCode() {
        return statusCode;
    }

    public GroqRateSnapshot rate() {
        return rate;
    }

    public boolean retryable() {
        return statusCode == 429 || statusCode == 408 || statusCode >= 500;
    }
}
