package br.com.immersionhub.generator.desktop.timing;

public final class TimingFeedback {
    private TimingFeedback() {}

    public static String playbackFailure() {
        return "Não foi possível abrir a prévia desta fonte.";
    }

    public static String waveformFailure() {
        return "Não foi possível preparar a waveform. Tente novamente.";
    }

    public static String cutFailure() {
        return "Não foi possível salvar o recorte. Revise IN e OUT e tente novamente.";
    }
}
