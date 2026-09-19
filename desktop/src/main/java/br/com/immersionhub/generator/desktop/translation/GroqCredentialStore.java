package br.com.immersionhub.generator.desktop.translation;

import java.util.Optional;

public interface GroqCredentialStore {
    Optional<String> load() throws Exception;
    void save(String apiKey) throws Exception;
    void clear() throws Exception;
}
