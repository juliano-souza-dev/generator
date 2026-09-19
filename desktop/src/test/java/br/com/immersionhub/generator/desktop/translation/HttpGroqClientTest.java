package br.com.immersionhub.generator.desktop.translation;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class HttpGroqClientTest {
    @Test
    void parsesGroqResetDurationsAndRetryAfter() {
        assertEquals(Duration.ofMillis(7660), HttpGroqClient.parseDuration("7.66s"));
        assertEquals(Duration.ofMillis(179560), HttpGroqClient.parseDuration("2m59.56s"));
        assertEquals(Duration.ofSeconds(2), HttpGroqClient.parseDuration("2"));
        assertEquals(Duration.ofMillis(250), HttpGroqClient.parseDuration("250ms"));
        assertNull(HttpGroqClient.parseDuration("n/a"));
    }
}
