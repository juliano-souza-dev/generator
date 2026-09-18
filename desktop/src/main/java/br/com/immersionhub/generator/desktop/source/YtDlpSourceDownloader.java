package br.com.immersionhub.generator.desktop.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public final class YtDlpSourceDownloader implements SourceDownloader {
    private final Path executable;
    private final ObjectMapper mapper = new ObjectMapper();

    public YtDlpSourceDownloader(Path executable) {
        this.executable = executable.toAbsolutePath().normalize();
    }

    @Override
    public SourceDescriptor inspect(String canonicalUrl) throws Exception {
        CommandResult result = run(List.of(
            executable.toString(),
            "--dump-single-json",
            "--skip-download",
            "--no-playlist",
            "--no-warnings",
            canonicalUrl
        ));
        if (result.exitCode() != 0) {
            throw new IOException(productMessage(result.output()));
        }

        JsonNode json = mapper.readTree(result.output());
        String id = json.path("id").asText("");
        String title = json.path("title").asText("Fonte de vídeo");
        double seconds = json.path("duration").asDouble(0);
        long durationMs = Math.round(seconds * 1000.0);
        return new SourceDescriptor(id, title, durationMs);
    }

    @Override
    public Path download(String canonicalUrl, Path targetDirectory) throws Exception {
        Files.createDirectories(targetDirectory);
        String outputTemplate = targetDirectory.resolve("source.%(ext)s").toString();

        CommandResult result = run(List.of(
            executable.toString(),
            "--no-playlist",
            "--no-warnings",
            "--format",
            "best[ext=mp4]/best",
            "--output",
            outputTemplate,
            canonicalUrl
        ));
        if (result.exitCode() != 0) {
            throw new IOException(productMessage(result.output()));
        }

        try (var files = Files.list(targetDirectory)) {
            return files
                .filter(Files::isRegularFile)
                .filter(path -> !path.getFileName().toString().endsWith(".part"))
                .max(Comparator.comparingLong(this::safeSize))
                .orElseThrow(() -> new IOException("A fonte foi processada, mas a mídia local não foi criada."));
        }
    }

    private CommandResult run(List<String> command) throws Exception {
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

    private static String productMessage(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) return "Não foi possível preparar essa fonte.";
        String lastLine = text.lines().reduce((left, right) -> right).orElse(text);
        return lastLine.replaceFirst("^ERROR:\\s*", "").trim();
    }

    private record CommandResult(int exitCode, String output) {}
}
