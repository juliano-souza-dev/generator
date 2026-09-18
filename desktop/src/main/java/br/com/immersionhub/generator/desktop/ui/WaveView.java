package br.com.immersionhub.generator.desktop.ui;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public final class WaveView {
    private final VBox root = new VBox(18);

    public WaveView(Runnable backAction) {
        root.setPadding(new Insets(42));
        root.getStyleClass().add("content-page");

        Label eyebrow = new Label("ETAPA 02 · WAVE");
        eyebrow.getStyleClass().add("eyebrow");

        Label title = new Label("Wave Editor");
        title.getStyleClass().add("page-title");

        Label proof = new Label(
            "Se esta tela apareceu na mesma janela, o primeiro contrato da nova arquitetura Java está funcionando."
        );
        proof.getStyleClass().add("page-copy");
        proof.setWrapText(true);

        Label placeholder = new Label("A waveform será implementada nesta mesma tela, sem criar outra Stage.");
        placeholder.getStyleClass().add("wave-placeholder");

        Button back = new Button("Voltar para Source");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(event -> backAction.run());

        root.getChildren().addAll(eyebrow, title, proof, placeholder, back);
    }

    public Parent root() { return root; }
}
