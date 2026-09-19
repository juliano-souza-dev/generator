package br.com.immersionhub.generator.desktop.translation;

import java.util.Optional;

public interface TranslationMaterialRepository {
    Optional<TranslationMaterial> load(TranslationMaterial expectedBase);
    void save(TranslationMaterial material) throws Exception;
}
