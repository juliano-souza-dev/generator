package br.com.immersionhub.generator.desktop.source;

import br.com.immersionhub.generator.desktop.infrastructure.AppDirectories;
import br.com.immersionhub.generator.desktop.infrastructure.BundledTools;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

public final class YtDlpSourceDownloader implements SourceDownloader {
    private static final int MAX_TRANSIENT_ATTEMPTS = 3;
    private static final List<FormatStrategy> FORMAT_STRATEGIES = List.of(
        new FormatStrategy("mp4-first", "bv*[ext=mp4]+ba[ext=m4a]/b[ext=mp4]/best[ext=mp4]"),
        new FormatStrategy("best-merge", "bv*+ba/b"),
        new FormatStrategy("single-file", "best[ext=mp4]/best")
    );

    private final Supplier<Path> executableSupplier;
    private final Supplier<Path> ffmpegSupplier;
    private final CommandRunner commandRunner;
    private final Sleeper sleeper;
    private final SourceDownloadDiagnostics diagnostics;
    private final ObjectMapper mapper = new ObjectMapper();

    public YtDlpSourceDownloader() {
        this(
            BundledTools::ytDlp,
            BundledTools::ffmpeg,
            YtDlpSourceDownloader::runProcess,
            Thread::sleep,
            new SourceDownloadDiagnostics(AppDirectories.logsDir().resolve("source-download.log"))
        );
    }

    YtDlpSourceDownloader(
        Supplier<Path> executableSupplier,
        Supplier<Path> ffmpegSupplier,
        CommandRunner commandRunner,
        Sleeper sleeper,
        SourceDownloadDiagnostics diagnostics
    ) {
        this.executableSupplier = executableSupplier;
        this.ffmpegSupplier = ffmpegSupplier;
        this.commandRunner = commandRunner;
        this.sleeper = sleeper;
        this.diagnostics = diagnostics;
    }

    @Override
    public SourceDescriptor inspect(String canonicalUrl) throws Exception {
        Path executable = executableSupplier.get();
        CommandResult result = runWithRetry(
            "inspect",
            "metadata",
            List.of(
                executable.toString(),
                "--dump-single-json",
                "--skip-download",
                "--no-playlist",
                "--no-warnings",
                "--socket-timeout",
                "20",
                canonicalUrl
            )
        );

        if (result.exitCode() != 0) {
            throw new IOException(productMessage(result.output()));
        }

        JsonNode json = mapper.readTree(result.output());
        String id = json.path("id").asText("");
        String title = json.path("title").asText("Fonte de vídeo");
        long durationMs = Math.round(json.path("duration").asDouble(0) * 1000.0);
        return new SourceDescriptor(id, title, durationMs);
    }

    @Override
    public Path download(String canonicalUrl, Path targetDirectory) throws Exception {
        Files.createDirectories(targetDirectory);
        Path executable = executableSupplier.get();
        Path ffmpeg = ffmpegSupplier.get();
        String outputTemplate = targetDirectory.resolve("source.%(ext)s").toString();
        CommandResult lastFailure = new CommandResult(1, "Não foi possível preparar essa fonte.");

        for (FormatStrategy strategy : FORMAT_STRATEGIES) {
            List<String> command = List.of(
                executable.toString(),
                "--no-playlist",
                "--no-warnings",
                "--socket-timeout",
                "20",
                "--ffmpeg-location",
                ffmpeg.getParent().toString(),
                "--merge-output-format",
                "mp4",
                "--format",
                strategy.selector(),
                "--output",
                outputTemplate,
                canonicalUrl
            );

            CommandResult result = runWithRetry("download", strategy.name(), command);
            if (result.exitCode() == 0) {
                Optional<Path> media = findDownloadedMedia(targetDirectory);
                if (media.isPresent()) return media.get();

                diagnostics.result(
                    "download",
                    1,
                    strategy.name() + "-missing-output",
                    1,
                    "yt-dlp terminou sem produzir mídia local válida."
                );
                lastFailure = new CommandResult(1, "A fonte foi processada, mas a mídia local não foi criada.");
                cleanupDownloadArtifacts(targetDirectory);
                continue;
            }

            lastFailure = result;
            if (isDefinitiveFailure(result.output())) {
                throw new IOException(productMessage(result.output()));
            }

            if (isFormatUnavailable(result.output())) {
                cleanupDownloadArtifacts(targetDirectory);
                continue;
            }

            // Transient retries are already exhausted by runWithRetry().
            throw new IOException(productMessage(result.output()));
        }

        throw new IOException(productMessage(lastFailure.output()));
    }

    private CommandResult runWithRetry(String stage, String strategy, List<String> command) throws Exception {
        CommandResult last = new CommandResult(1, "");

        for (int attempt = 1; attempt <= MAX_TRANSIENT_ATTEMPTS; attempt++) {
            try {
                CommandResult result = commandRunner.run(command);
                diagnostics.result(stage, attempt, strategy, result.exitCode(), result.output());
                last = result;

                if (result.exitCode() == 0) return result;
                if (isDefinitiveFailure(result.output()) || isFormatUnavailable(result.output())) return result;
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                diagnostics.failure(stage, attempt, strategy, interrupted);
                throw interrupted;
            } catch (Exception failure) {
                diagnostics.failure(stage, attempt, strategy, failure);
                if (attempt == MAX_TRANSIENT_ATTEMPTS) throw failure;
            }

            if (attempt < MAX_TRANSIENT_ATTEMPTS) {
                sleeper.sleep(backoffMillis(attempt));
            }
        }

        return last;
    }

    private Optional<Path> findDownloadedMedia(Path targetDirectory) throws IOException {
        try (var files = Files.list(targetDirectory)) {
            return files
                .filter(Files::isRegularFile)
                .filter(path -> !path.getFileName().toString().endsWith(".part"))
                .filter(path -> !path.getFileName().toString().endsWith(".ytdl"))
                .filter(path -> safeSize(path) > 0)
                .max(Comparator.comparingLong(this::safeSize));
        }
    }

    private void cleanupDownloadArtifacts(Path targetDirectory) {
        if (!Files.isDirectory(targetDirectory)) return;
        try (var files = Files.list(targetDirectory)) {
            files.filter(Files::isRegularFile).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    private static CommandResult runProcess(List<String> command) throws Exception {
        Process process = new ProcessBuilder(command)
            .redirectErrorStream(true)
            .start();

        String output;
        try (var input = process.getInputStream()) {
            output = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        int exitCode = process.waitFor();
        return new CommandResult(exitCode, output);
    }

    private long safeSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException exception) {
            return 0;
        }
    }

    private static long backoffMillis(int completedAttempt) {
        return completedAttempt * 350L;
    }

    private static boolean isFormatUnavailable(String raw) {
        String text = normalize(raw);
        return text.contains("requested format is not available")
            || text.contains("format is not available");
    }

    private static boolean isDefinitiveFailure(String raw) {
        String text = normalize(raw);
        return text.contains("video unavailable")
            || text.contains("private video")
            || text.contains("this video is private")
            || text.contains("has been removed")
            || text.contains("members-only")
            || text.contains("not available in your country")
            || text.contains("unsupported url")
            || text.contains("invalid url")
            || text.contains("not a valid url")
            || text.contains("sign in to confirm your age")
            || text.contains("login required")
            || text.contains("copyright");
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.toLowerCase(Locale.ROOT);
    }

    private static String productMessage(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) return "Não foi possível preparar essa fonte.";
        String lastLine = text.lines().reduce((left, right) -> right).orElse(text);
        return lastLine.replaceFirst("^ERROR:\\s*", "").trim();
    }

    @FunctionalInterface
    interface CommandRunner {
        CommandResult run(List<String> command) throws Exception;
    }

    @FunctionalInterface
    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    record CommandResult(int exitCode, String output) {}

    private record FormatStrategy(String name, String selector) {}
}
