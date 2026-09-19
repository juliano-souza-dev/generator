package br.com.immersionhub.generator.desktop.translation;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GroqRateControllerTest {
    @Test
    void waitsForTokenResetBeforeSendingBeyondRemainingBudget() throws Exception {
        List<Duration> waits = new ArrayList<>();
        GroqRateController controller = new GroqRateController(waits::add);
        controller.observe(new GroqRateSnapshot(
            10L,
            100L,
            Duration.ofSeconds(10),
            Duration.ofMillis(750),
            null
        ));

        controller.beforeRequest(200L);

        assertEquals(1, waits.size());
        assertEquals(Duration.ofMillis(900), waits.getFirst());
    }

    @Test
    void honorsRetryAfterFor429InsteadOfBlindBackoff() throws Exception {
        List<Duration> waits = new ArrayList<>();
        GroqRateController controller = new GroqRateController(waits::add);
        GroqApiException failure = new GroqApiException(
            429,
            "rate",
            new GroqRateSnapshot(null, null, null, null, Duration.ofSeconds(2))
        );

        controller.waitAfterFailure(failure, 0);

        assertEquals(List.of(Duration.ofMillis(2150)), waits);
        assertTrue(failure.retryable());
        assertFalse(new GroqApiException(401, "auth", GroqRateSnapshot.empty()).retryable());
        assertFalse(new GroqApiException(400, "contract", GroqRateSnapshot.empty()).retryable());
    }
}
