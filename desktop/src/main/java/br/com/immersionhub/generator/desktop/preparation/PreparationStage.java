package br.com.immersionhub.generator.desktop.preparation;

public enum PreparationStage {
    PREPARING("Preparando material…"),
    PREPARING_AUDIO("Preparando áudio…"),
    TRANSCRIBING("Transcrevendo material…"),
    TRANSCRIPTION_READY("Transcrição concluída."),
    ALIGNING("Ajustando tempos…"),
    ALIGNMENT_READY("Tempos ajustados."),
    USING_BASE_TIMINGS("Usando tempos preparados."),
    READY("Material pronto.");

    private final String userMessage;

    PreparationStage(String userMessage) {
        this.userMessage = userMessage;
    }

    public String userMessage() {
        return userMessage;
    }
}
