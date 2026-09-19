package br.com.immersionhub.generator.desktop.translation;

import java.util.Objects;
import java.util.Optional;

public final class GroqSettingsService {
    private final GroqCredentialStore store;
    private final GroqKeyValidator validator;

    public GroqSettingsService(GroqCredentialStore store, GroqKeyValidator validator) {
        this.store = Objects.requireNonNull(store, "store");
        this.validator = Objects.requireNonNull(validator, "validator");
    }

    public Optional<String> apiKey() {
        try {
            return store.load().map(String::trim).filter(value -> !value.isEmpty());
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    public boolean hasApiKey() {
        return apiKey().isPresent();
    }

    public void validateAndSave(String apiKey) throws Exception {
        String normalized = apiKey == null ? "" : apiKey.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException("Informe uma chave Groq.");
        validator.validate(normalized);
        store.save(normalized);
    }

    public void clear() throws Exception {
        store.clear();
    }
}
