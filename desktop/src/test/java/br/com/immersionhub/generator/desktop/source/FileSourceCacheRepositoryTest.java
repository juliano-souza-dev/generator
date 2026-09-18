package br.com.immersionhub.generator.desktop.source;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSourceCacheRepositoryTest {
    @Test
    void corruptMetadataIsIgnoredSafely(@TempDir Path tempDir) throws Exception {
        String url = "https://www.youtube.com/watch?v=YuUeNsZgmyc";
        String sourceId = SourceIds.fromCanonicalUrl(url);
        Path entry = tempDir.resolve(sourceId);
        Files.createDirectories(entry);
        Files.writeString(entry.resolve("source.json"), "{broken-json");

        FileSourceCacheRepository repository = new FileSourceCacheRepository(tempDir);
        assertTrue(repository.find(sourceId, url).isEmpty());
    }
}
