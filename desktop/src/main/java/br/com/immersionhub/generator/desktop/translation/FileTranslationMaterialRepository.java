package br.com.immersionhub.generator.desktop.translation;

import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public final class FileTranslationMaterialRepository implements TranslationMaterialRepository {
    private final Path root;
    private final TranslationDocumentCodec codec = new TranslationDocumentCodec();

    public FileTranslationMaterialRepository(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public Optional<TranslationMaterial> load(TranslationMaterial expectedBase) {
        Path file = root.resolve("translation-material.json");
        if (!Files.isRegularFile(file)) return Optional.empty();
        try {
            String json = Files.readString(file);
            return Optional.of(codec.parseAndValidate(json, expectedBase, TranslationSource.GROQ));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    @Override
    public void save(TranslationMaterial material) throws Exception {
        Files.createDirectories(root);
        Path temporary = root.resolve("translation-material.tmp.json");
        Path target = root.resolve("translation-material.json");
        Files.writeString(temporary, codec.toJson(material));
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public Path materialPath() {
        return root.resolve("translation-material.json");
    }
}
