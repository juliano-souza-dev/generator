package br.com.immersionhub.generator.desktop.source;

import br.com.immersionhub.generator.desktop.model.SourceMedia;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Optional;

public final class FileSourceCacheRepository implements SourceCacheRepository {
    private static final String METADATA_FILE = "source.json";

    private final Path root;
    private final ObjectMapper mapper;

    public FileSourceCacheRepository(Path root) {
        this(root, new ObjectMapper());
    }

    FileSourceCacheRepository(Path root, ObjectMapper mapper) {
        this.root = root.toAbsolutePath().normalize();
        this.mapper = mapper;
    }

    @Override
    public Optional<SourceMedia> find(String sourceId, String canonicalUrl) {
        Path directory = root.resolve(sourceId);
        Path metadataPath = directory.resolve(METADATA_FILE);
        if (!Files.isRegularFile(metadataPath)) return Optional.empty();

        try {
            CacheMetadata metadata = mapper.readValue(metadataPath.toFile(), CacheMetadata.class);
            if (!sourceId.equals(metadata.sourceId()) || !canonicalUrl.equals(metadata.canonicalUrl())) {
                return Optional.empty();
            }
            Path media = directory.resolve(metadata.fileName()).normalize();
            if (!media.startsWith(directory) || !Files.isRegularFile(media) || metadata.durationMs() <= 0) {
                return Optional.empty();
            }
            return Optional.of(new SourceMedia(
                metadata.sourceId(),
                metadata.canonicalUrl(),
                media,
                metadata.title(),
                metadata.durationMs(),
                Instant.parse(metadata.fetchedAt()),
                true
            ));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    @Override
    public SourceMedia store(
        String sourceId,
        String canonicalUrl,
        SourceDescriptor descriptor,
        Path downloadedFile
    ) throws Exception {
        if (!Files.isRegularFile(downloadedFile)) {
            throw new IOException("O download terminou sem produzir uma mídia local válida.");
        }

        Path directory = root.resolve(sourceId);
        Files.createDirectories(directory);

        String fileName = "source" + extensionOf(downloadedFile);
        Path finalMedia = directory.resolve(fileName);
        Files.copy(downloadedFile, finalMedia, StandardCopyOption.REPLACE_EXISTING);

        Instant fetchedAt = Instant.now();
        CacheMetadata metadata = new CacheMetadata(
            sourceId,
            canonicalUrl,
            fileName,
            descriptor.title(),
            descriptor.durationMs(),
            fetchedAt.toString()
        );

        Path temporaryMetadata = directory.resolve(METADATA_FILE + ".tmp");
        mapper.writerWithDefaultPrettyPrinter().writeValue(temporaryMetadata.toFile(), metadata);
        moveAtomicallyWhenPossible(temporaryMetadata, directory.resolve(METADATA_FILE));

        return new SourceMedia(
            sourceId,
            canonicalUrl,
            finalMedia,
            descriptor.title(),
            descriptor.durationMs(),
            fetchedAt,
            false
        );
    }

    private static void moveAtomicallyWhenPossible(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String extensionOf(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot).toLowerCase() : ".mp4";
    }

    private record CacheMetadata(
        String sourceId,
        String canonicalUrl,
        String fileName,
        String title,
        long durationMs,
        String fetchedAt
    ) {}
}
