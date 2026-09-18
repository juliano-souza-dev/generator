package br.com.immersionhub.generator.desktop.ui;

import br.com.immersionhub.generator.desktop.model.SourceMedia;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public final class WaveView {
    private final VBox root = new VBox(18);

    public WaveView(SourceMedia sourceMedia, Runnable backAction) {
        root.setPadding(new Insets(42));
        root.getStyleClass().add("content-page");

        Label eyebrow = new Label("ETAPA 02 · WAVE");
        eyebrow.getStyleClass().add("eyebrow");

        Label title = new Label("Wave Editor");
        title.getStyleClass().add("page-title");

        Label source = new Label("Fonte pronta · " + sourceMedia.title() + " · " + formatDuration(sourceMedia.durationMs()));
        source.getStyleClass().add("path-chip");

        Label placeholder = new Label(
            "A mídia local foi entregue ao Wave. A waveform e o recorte serão implementados pelo Timing Editor Agent."
        );
        placeholder.getStyleClass().add("wave-placeholder");
        placeholder.setWrapText(true);

        Button back = new Button("Voltar para Source");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(event -> backAction.run());

        root.getChildren().addAll(eyebrow, title, source, placeholder, back);
    }

    public Parent root() { return root; }

    private static String formatDuration(long durationMs) {
        long totalSeconds = Math.max(0, durationMs / 1000);
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return "%d:%02d".formatted(minutes, seconds);
    }
}
