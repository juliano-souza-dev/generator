package br.com.immersionhub.generator.desktop.timing;

import br.com.immersionhub.generator.desktop.infrastructure.AppDirectories;

public final class TimingModule {
    private TimingModule() {}

    public static MediaProcessor createProcessor() {
        return new FfmpegMediaProcessor();
    }

    public static MediaCutRepository createRepository() {
        return new MediaCutRepository(AppDirectories.workspaceDir().resolve("timing").resolve("media-cut.json"));
    }
}
