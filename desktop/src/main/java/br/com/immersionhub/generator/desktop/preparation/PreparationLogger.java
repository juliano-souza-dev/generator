package br.com.immersionhub.generator.desktop.preparation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;

public final class PreparationLogger {
    private final Path logFile;

    public PreparationLogger(Path logFile) {
        this.logFile = logFile.toAbsolutePath().normalize();
    }

    public synchronized void info(String message) {
        append("INFO", message, null);
    }

    public synchronized void warn(String message, Throwable throwable) {
        append("WARN", message, throwable);
    }

    public synchronized void error(String message, Throwable throwable) {
        append("ERROR", message, throwable);
    }

    private void append(String level, String message, Throwable throwable) {
        try {
            Files.createDirectories(logFile.getParent());
            String detail = throwable == null
                ? ""
                : " | " + throwable.getClass().getSimpleName() + ": " + safe(throwable.getMessage());
            String line = Instant.now() + " [" + level + "] " + safe(message) + detail + System.lineSeparator();
            Files.writeString(
                logFile,
                line,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );
        } catch (IOException ignored) {
            // Logging cannot turn a recoverable product failure into another product failure.
        }
    }

    private static String safe(String value) {
        if (value == null) return "";
        return value.replace("\r", " ").replace("\n", " ");
    }
}
