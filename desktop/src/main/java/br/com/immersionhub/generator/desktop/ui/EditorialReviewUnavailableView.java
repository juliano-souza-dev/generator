package br.com.immersionhub.generator.desktop.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class EditorialReviewUnavailableView {
    private final VBox root = new VBox(14);

    public EditorialReviewUnavailableView(Runnable retryAction, Runnable backAction) {
        root.setPadding(new Insets(36));
        root.getStyleClass().add("content-page");

        Label eyebrow = new Label("ETAPA 05 · REVISÃO");
        eyebrow.getStyleClass().add("eyebrow");

        Label title = new Label("A revisão salva precisa de atenção");
        title.getStyleClass().add("page-title");

        Label copy = new Label(
            "A tradução foi preservada, mas a revisão salva não pôde ser aberta com segurança. Tente novamente ou volte para a tradução."
        );
        copy.setWrapText(true);
        copy.getStyleClass().add("page-copy");

        Button retry = new Button("Tentar novamente");
        retry.getStyleClass().add("primary-button");
        retry.setOnAction(event -> retryAction.run());

        Button back = new Button("← Tradução");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(event -> backAction.run());

        HBox actions = new HBox(10, back, retry);
        actions.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(eyebrow, title, copy, actions);
    }

    public Parent root() {
        return root;
    }
}
