package br.com.immersionhub.generator.desktop.preparation;

import java.util.Optional;

public interface PreparedMaterialRepository {
    Optional<PreparedMaterial> load(String id);
    void save(PreparedMaterial material) throws Exception;
}
