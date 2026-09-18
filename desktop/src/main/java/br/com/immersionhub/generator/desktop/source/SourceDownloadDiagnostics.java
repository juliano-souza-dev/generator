package br.com.immersionhub.generator.desktop.source;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

final class SourceDownloadDiagnostics {
    private static final int MAX_OUTPUT_CHARS = 4_000;
    private final Path logFile;

    SourceDownloadDiagnostics(Path logFile) {
        this.logFile = logFile.toAbsolutePath().normalize();
    }

    synchronized void result(String stage, int attempt, String strategy, int exitCode, String output) {
        append(
            "stage=" + stage +
            " attempt=" + attempt +
            " strategy=" + strategy +
            " exit=" + exitCode +
            " output=" + compact(output)
        );
    }

    synchronized void failure(String stage, int attempt, String strategy, Throwable failure) {
        String message = failure == null ? "" : failure.getClass().getSimpleName() + ": " + failure.getMessage();
        append(
            "stage=" + stage +
            " attempt=" + attempt +
            " strategy=" + strategy +
            " exception=" + compact(message)
        );
    }

    private void append(String message) {
        try {
            Files.createDirectories(logFile.getParent());
            Files.writeString(
                logFile,
                Instant.now() + " " + message + System.lineSeparator(),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );
        } catch (Exception ignored) {
            // Diagnostics must never break Source acquisition.
        }
    }

    private static String compact(String text) {
        if (text == null || text.isBlank()) return "<empty>";
        String compacted = text.replace('\r', ' ').replace('\n', ' ').replaceAll("\\s+", " ").trim();
        if (compacted.length() <= MAX_OUTPUT_CHARS) return compacted;
        return compacted.substring(0, MAX_OUTPUT_CHARS) + "…";
    }
}
