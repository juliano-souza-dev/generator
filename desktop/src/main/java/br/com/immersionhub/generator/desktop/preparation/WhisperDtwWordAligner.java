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
    private static final Pattern EDGE_PUNCTUATION =
        Pattern.compile("^[^\\p{L}\\p{N}']+|[^\\p{L}\\p{N}']+$");

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

        // VAD não é habilitado nesta passagem: o alinhamento deve permanecer na timeline
        // original do MediaCut. O DTW fornece âncoras acústicas por token.
        Process process = new ProcessBuilder(
            executable.get().toString(),
            "-m", model.toString(),
            "-f", technicalAudio.toString(),
            "-l", "en",
            "-dtw", dtwPreset,
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

        List<TimedText> candidates = parseDtwWords(json, durationMs);
        return preserveTranscript(transcription.text(), candidates);
    }

    List<TimedText> parseDtwWords(Path json, long durationMs) throws Exception {
        JsonNode root = mapper.readTree(json.toFile());
        JsonNode transcription = root.path("transcription");
        if (!transcription.isArray()) {
            throw new IOException("Resultado de alinhamento inválido.");
        }

        List<TimedText> words = new ArrayList<>();
        for (JsonNode segment : transcription) {
            long segmentStart = WhisperCppAsrEngine.offsetMs(segment, "from");
            long segmentEnd = WhisperCppAsrEngine.offsetMs(segment, "to");
            if (segmentStart < 0 || segmentEnd <= segmentStart || segmentEnd > durationMs) {
                throw new IOException("Resultado de alinhamento fora da duração.");
            }

            List<AnchoredWord> anchored = groupWordAnchors(segment.path("tokens"));
            if (anchored.isEmpty()) continue;

            for (int index = 0; index < anchored.size(); index++) {
                AnchoredWord current = anchored.get(index);
                long start = index == 0
                    ? segmentStart
                    : midpoint(anchored.get(index - 1).anchorMs(), current.anchorMs());
                long end = index == anchored.size() - 1
                    ? segmentEnd
                    : midpoint(current.anchorMs(), anchored.get(index + 1).anchorMs());

                start = Math.max(segmentStart, start);
                end = Math.min(segmentEnd, end);
                if (end <= start) {
                    throw new IOException("Âncoras DTW não formam uma sequência temporal válida.");
                }
                words.add(new TimedText(current.text(), start, end, current.confidence()));
            }
        }

        if (words.isEmpty()) {
            throw new IOException("Alinhamento não produziu palavras.");
        }
        AlignedMaterial.validate(words, durationMs);
        return words;
    }

    private static List<AnchoredWord> groupWordAnchors(JsonNode tokens) throws IOException {
        if (!tokens.isArray()) {
            throw new IOException("Resultado DTW sem tokens.");
        }

        List<AnchoredWord> words = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        long anchorMs = -1;
        double confidenceSum = 0.0;
        int confidenceCount = 0;

        for (JsonNode token : tokens) {
            String raw = token.path("text").asText("");
            if (raw.isBlank() || raw.startsWith("[_")) continue;

            boolean startsWord = Character.isWhitespace(raw.charAt(0));
            String piece = raw.trim();
            if (piece.isEmpty()) continue;

            String lexical = normalize(piece);
            boolean punctuationOnly = lexical.isEmpty();

            if (startsWord && !current.isEmpty()) {
                if (anchorMs < 0) throw new IOException("Palavra sem âncora DTW.");
                words.add(new AnchoredWord(
                    current.toString(),
                    anchorMs,
                    confidenceCount == 0 ? null : confidenceSum / confidenceCount
                ));
                current.setLength(0);
                anchorMs = -1;
                confidenceSum = 0.0;
                confidenceCount = 0;
            }

            if (!current.isEmpty() && punctuationOnly) {
                current.append(piece);
                continue;
            }

            if (current.isEmpty() && punctuationOnly) {
                continue;
            }

            current.append(piece);
            if (token.path("p").isNumber()) {
                confidenceSum += token.path("p").asDouble();
                confidenceCount++;
            }
            long tokenDtw = token.path("t_dtw").asLong(-1);
            if (anchorMs < 0 && tokenDtw >= 0) {
                anchorMs = tokenDtw * 10L;
            }
        }

        if (!current.isEmpty()) {
            if (anchorMs < 0) throw new IOException("Palavra sem âncora DTW.");
            words.add(new AnchoredWord(
                current.toString(),
                anchorMs,
                confidenceCount == 0 ? null : confidenceSum / confidenceCount
            ));
        }
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
            preserved.add(new TimedText(original, timing.startMs(), timing.endMs(), timing.confidence()));
        }
        return preserved;
    }

    private static long midpoint(long left, long right) {
        return left + ((right - left) / 2L);
    }

    private static String normalize(String value) {
        return EDGE_PUNCTUATION.matcher(value.toLowerCase(Locale.ROOT).trim()).replaceAll("");
    }

    private record AnchoredWord(String text, long anchorMs, Double confidence) {}
}
