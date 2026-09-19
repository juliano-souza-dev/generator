package br.com.immersionhub.generator.desktop.translation;

import java.net.URI;
import java.util.Objects;

public record GroqConfig(
    URI baseUri,
    String apiKey,
    String model,
    int maxRetries
) {
    public static final URI DEFAULT_BASE_URI = URI.create("https://api.groq.com/openai/v1/");
    public static final String DEFAULT_MODEL = "openai/gpt-oss-20b";

    public GroqConfig {
        baseUri = Objects.requireNonNull(baseUri, "baseUri");
        apiKey = Objects.requireNonNull(apiKey, "apiKey").trim();
        model = Objects.requireNonNull(model, "model").trim();
        if (apiKey.isEmpty()) throw new IllegalArgumentException("Chave Groq não informada.");
        if (model.isEmpty()) throw new IllegalArgumentException("Modelo Groq não informado.");
        if (maxRetries < 0 || maxRetries > 5) throw new IllegalArgumentException("maxRetries inválido.");
    }

    public static GroqConfig fromEnvironment() {
        String key = System.getenv("GROQ_API_KEY");
        String model = System.getenv("IHUB_GROQ_MODEL");
        if (model == null || model.isBlank()) model = DEFAULT_MODEL;
        return new GroqConfig(DEFAULT_BASE_URI, key == null ? "" : key, model, 3);
    }
}
