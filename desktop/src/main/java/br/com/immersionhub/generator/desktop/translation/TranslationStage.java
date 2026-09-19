package br.com.immersionhub.generator.desktop.translation;

public enum TranslationStage {
    PREPARING_PACKAGE("Preparando opção externa…"),
    TRANSLATING("Traduzindo material…"),
    VALIDATING("Validando traduções…"),
    READY("Traduções prontas."),
    EXTERNAL_REQUIRED("O material externo está disponível.");

    private final String userMessage;

    TranslationStage(String userMessage) {
        this.userMessage = userMessage;
    }

    public String userMessage() {
        return userMessage;
    }
}
