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
            TranslationSource source = readSource();
            return Optional.of(codec.parseAndValidate(json, expectedBase, source));
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

        Path sourceTemporary = root.resolve("translation-source.tmp");
        Path sourceTarget = root.resolve("translation-source.txt");
        Files.writeString(sourceTemporary, material.source().name());
        try {
            Files.move(sourceTemporary, sourceTarget, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(sourceTemporary, sourceTarget, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private TranslationSource readSource() {
        Path source = root.resolve("translation-source.txt");
        if (!Files.isRegularFile(source)) return TranslationSource.GROQ;
        try {
            return TranslationSource.valueOf(Files.readString(source).trim());
        } catch (Exception ignored) {
            return TranslationSource.GROQ;
        }
    }

    public Path materialPath() {
        return root.resolve("translation-material.json");
    }
}
