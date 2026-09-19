package br.com.immersionhub.generator.desktop.editorial;

import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class WordReviewCursorRepository {
    private final Path root;

    public WordReviewCursorRepository(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    public WordReviewPosition load(EditorialMaterial material) {
        WordReviewPosition fallback = firstPosition(material);
        Path file = root.resolve("current-word.txt");
        if (!Files.isRegularFile(file)) return fallback;

        try {
            String[] parts = Files.readString(file).trim().split(":");
            if (parts.length != 2) return fallback;
            WordReviewPosition position = new WordReviewPosition(
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1])
            );
            return valid(material, position) ? position : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public void save(EditorialMaterial material, WordReviewPosition position) throws Exception {
        if (!valid(material, position)) {
            throw new IllegalArgumentException("Posição de revisão inválida.");
        }
        Files.createDirectories(root);
        Path temporary = root.resolve("current-word.tmp");
        Path target = root.resolve("current-word.txt");
        Files.writeString(temporary, position.cueOrder() + ":" + position.wordIndex());
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    static boolean valid(EditorialMaterial material, WordReviewPosition position) {
        if (position.cueOrder() < 1 || position.cueOrder() > material.cues().size()) return false;
        EditorialCue cue = material.cues().get(position.cueOrder() - 1);
        return position.wordIndex() >= 1 && position.wordIndex() <= cue.words().size();
    }

    static WordReviewPosition firstPosition(EditorialMaterial material) {
        for (EditorialCue cue : material.cues()) {
            if (!cue.words().isEmpty()) return new WordReviewPosition(cue.order(), 1);
        }
        throw new IllegalArgumentException("Material editorial sem words.");
    }
}
