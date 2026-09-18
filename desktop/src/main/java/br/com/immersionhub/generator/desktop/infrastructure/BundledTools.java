package br.com.immersionhub.generator.desktop.infrastructure;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class BundledTools {
    private BundledTools() {}

    public static Path ytDlp() {
        return resolve(
            "ihub.ytdlp.path",
            "yt-dlp.exe",
            List.of(
                Path.of("tools", "yt-dlp.exe"),
                Path.of("target", "app", "tools", "yt-dlp.exe")
            ),
            "O componente de preparação da fonte não foi encontrado."
        );
    }

    public static Path whisper() {
        return resolve(
            "ihub.whisper.path",
            Path.of("whisper", "whisper-cli.exe").toString(),
            List.of(
                Path.of("tools", "whisper", "whisper-cli.exe"),
                Path.of("target", "app", "tools", "whisper", "whisper-cli.exe")
            ),
            "O componente de transcrição não foi encontrado."
        );
    }

    public static Path whisperModel() {
        return resolve(
            "ihub.whisper.model.path",
            Path.of("whisper", "models", "ggml-base.en.bin").toString(),
            List.of(
                Path.of("tools", "whisper", "models", "ggml-base.en.bin"),
                Path.of("target", "app", "tools", "whisper", "models", "ggml-base.en.bin")
            ),
            "O modelo local de transcrição não foi encontrado."
        );
    }

    public static Path ffmpeg() {
        return resolve(
            "ihub.ffmpeg.path",
            Path.of("ffmpeg", "ffmpeg.exe").toString(),
            List.of(
                Path.of("tools", "ffmpeg", "ffmpeg.exe"),
                Path.of("target", "app", "tools", "ffmpeg", "ffmpeg.exe")
            ),
            "O componente de processamento de mídia não foi encontrado."
        );
    }

    private static Path resolve(
        String property,
        String packagedRelativePath,
        List<Path> developmentCandidates,
        String errorMessage
    ) {
        List<Path> candidates = new ArrayList<>();
        String override = System.getProperty(property, "").trim();
        if (!override.isEmpty()) candidates.add(Path.of(override));

        Path javaHome = Path.of(System.getProperty("java.home")).toAbsolutePath().normalize();
        Path appRoot = javaHome.getParent();
        if (appRoot != null) {
            candidates.add(appRoot.resolve("app").resolve("tools").resolve(packagedRelativePath));
        }

        for (Path candidate : developmentCandidates) {
            candidates.add(candidate.toAbsolutePath().normalize());
        }

        return candidates.stream()
            .map(path -> path.toAbsolutePath().normalize())
            .filter(Files::isRegularFile)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(errorMessage));
    }
}
