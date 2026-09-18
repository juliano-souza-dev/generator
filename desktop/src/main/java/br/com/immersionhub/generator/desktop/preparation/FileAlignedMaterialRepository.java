package br.com.immersionhub.generator.desktop.preparation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

public final class FileAlignedMaterialRepository implements AlignedMaterialRepository {
    private final Path root;
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public FileAlignedMaterialRepository(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public Optional<AlignedMaterial> load(String id, PreparedMaterial preparedMaterial) {
        Path json = root.resolve(id).resolve("aligned-material.json");
        if (!Files.isRegularFile(json)) return Optional.empty();
        try {
            Snapshot snapshot = mapper.readValue(json.toFile(), Snapshot.class);
            if (!id.equals(snapshot.id) || !preparedMaterial.id().equals(snapshot.preparedMaterialId)) {
                return Optional.empty();
            }
            List<TimedText> words = snapshot.words == null
                ? List.of()
                : snapshot.words.stream().map(item -> new TimedText(item.text, item.startMs, item.endMs)).toList();
            return Optional.of(new AlignedMaterial(
                snapshot.id,
                preparedMaterial,
                snapshot.alignerVersion,
                words,
                Instant.parse(snapshot.createdAt)
            ));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    @Override
    public void save(AlignedMaterial material) throws Exception {
        Path dir = root.resolve(material.id());
        Files.createDirectories(dir);
        Path tmp = dir.resolve("aligned-material.tmp.json");
        Path out = dir.resolve("aligned-material.json");
        mapper.writeValue(tmp.toFile(), Snapshot.from(material));
        try {
            Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static final class Timed {
        public String text;
        public long startMs;
        public long endMs;
        public Timed() {}
        Timed(TimedText word) {
            text = word.text();
            startMs = word.startMs();
            endMs = word.endMs();
        }
    }

    public static final class Snapshot {
        public String id;
        public String preparedMaterialId;
        public String alignerVersion;
        public String createdAt;
        public List<Timed> words;
        public Snapshot() {}

        static Snapshot from(AlignedMaterial material) {
            Snapshot snapshot = new Snapshot();
            snapshot.id = material.id();
            snapshot.preparedMaterialId = material.preparedMaterial().id();
            snapshot.alignerVersion = material.alignerVersion();
            snapshot.createdAt = material.createdAt().toString();
            snapshot.words = material.words().stream().map(Timed::new).toList();
            return snapshot;
        }
    }
}
