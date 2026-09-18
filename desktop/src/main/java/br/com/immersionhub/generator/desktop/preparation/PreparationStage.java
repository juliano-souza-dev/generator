package br.com.immersionhub.generator.desktop.preparation;

public enum PreparationStage {
    PREPARING("Preparando material…"),
    ALIGNING("Ajustando tempos…"),
    READY("Material pronto.");

    private final String userMessage;

    PreparationStage(String userMessage) {
        this.userMessage = userMessage;
    }

    public String userMessage() {
        return userMessage;
    }
}
