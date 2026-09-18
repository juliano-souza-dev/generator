package br.com.immersionhub.generator.desktop.timing;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Optional;

public final class TimingDraftRepository {
    private final Path root;
    private final ObjectMapper mapper = new ObjectMapper();

    public TimingDraftRepository(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    public void save(String sourceId, long startMs, long endMs) throws Exception {
        TimingRange range = new TimingRange(startMs, endMs);
        Files.createDirectories(root);

        DraftPayload payload = new DraftPayload(
            sourceId,
            range.startMs(),
            range.endMs(),
            Instant.now().toString()
        );

        Path target = pathFor(sourceId);
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        mapper.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), payload);
        moveAtomicallyWhenPossible(temporary, target);
    }

    public Optional<TimingRange> load(String sourceId, long mediaDurationMs) {
        Path file = pathFor(sourceId);
        if (!Files.isRegularFile(file)) return Optional.empty();

        try {
            DraftPayload payload = mapper.readValue(file.toFile(), DraftPayload.class);
            if (!sourceId.equals(payload.sourceId())) return Optional.empty();
            if (payload.startMs() < 0 || payload.endMs() <= payload.startMs()) return Optional.empty();
            if (payload.endMs() > mediaDurationMs) return Optional.empty();
            return Optional.of(new TimingRange(payload.startMs(), payload.endMs()));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public Path pathFor(String sourceId) {
        String safe = sourceId.replaceAll("[^A-Za-z0-9._-]", "_");
        return root.resolve(safe + ".json").normalize();
    }

    private static void moveAtomicallyWhenPossible(Path source, Path target) throws Exception {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private record DraftPayload(
        String sourceId,
        long startMs,
        long endMs,
        String updatedAt
    ) {}
}
