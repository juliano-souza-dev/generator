package br.com.immersionhub.generator.desktop.timing;

import br.com.immersionhub.generator.desktop.infrastructure.AppDirectories;

import java.nio.file.Path;

public final class TimingModule {
    private TimingModule() {}

    public static MediaProcessor createProcessor() {
        return new FfmpegMediaProcessor();
    }

    public static MediaCutRepository createRepository() {
        return new MediaCutRepository(AppDirectories.workspaceDir().resolve("timing").resolve("media-cut.json"));
    }

    public static TimingDraftRepository createDraftRepository() {
        return new TimingDraftRepository(AppDirectories.workspaceDir().resolve("timing").resolve("drafts"));
    }

    public static MediaCutRepository createRepository(String projectId) {
        return new MediaCutRepository(projectTimingDir(projectId).resolve("media-cut.json"));
    }

    public static TimingDraftRepository createDraftRepository(String projectId) {
        return new TimingDraftRepository(projectTimingDir(projectId).resolve("drafts"));
    }

    public static Path projectTimingDir(String projectId) {
        String safe = projectId.replaceAll("[^A-Za-z0-9._-]", "_");
        return AppDirectories.projectsDir().resolve(safe).resolve("timing");
    }
}
