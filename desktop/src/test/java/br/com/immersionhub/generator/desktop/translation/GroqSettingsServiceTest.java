package br.com.immersionhub.generator.desktop.translation;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GroqSettingsServiceTest {
    @Test
    void validKeyIsStoredGloballyAfterValidation() throws Exception {
        MemoryStore store = new MemoryStore();
        AtomicReference<String> validated = new AtomicReference<>();
        GroqSettingsService service = new GroqSettingsService(store, validated::set);

        service.validateAndSave("  gsk_example  ");

        assertEquals("gsk_example", validated.get());
        assertEquals(Optional.of("gsk_example"), service.apiKey());
    }

    @Test
    void invalidKeyIsNeverPersisted() {
        MemoryStore store = new MemoryStore();
        GroqSettingsService service = new GroqSettingsService(store, key -> {
            throw new IllegalArgumentException("invalid");
        });

        assertThrows(Exception.class, () -> service.validateAndSave("bad-key"));
        assertTrue(service.apiKey().isEmpty());
    }

    @Test
    void storeIsNotScopedByProject() throws Exception {
        MemoryStore global = new MemoryStore();
        GroqSettingsService firstProject = new GroqSettingsService(global, key -> {});
        GroqSettingsService secondProject = new GroqSettingsService(global, key -> {});

        firstProject.validateAndSave("gsk_global");

        assertEquals(Optional.of("gsk_global"), secondProject.apiKey());
    }

    private static final class MemoryStore implements GroqCredentialStore {
        private String value;

        @Override
        public Optional<String> load() {
            return Optional.ofNullable(value);
        }

        @Override
        public void save(String apiKey) {
            value = apiKey;
        }

        @Override
        public void clear() {
            value = null;
        }
    }
}
