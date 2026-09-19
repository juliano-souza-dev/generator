package br.com.immersionhub.generator.desktop.translation;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

public final class TranslationLogger {
    private final Path path;

    public TranslationLogger(Path path) {
        this.path = path.toAbsolutePath().normalize();
    }

    public synchronized void info(String message) {
        append("INFO", message, null);
    }

    public synchronized void warn(String message, Throwable failure) {
        append("WARN", message, failure);
    }

    public synchronized void error(String message, Throwable failure) {
        append("ERROR", message, failure);
    }

    private void append(String level, String message, Throwable failure) {
        try {
            Files.createDirectories(path.getParent());
            StringBuilder line = new StringBuilder()
                .append(Instant.now()).append(" [").append(level).append("] ")
                .append(message == null ? "" : message);
            if (failure != null) {
                line.append(" | ").append(failure.getClass().getSimpleName())
                    .append(": ").append(safe(failure.getMessage()));
            }
            line.append(System.lineSeparator());
            Files.writeString(
                path,
                line.toString(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );
        } catch (Exception ignored) {
        }
    }

    private static String safe(String value) {
        if (value == null) return "";
        return value.replaceAll("(?i)bearer\\s+[A-Za-z0-9._-]+", "Bearer [REDACTED]");
    }
}
