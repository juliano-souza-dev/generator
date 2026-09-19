package br.com.immersionhub.generator.desktop.translation;

import br.com.immersionhub.generator.desktop.infrastructure.AppDirectories;
import br.com.immersionhub.generator.desktop.preparation.AlignedMaterial;

import java.nio.file.Path;
import java.util.Optional;

public final class TranslationModule {
    private static final GroqSettingsService SETTINGS = new GroqSettingsService(
        new WindowsDpapiGroqCredentialStore(),
        TranslationModule::validateKeyRemotely
    );

    private TranslationModule() {}

    public static TranslationService createService(String projectId) {
        return createService(projectId, configuredApiKey().orElse(""));
    }

    static TranslationService createService(String projectId, String apiKey) {
        Path dir = directory(projectId);
        String key = apiKey == null ? "" : apiKey.trim();

        TranslationLogger logger = new TranslationLogger(
            AppDirectories.logsDir().resolve("translation.log")
        );

        GroqClient groq = null;
        if (!key.isEmpty()) {
            groq = new HttpGroqClient(
                new GroqConfig(GroqConfig.DEFAULT_BASE_URI, key, configuredModel(), 3),
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

    public static boolean hasApiKey() {
        return configuredApiKey().isPresent();
    }

    public static void validateAndStoreApiKey(String apiKey) throws Exception {
        SETTINGS.validateAndSave(apiKey);
    }

    public static boolean authenticationFailure(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof GroqApiException api
                && (api.statusCode() == 401 || api.statusCode() == 403)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
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

    private static Optional<String> configuredApiKey() {
        Optional<String> stored = SETTINGS.apiKey();
        if (stored.isPresent()) return stored;

        String environment = System.getenv("GROQ_API_KEY");
        if (environment == null || environment.isBlank()) return Optional.empty();
        return Optional.of(environment.trim());
    }

    private static String configuredModel() {
        String model = System.getenv("IHUB_GROQ_MODEL");
        return model == null || model.isBlank() ? GroqConfig.DEFAULT_MODEL : model.trim();
    }

    private static void validateKeyRemotely(String apiKey) throws Exception {
        HttpGroqClient client = new HttpGroqClient(
            new GroqConfig(GroqConfig.DEFAULT_BASE_URI, apiKey, configuredModel(), 0)
        );
        client.modelInfo();
    }
}
