package br.com.immersionhub.generator.desktop.translation;

import java.time.Duration;
import java.util.Objects;

public final class GroqRateController {
    @FunctionalInterface
    public interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }

    private final Sleeper sleeper;
    private GroqRateSnapshot last = GroqRateSnapshot.empty();

    public GroqRateController() {
        this(duration -> Thread.sleep(Math.max(0L, duration.toMillis())));
    }

    GroqRateController(Sleeper sleeper) {
        this.sleeper = Objects.requireNonNull(sleeper);
    }

    public synchronized void observe(GroqRateSnapshot snapshot) {
        last = snapshot == null ? GroqRateSnapshot.empty() : snapshot;
    }

    public synchronized void beforeRequest(long estimatedTokens) throws InterruptedException {
        Duration wait = null;

        if (last.remainingRequests() != null
            && last.remainingRequests() <= 0
            && last.resetRequests() != null) {
            wait = max(wait, last.resetRequests());
        }
        if (last.remainingTokens() != null
            && last.remainingTokens() < estimatedTokens
            && last.resetTokens() != null) {
            wait = max(wait, last.resetTokens());
        }

        if (wait != null && !wait.isNegative() && !wait.isZero()) {
            sleeper.sleep(wait.plusMillis(150));
            last = GroqRateSnapshot.empty();
        }
    }

    public void waitAfterFailure(GroqApiException exception, int attempt) throws InterruptedException {
        Duration retryAfter = exception.rate().retryAfter();
        if (retryAfter != null && !retryAfter.isNegative() && !retryAfter.isZero()) {
            sleeper.sleep(retryAfter.plusMillis(150));
            return;
        }
        long millis = Math.min(8_000L, 500L * (1L << Math.min(attempt, 4)));
        sleeper.sleep(Duration.ofMillis(millis));
    }

    private static Duration max(Duration first, Duration second) {
        if (first == null) return second;
        return first.compareTo(second) >= 0 ? first : second;
    }
}
