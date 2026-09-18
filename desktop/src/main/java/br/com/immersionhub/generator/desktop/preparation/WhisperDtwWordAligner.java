package br.com.immersionhub.generator.desktop.preparation;

import br.com.immersionhub.generator.desktop.infrastructure.BundledTools;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public final class WhisperDtwWordAligner implements WordAligner {
    private static final Pattern EDGE_PUNCTUATION = Pattern.compile("^[^\\p{L}\\p{N}']+|[^\\p{L}\\p{N}']+$");

    private final Supplier<Path> executable;
    private final Path model;
    private final String dtwPreset;
    private final String version;
    private final ObjectMapper mapper = new ObjectMapper();

    public WhisperDtwWordAligner(Path model, String dtwPreset, String version) {
        this(BundledTools::whisper, model, dtwPreset, version);
    }

    WhisperDtwWordAligner(Supplier<Path> executable, Path model, String dtwPreset, String version) {
        this.executable = Objects.requireNonNull(executable);
        this.model = Objects.requireNonNull(model).toAbsolutePath().normalize();
        this.dtwPreset = Objects.requireNonNull(dtwPreset).trim();
        this.version = Objects.requireNonNull(version).trim();
        if (this.dtwPreset.isEmpty() || this.version.isEmpty()) {
            throw new IllegalArgumentException("Configuração de alinhamento incompleta.");
        }
    }

    @Override
    public String version() {
        return version;
    }

    @Override
    public List<TimedText> align(Path technicalAudio, AsrResult transcription, long durationMs) throws Exception {
        if (!Files.isRegularFile(model)) {
            throw new IllegalStateException("Modelo local de alinhamento não encontrado.");
        }

        Path prefix = technicalAudio.resolveSibling("whisper-dtw");
        Path json = Path.of(prefix + ".json");
        Files.deleteIfExists(json);

        Process process = new ProcessBuilder(
            executable.get().toString(),
            "-m", model.toString(),
            "-f", technicalAudio.toString(),
            "-l", "en",
            "-dtw", dtwPreset,
            "-ml", "1",
            "-sow",
            "-ojf",
            "-of", prefix.toString()
        ).redirectErrorStream(true).start();

        String log;
        try (var input = process.getInputStream()) {
            log = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        int exit = process.waitFor();
        if (exit != 0 || !Files.isRegularFile(json)) {
            throw new IOException(log.isBlank() ? "Não foi possível alinhar o material." : log.trim());
        }

        List<TimedText> candidates = parseWordSegments(json, durationMs);
        return preserveTranscript(transcription.text(), candidates);
    }

    List<TimedText> parseWordSegments(Path json, long durationMs) throws Exception {
        JsonNode root = mapper.readTree(json.toFile());
        JsonNode transcription = root.path("transcription");
        if (!transcription.isArray()) throw new IOException("Resultado de alinhamento inválido.");

        List<TimedText> words = new ArrayList<>();
        for (JsonNode segment : transcription) {
            String text = segment.path("text").asText("").trim();
            if (text.isEmpty()) continue;
            long start = WhisperCppAsrEngine.offsetMs(segment, "from");
            long end = WhisperCppAsrEngine.offsetMs(segment, "to");
            if (start < 0 || end <= start || end > durationMs) {
                throw new IOException("Resultado de alinhamento fora da duração.");
            }
            for (String raw : text.split("\\s+")) {
                String word = normalize(raw);
                if (!word.isEmpty()) {
                    words.add(new TimedText(raw.trim(), start, end));
                }
            }
        }
        if (words.isEmpty()) throw new IOException("Alinhamento não produziu palavras.");
        return words;
    }

    List<TimedText> preserveTranscript(String expectedText, List<TimedText> candidates) throws IOException {
        List<String> expected = Arrays.stream(expectedText.trim().split("\\s+"))
            .map(WhisperDtwWordAligner::normalize)
            .filter(word -> !word.isEmpty())
            .toList();
        List<String> actual = candidates.stream()
            .map(TimedText::text)
            .map(WhisperDtwWordAligner::normalize)
            .filter(word -> !word.isEmpty())
            .toList();

        if (!expected.equals(actual)) {
            throw new IOException("O alinhamento não corresponde integralmente à transcrição aceita.");
        }

        List<TimedText> preserved = new ArrayList<>(expected.size());
        String[] originalWords = expectedText.trim().split("\\s+");
        int candidateIndex = 0;
        for (String original : originalWords) {
            if (normalize(original).isEmpty()) continue;
            TimedText timing = candidates.get(candidateIndex++);
            preserved.add(new TimedText(original, timing.startMs(), timing.endMs()));
        }
        return preserved;
    }

    private static String normalize(String value) {
        return EDGE_PUNCTUATION.matcher(value.toLowerCase(Locale.ROOT).trim()).replaceAll("");
    }
}
