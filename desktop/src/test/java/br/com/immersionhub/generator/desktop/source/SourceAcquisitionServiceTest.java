package br.com.immersionhub.generator.desktop.source;

import br.com.immersionhub.generator.desktop.model.SourceMedia;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceAcquisitionServiceTest {
    @Test
    void downloadsOnceThenReusesCache(@TempDir Path tempDir) throws Exception {
        AtomicInteger downloads = new AtomicInteger();
        SourceDownloader downloader = new SourceDownloader() {
            @Override
            public SourceDescriptor inspect(String canonicalUrl) {
                return new SourceDescriptor("YuUeNsZgmyc", "Cena de teste", 30_000);
            }

            @Override
            public Path download(String canonicalUrl, Path targetDirectory) throws Exception {
                downloads.incrementAndGet();
                Path file = targetDirectory.resolve("source.mp4");
                Files.writeString(file, "fake-media");
                return file;
            }
        };

        Path cacheDir = tempDir.resolve("cache");
        SourceAcquisitionService service = new SourceAcquisitionService(
            new SourceUrlCanonicalizer(),
            new FileSourceCacheRepository(cacheDir),
            downloader,
            cacheDir.resolve(".incoming")
        );

        SourceMedia first = service.acquire("https://youtu.be/YuUeNsZgmyc?t=2");
        SourceMedia second = service.acquire("https://www.youtube.com/watch?v=YuUeNsZgmyc");

        assertFalse(first.cacheHit());
        assertTrue(second.cacheHit());
        assertEquals(1, downloads.get());
        assertEquals(first.localPath(), second.localPath());
        assertTrue(Files.isRegularFile(second.localPath()));
    }

    @Test
    void missingCachedMediaForcesFreshDownload(@TempDir Path tempDir) throws Exception {
        AtomicInteger downloads = new AtomicInteger();
        SourceDownloader downloader = new SourceDownloader() {
            @Override
            public SourceDescriptor inspect(String canonicalUrl) {
                return new SourceDescriptor("YuUeNsZgmyc", "Cena", 30_000);
            }

            @Override
            public Path download(String canonicalUrl, Path targetDirectory) throws Exception {
                downloads.incrementAndGet();
                Path file = targetDirectory.resolve("source.mp4");
                Files.writeString(file, "media-" + downloads.get());
                return file;
            }
        };

        Path cacheDir = tempDir.resolve("cache");
        SourceAcquisitionService service = new SourceAcquisitionService(
            new SourceUrlCanonicalizer(),
            new FileSourceCacheRepository(cacheDir),
            downloader,
            cacheDir.resolve(".incoming")
        );

        SourceMedia first = service.acquire("https://youtu.be/YuUeNsZgmyc");
        Files.delete(first.localPath());
        SourceMedia second = service.acquire("https://youtu.be/YuUeNsZgmyc");

        assertEquals(2, downloads.get());
        assertFalse(second.cacheHit());
        assertTrue(Files.isRegularFile(second.localPath()));
    }
}
