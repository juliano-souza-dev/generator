package br.com.immersionhub.generator.desktop.project;

import java.util.List;

public record ProjectListing(List<ProjectState> projects, int unreadableCount) {
    public ProjectListing {
        projects = List.copyOf(projects);
        if (unreadableCount < 0) throw new IllegalArgumentException("unreadableCount inválido.");
    }
}
