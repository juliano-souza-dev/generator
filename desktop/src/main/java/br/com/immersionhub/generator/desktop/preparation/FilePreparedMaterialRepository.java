package br.com.immersionhub.generator.desktop.preparation;

import br.com.immersionhub.generator.desktop.model.MediaCut;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

public final class FilePreparedMaterialRepository implements PreparedMaterialRepository {
    private final Path root;
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public FilePreparedMaterialRepository(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public Optional<PreparedMaterial> load(String id) {
        Path json = root.resolve(id).resolve("prepared-material.json");
        if (!Files.isRegularFile(json)) return Optional.empty();
        try {
            Snapshot snapshot = mapper.readValue(json.toFile(), Snapshot.class);
            Path source = Path.of(snapshot.sourcePath);
            Path cut = Path.of(snapshot.cutPath);
            Path audio = Path.of(snapshot.technicalAudio);
            if (!Files.isRegularFile(source) || !Files.isRegularFile(cut) || !Files.isRegularFile(audio)) {
                return Optional.empty();
            }

            MediaCut mediaCut = new MediaCut(
                snapshot.sourceId,
                source,
                cut,
                snapshot.startMs,
                snapshot.endMs,
                snapshot.durationMs,
                Instant.parse(snapshot.cutCreatedAt)
            );
            AsrResult asr = new AsrResult(
                snapshot.language,
                snapshot.text,
                toTimed(snapshot.segments),
                toTimed(snapshot.words)
            );
            PreparedMaterial material = new PreparedMaterial(
                snapshot.id,
                snapshot.pipelineVersion,
                snapshot.asrVersion,
                mediaCut,
                audio,
                asr,
                Instant.parse(snapshot.createdAt)
            );
            return id.equals(material.id()) ? Optional.of(material) : Optional.empty();
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    @Override
    public void save(PreparedMaterial material) throws Exception {
        Path dir = root.resolve(material.id());
        Files.createDirectories(dir);
        Path tmp = dir.resolve("prepared-material.tmp.json");
        Path out = dir.resolve("prepared-material.json");
        mapper.writeValue(tmp.toFile(), Snapshot.from(material));
        try {
            Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static List<TimedText> toTimed(List<Timed> values) {
        return values == null
            ? List.of()
            : values.stream()
                .map(item -> new TimedText(item.text, item.startMs, item.endMs, item.confidence))
                .toList();
    }

    public static final class Timed {
        public String text;
        public long startMs;
        public long endMs;
        public Double confidence;

        public Timed() {}

        Timed(TimedText value) {
            text = value.text();
            startMs = value.startMs();
            endMs = value.endMs();
            confidence = value.confidence();
        }
    }

    public static final class Snapshot {
        public String id;
        public String pipelineVersion;
        public String asrVersion;
        public String sourceId;
        public String sourcePath;
        public String cutPath;
        public String technicalAudio;
        public String cutCreatedAt;
        public String createdAt;
        public String language;
        public String text;
        public long startMs;
        public long endMs;
        public long durationMs;
        public List<Timed> segments;
        public List<Timed> words;

        public Snapshot() {}

        static Snapshot from(PreparedMaterial material) {
            Snapshot snapshot = new Snapshot();
            var cut = material.mediaCut();
            snapshot.id = material.id();
            snapshot.pipelineVersion = material.pipelineVersion();
            snapshot.asrVersion = material.asrVersion();
            snapshot.sourceId = cut.sourceId();
            snapshot.sourcePath = cut.sourcePath().toString();
            snapshot.cutPath = cut.outputPath().toString();
            snapshot.startMs = cut.startMs();
            snapshot.endMs = cut.endMs();
            snapshot.durationMs = cut.durationMs();
            snapshot.cutCreatedAt = cut.createdAt().toString();
            snapshot.technicalAudio = material.technicalAudio().toString();
            snapshot.createdAt = material.createdAt().toString();
            snapshot.language = material.transcription().language();
            snapshot.text = material.transcription().text();
            snapshot.segments = material.transcription().segments().stream().map(Timed::new).toList();
            snapshot.words = material.transcription().words().stream().map(Timed::new).toList();
            return snapshot;
        }
    }
}
