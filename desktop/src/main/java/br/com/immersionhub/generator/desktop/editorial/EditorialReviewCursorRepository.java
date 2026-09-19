package br.com.immersionhub.generator.desktop.editorial;

import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class EditorialReviewCursorRepository {
    private final Path root;

    public EditorialReviewCursorRepository(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    public int load(int cueCount) {
        Path file = root.resolve("current-cue.txt");
        if (!Files.isRegularFile(file)) return 1;
        try {
            int value = Integer.parseInt(Files.readString(file).trim());
            return Math.max(1, Math.min(cueCount, value));
        } catch (Exception ignored) {
            return 1;
        }
    }

    public void save(int cueOrder, int cueCount) throws Exception {
        if (cueOrder < 1 || cueOrder > cueCount) {
            throw new IllegalArgumentException("Cue atual inválida.");
        }
        Files.createDirectories(root);
        Path temporary = root.resolve("current-cue.tmp");
        Path target = root.resolve("current-cue.txt");
        Files.writeString(temporary, Integer.toString(cueOrder));
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
