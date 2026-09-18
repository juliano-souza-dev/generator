package br.com.immersionhub.generator.desktop.source;

import br.com.immersionhub.generator.desktop.model.SourceMedia;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;

public final class SourceAcquisitionService {
    private final SourceUrlCanonicalizer canonicalizer;
    private final SourceCacheRepository cache;
    private final SourceDownloader downloader;
    private final Path incomingRoot;

    public SourceAcquisitionService(
        SourceUrlCanonicalizer canonicalizer,
        SourceCacheRepository cache,
        SourceDownloader downloader,
        Path incomingRoot
    ) {
        this.canonicalizer = canonicalizer;
        this.cache = cache;
        this.downloader = downloader;
        this.incomingRoot = incomingRoot.toAbsolutePath().normalize();
    }

    public SourceMedia acquire(String rawUrl) throws Exception {
        String canonicalUrl = canonicalizer.canonicalize(rawUrl);
        String sourceId = SourceIds.fromCanonicalUrl(canonicalUrl);

        var cached = cache.find(sourceId, canonicalUrl);
        if (cached.isPresent()) return cached.get().asCacheHit();

        SourceDescriptor descriptor = downloader.inspect(canonicalUrl);
        Path incoming = incomingRoot.resolve(UUID.randomUUID().toString());
        Files.createDirectories(incoming);

        try {
            Path downloaded = downloader.download(canonicalUrl, incoming);
            return cache.store(sourceId, canonicalUrl, descriptor, downloaded);
        } finally {
            deleteTree(incoming);
        }
    }

    private static void deleteTree(Path root) {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }
}
