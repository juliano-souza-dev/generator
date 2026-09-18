package br.com.immersionhub.generator.desktop.infrastructure;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class BundledTools {
    private BundledTools() {}

    public static Path ytDlp() {
        String override = System.getProperty("ihub.ytdlp.path", "").trim();
        List<Path> candidates = new ArrayList<>();
        if (!override.isEmpty()) candidates.add(Path.of(override));

        Path javaHome = Path.of(System.getProperty("java.home")).toAbsolutePath().normalize();
        Path appRoot = javaHome.getParent();
        if (appRoot != null) {
            candidates.add(appRoot.resolve("app").resolve("tools").resolve("yt-dlp.exe"));
        }

        candidates.add(Path.of("target", "app", "tools", "yt-dlp.exe").toAbsolutePath());
        candidates.add(Path.of("tools", "yt-dlp.exe").toAbsolutePath());

        return candidates.stream()
            .filter(Files::isRegularFile)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("O componente de preparação da fonte não foi encontrado."));
    }
}
