package br.com.immersionhub.generator.desktop.infrastructure;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AppDirectories {
    private static final String APP_FOLDER = "ImmersionHub Generator";

    private AppDirectories() {}

    public static Path baseDir() {
        String override = System.getProperty("ihub.data.dir", "").trim();
        if (!override.isEmpty()) {
            return Path.of(override).toAbsolutePath().normalize();
        }

        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            return Path.of(localAppData, APP_FOLDER).toAbsolutePath().normalize();
        }

        return Path.of(System.getProperty("user.home"), ".immersionhub-generator")
            .toAbsolutePath().normalize();
    }

    public static Path sourceCacheDir() { return baseDir().resolve("source-cache"); }
    public static Path projectsDir() { return baseDir().resolve("projects"); }
    public static Path logsDir() { return baseDir().resolve("logs"); }
    public static Path settingsDir() { return baseDir().resolve("settings"); }

    public static void prepare() {
        try {
            Files.createDirectories(sourceCacheDir());
            Files.createDirectories(projectsDir());
            Files.createDirectories(logsDir());
            Files.createDirectories(settingsDir());
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível preparar os diretórios locais do Generator.", exception);
        }
    }
}
