package br.com.immersionhub.generator.desktop.editorial;

import br.com.immersionhub.generator.desktop.translation.TranslationMaterial;

import java.util.Optional;

public interface EditorialMaterialRepository {
    Optional<EditorialMaterial> load(TranslationMaterial expectedTranslation) throws Exception;
    void save(EditorialMaterial material) throws Exception;
}
