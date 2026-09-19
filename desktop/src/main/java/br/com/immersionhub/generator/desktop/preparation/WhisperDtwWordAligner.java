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
            command(executable.get(), model, technicalAudio, prefix, dtwPreset)
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

    static List<String> command(
        Path executable,
        Path model,
        Path technicalAudio,
        Path prefix,
        String dtwPreset
    ) {
        return List.of(
            executable.toString(),
            "-m", model.toString(),
            "-f", technicalAudio.toString(),
            "-l", "en",
            "-dtw", dtwPreset,
            "-nfa",
            "-ojf",
            "-of", prefix.toString()
        );
    }

    List<TimedText> parseDtwWords(Path json, long durationMs) throws Exception {
        JsonNode root = mapper.readTree(json.toFile());
        JsonNode transcription = root.path("transcription");
        if (!transcription.isArray()) {
            throw new IOException("Resultado de alinhamento inválido.");
        }

        List<TimedText> words = new ArrayList<>();
        for (int segmentIndex = 0; segmentIndex < transcription.size(); segmentIndex++) {
            JsonNode segment = transcription.get(segmentIndex);
            long segmentStart = WhisperCppAsrEngine.offsetMs(segment, "from");
            long rawSegmentEnd = WhisperCppAsrEngine.offsetMs(segment, "to");
            long segmentEnd = normalizeSegmentEnd(segmentIndex, segmentStart, rawSegmentEnd, durationMs);

            List<AnchoredWord> anchored = groupWordAnchors(segment.path("tokens"));
            if (anchored.isEmpty()) continue;

            words.addAll(buildTimedWords(segmentIndex, anchored, segmentStart, segmentEnd));
        }

        if (words.isEmpty()) {
            throw new IOException("Alinhamento não produziu palavras.");
        }
        AlignedMaterial.validate(words, durationMs);
        return words;
    }

    private static List<TimedText> buildTimedWords(
        int segmentIndex,
        List<AnchoredWord> anchored,
        long segmentStart,
        long segmentEnd
    ) throws IOException {
        List<TimedText> words = new ArrayList<>(anchored.size());

        long previousAnchor = Long.MIN_VALUE;
        for (int index = 0; index < anchored.size(); index++) {
            long anchor = anchored.get(index).anchorMs();
            if (anchor < previousAnchor) {
                throw new IOException(
                    "Âncoras DTW regressivas: segment[" + segmentIndex + "] word[" + index
                        + "] anchorMs=" + anchor + ", previousAnchorMs=" + previousAnchor + "."
                );
            }
            previousAnchor = anchor;
        }

        int runStart = 0;
        while (runStart < anchored.size()) {
            long anchor = anchored.get(runStart).anchorMs();
            int runEnd = runStart + 1;
            while (runEnd < anchored.size() && anchored.get(runEnd).anchorMs() == anchor) {
                runEnd++;
            }

            long leftBound = runStart == 0
                ? segmentStart
                : midpoint(anchored.get(runStart - 1).anchorMs(), anchor);
            long rightBound = runEnd == anchored.size()
                ? segmentEnd
                : midpoint(anchor, anchored.get(runEnd).anchorMs());

            leftBound = Math.max(segmentStart, leftBound);
            rightBound = Math.min(segmentEnd, rightBound);

            int runSize = runEnd - runStart;
            long available = rightBound - leftBound;
            if (available < runSize) {
                throw new IOException(
                    "Âncoras DTW sem espaço temporal suficiente: segment[" + segmentIndex
                        + "] runStartWord[" + runStart + "] runSize=" + runSize
                        + ", anchorMs=" + anchor + ", leftMs=" + leftBound
                        + ", rightMs=" + rightBound + "."
                );
            }

            for (int offset = 0; offset < runSize; offset++) {
                AnchoredWord current = anchored.get(runStart + offset);
                long start = leftBound + ((available * offset) / runSize);
                long end = leftBound + ((available * (offset + 1L)) / runSize);
                if (end <= start) {
                    throw new IOException(
                        "Âncoras DTW não formam uma sequência temporal válida: segment[" + segmentIndex
                            + "] word[" + (runStart + offset) + "] startMs=" + start
                            + ", endMs=" + end + ", anchorMs=" + anchor + "."
                    );
                }
                words.add(new TimedText(current.text(), start, end, current.confidence()));
            }

            runStart = runEnd;
        }

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

    private static long normalizeSegmentEnd(
        int segmentIndex,
        long segmentStart,
        long segmentEnd,
        long durationMs
    ) throws IOException {
        if (segmentStart < 0 || segmentEnd <= segmentStart || segmentStart >= durationMs) {
            throw invalidSegment(segmentIndex, segmentStart, segmentEnd, durationMs);
        }
        if (segmentEnd <= durationMs) {
            return segmentEnd;
        }

        long overshoot = segmentEnd - durationMs;
        if (overshoot <= AsrTimelineNormalizer.END_BOUNDARY_TOLERANCE_MS) {
            return durationMs;
        }
        throw invalidSegment(segmentIndex, segmentStart, segmentEnd, durationMs);
    }

    private static IOException invalidSegment(
        int segmentIndex,
        long segmentStart,
        long segmentEnd,
        long durationMs
    ) {
        return new IOException(
            "Resultado de alinhamento fora da duração: segment[" + segmentIndex
                + "] startMs=" + segmentStart
                + ", endMs=" + segmentEnd
                + ", durationMs=" + durationMs + "."
        );
    }

    private static long midpoint(long left, long right) {
        return left + ((right - left) / 2L);
    }

    private static String normalize(String value) {
        return EDGE_PUNCTUATION.matcher(value.toLowerCase(Locale.ROOT).trim()).replaceAll("");
    }

    private record AnchoredWord(String text, long anchorMs, Double confidence) {}
}
