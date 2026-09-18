package br.com.immersionhub.generator.desktop.preparation;

import br.com.immersionhub.generator.desktop.infrastructure.BundledTools;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Supplier;

public final class WhisperCppAsrEngine implements AsrEngine {
    private final Supplier<Path> executable;
    private final Path model;
    private final String version;
    private final ObjectMapper mapper = new ObjectMapper();

    public WhisperCppAsrEngine(Path model, String version) {
        this(BundledTools::whisper, model, version);
    }

    WhisperCppAsrEngine(Supplier<Path> executable, Path model, String version) {
        this.executable = executable;
        this.model = model.toAbsolutePath().normalize();
        this.version = version;
    }

    @Override public String version() { return version; }

    @Override public AsrResult transcribeEnglish(Path audio) throws Exception {
        if (!Files.isRegularFile(model)) {
            throw new IllegalStateException("Modelo local de transcrição não encontrado.");
        }

        Path prefix = audio.resolveSibling("whisper-result");
        Path json = Path.of(prefix + ".json");
        Files.deleteIfExists(json);

        Process process = new ProcessBuilder(
            executable.get().toString(),
            "-m", model.toString(),
            "-f", audio.toString(),
            "-l", "en",
            "-ojf",
            "-of", prefix.toString()
        ).redirectErrorStream(true).start();

        String log;
        try (var input = process.getInputStream()) {
            log = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        int exit = process.waitFor();
        if (exit != 0 || !Files.isRegularFile(json)) {
            throw new IOException(log.isBlank() ? "Não foi possível transcrever o material." : log.trim());
        }
        return parse(json);
    }

    AsrResult parse(Path json) throws Exception {
        JsonNode root = mapper.readTree(json.toFile());
        List<TimedText> segments = new ArrayList<>();
        List<TimedText> words = new ArrayList<>();
        StringBuilder full = new StringBuilder();

        JsonNode transcription = root.path("transcription");
        if (!transcription.isArray()) {
            throw new IOException("Resultado de transcrição inválido.");
        }

        for (JsonNode segment : transcription) {
            String text = segment.path("text").asText("").trim();
            if (text.isEmpty()) continue;

            long start = offsetMs(segment, "from");
            long end = offsetMs(segment, "to");
            if (end > start) segments.add(new TimedText(text, start, end));

            if (!full.isEmpty()) full.append(' ');
            full.append(text);

            JsonNode tokens = segment.path("tokens");
            if (tokens.isArray()) {
                for (JsonNode token : tokens) {
                    String tokenText = token.path("text").asText("").trim();
                    long wordStart = offsetMs(token, "from");
                    long wordEnd = offsetMs(token, "to");
                    if (!tokenText.isEmpty() && wordEnd > wordStart) {
                        Double confidence = token.path("p").isNumber() ? token.path("p").asDouble() : null;
                        words.add(new TimedText(tokenText, wordStart, wordEnd, confidence));
                    }
                }
            }
        }
        return new AsrResult("en", full.toString(), segments, words);
    }

    static long offsetMs(JsonNode node, String side) {
        return node.path("offsets").path(side).asLong(-1);
    }
}
