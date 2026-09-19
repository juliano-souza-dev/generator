package br.com.immersionhub.generator.desktop.translation;

import br.com.immersionhub.generator.desktop.infrastructure.AppDirectories;
import br.com.immersionhub.generator.desktop.preparation.AlignedMaterial;

import java.nio.file.Path;
import java.util.Optional;

public final class TranslationModule {
    private TranslationModule() {}

    public static TranslationService createService(String projectId, String sessionApiKey) {
        Path dir = directory(projectId);
        String key = sessionApiKey == null ? "" : sessionApiKey.trim();
        if (key.isEmpty()) {
            String environment = System.getenv("GROQ_API_KEY");
            key = environment == null ? "" : environment.trim();
        }

        TranslationLogger logger = new TranslationLogger(
            AppDirectories.logsDir().resolve("translation.log")
        );

        GroqClient groq = null;
        if (!key.isEmpty()) {
            String model = System.getenv("IHUB_GROQ_MODEL");
            if (model == null || model.isBlank()) model = GroqConfig.DEFAULT_MODEL;
            groq = new HttpGroqClient(
                new GroqConfig(GroqConfig.DEFAULT_BASE_URI, key, model, 3),
                logger::info
            );
        }

        return new TranslationService(
            groq,
            new TranslationChunker(new TokenEstimator()),
            new GroqTranslationResponseCodec(),
            new ExternalAiPackageService(),
            new FileTranslationMaterialRepository(dir),
            logger,
            dir
        );
    }

    public static ExternalAiPackage prepareExternalPackage(
        String projectId,
        AlignedMaterial aligned
    ) throws Exception {
        TranslationMaterial base = TranslationMaterialFactory.base(aligned);
        return new ExternalAiPackageService().prepare(
            base,
            aligned.preparedMaterial().technicalAudio(),
            directory(projectId)
        );
    }

    public static Optional<TranslationMaterial> loadTranslation(
        String projectId,
        AlignedMaterial aligned
    ) {
        TranslationMaterial base = TranslationMaterialFactory.base(aligned);
        return new FileTranslationMaterialRepository(directory(projectId)).load(base);
    }

    public static Path directory(String projectId) {
        String safe = projectId.replaceAll("[^A-Za-z0-9._-]", "_");
        return AppDirectories.projectsDir().resolve(safe).resolve("translation");
    }

    public static boolean environmentKeyAvailable() {
        String key = System.getenv("GROQ_API_KEY");
        return key != null && !key.isBlank();
    }
}
