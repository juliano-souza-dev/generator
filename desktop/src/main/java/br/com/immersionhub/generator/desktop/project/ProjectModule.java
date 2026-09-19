package br.com.immersionhub.generator.desktop.project;

import br.com.immersionhub.generator.desktop.infrastructure.AppDirectories;

public final class ProjectModule {
    private ProjectModule() {}

    public static ProjectRepository createRepository() {
        return new ProjectRepository(AppDirectories.projectsDir());
    }
}
