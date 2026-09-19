package br.com.immersionhub.generator.desktop.editorial;

import br.com.immersionhub.generator.desktop.infrastructure.AppDirectories;

import java.nio.file.Path;

public final class EditorialModule {
    private EditorialModule() {}

    public static EditorialReviewService createService(String projectId) {
        Path directory = directory(projectId);
        return new EditorialReviewService(
            new FileEditorialMaterialRepository(directory),
            new EditorialReviewCursorRepository(directory)
        );
    }

    public static WordReviewService createWordReviewService(String projectId) {
        Path directory = directory(projectId);
        return new WordReviewService(
            new FileEditorialMaterialRepository(directory),
            new WordReviewCursorRepository(directory)
        );
    }

    public static Path directory(String projectId) {
        String safe = projectId.replaceAll("[^A-Za-z0-9._-]", "_");
        return AppDirectories.projectsDir().resolve(safe).resolve("editorial");
    }
}
