package br.com.immersionhub.generator.desktop.source;

import br.com.immersionhub.generator.desktop.model.SourceMedia;

import java.nio.file.Path;
import java.util.Optional;

public interface SourceCacheRepository {
    Optional<SourceMedia> find(String sourceId, String canonicalUrl);
    SourceMedia store(String sourceId, String canonicalUrl, SourceDescriptor descriptor, Path downloadedFile) throws Exception;
}
