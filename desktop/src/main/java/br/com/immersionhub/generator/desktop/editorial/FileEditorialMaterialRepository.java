package br.com.immersionhub.generator.desktop.editorial;

import br.com.immersionhub.generator.desktop.translation.TranslationMaterial;

import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public final class FileEditorialMaterialRepository implements EditorialMaterialRepository {
    private final Path root;
    private final EditorialDocumentCodec codec = new EditorialDocumentCodec();

    public FileEditorialMaterialRepository(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public Optional<EditorialMaterial> load(TranslationMaterial expectedTranslation) throws Exception {
        Path file = materialPath();
        if (!Files.isRegularFile(file)) return Optional.empty();
        String json = Files.readString(file);
        return Optional.of(codec.parseAndValidate(json, expectedTranslation));
    }

    @Override
    public void save(EditorialMaterial material) throws Exception {
        Files.createDirectories(root);
        Path temporary = root.resolve("editorial-material.tmp.json");
        Path target = materialPath();
        Files.writeString(temporary, codec.toJson(material));
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public Path materialPath() {
        return root.resolve("editorial-material.json");
    }
}
