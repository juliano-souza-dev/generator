package br.com.immersionhub.generator.desktop.source;

import br.com.immersionhub.generator.desktop.infrastructure.AppDirectories;
import br.com.immersionhub.generator.desktop.infrastructure.BundledTools;

public final class SourceModule {
    private SourceModule() {}

    public static SourceAcquisitionService createService() {
        return new SourceAcquisitionService(
            new SourceUrlCanonicalizer(),
            new FileSourceCacheRepository(AppDirectories.sourceCacheDir()),
            new YtDlpSourceDownloader(BundledTools.ytDlp()),
            AppDirectories.sourceCacheDir().resolve(".incoming")
        );
    }
}
