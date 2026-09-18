package br.com.immersionhub.generator.desktop.preparation;

import java.util.Optional;

public interface AlignedMaterialRepository {
    Optional<AlignedMaterial> load(String id, PreparedMaterial preparedMaterial);
    void save(AlignedMaterial material) throws Exception;
}
