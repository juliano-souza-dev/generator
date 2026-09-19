package br.com.immersionhub.generator.desktop.translation;

@FunctionalInterface
public interface GroqKeyValidator {
    void validate(String apiKey) throws Exception;
}
