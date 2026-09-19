package br.com.immersionhub.generator.desktop.project;

public enum ProjectStage {
    SOURCE("Source"),
    WAVE("Wave"),
    PREPARATION("Preparação"),
    TRANSLATION("Tradução");

    private final String userLabel;

    ProjectStage(String userLabel) {
        this.userLabel = userLabel;
    }

    public String userLabel() {
        return userLabel;
    }
}
