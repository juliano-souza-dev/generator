package br.com.immersionhub.generator.desktop.translation;

import java.util.Objects;
import java.util.Optional;

public record TranslationOutcome(
    TranslationMaterial baseMaterial,
    TranslationMaterial translatedMaterial,
    ExternalAiPackage externalPackage,
    String fallbackReason
) {
    public TranslationOutcome {
        baseMaterial = Objects.requireNonNull(baseMaterial);
        externalPackage = Objects.requireNonNull(externalPackage);
        fallbackReason = fallbackReason == null ? "" : fallbackReason;
    }

    public boolean translated() {
        return translatedMaterial != null && translatedMaterial.complete();
    }

    public Optional<TranslationMaterial> translatedMaterialOptional() {
        return Optional.ofNullable(translatedMaterial);
    }

    public boolean externalRequired() {
        return !translated();
    }
}
