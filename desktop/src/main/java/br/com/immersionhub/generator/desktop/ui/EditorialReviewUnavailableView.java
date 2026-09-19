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

    public EditorialReviewUnavailableView(
        Runnable retryAction,
        Runnable rebuildAction,
        Runnable backAction
    ) {
        root.setPadding(new Insets(36));
        root.getStyleClass().add("content-page");

        Label eyebrow = new Label("ETAPA 05 · REVISÃO");
        eyebrow.getStyleClass().add("eyebrow");

        Label title = new Label("A revisão salva precisa de atenção");
        title.getStyleClass().add("page-title");

        Label copy = new Label(
            "A tradução e todas as etapas anteriores foram preservadas. Você pode verificar novamente ou recriar somente a revisão editorial."
        );
        copy.setWrapText(true);
        copy.getStyleClass().add("page-copy");

        Label warning = new Label(
            "Recriar a revisão descarta apenas as decisões editoriais que não puderam ser recuperadas."
        );
        warning.setWrapText(true);
        warning.getStyleClass().add("project-card-warning");

        Button retry = new Button("Verificar novamente");
        retry.getStyleClass().add("secondary-button");
        retry.setOnAction(event -> retryAction.run());

        Button rebuild = new Button("Recriar revisão");
        rebuild.getStyleClass().add("primary-button");
        rebuild.setOnAction(event -> rebuildAction.run());

        Button back = new Button("← Tradução");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(event -> backAction.run());

        HBox actions = new HBox(10, back, retry, rebuild);
        actions.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(eyebrow, title, copy, warning, actions);
    }

    public Parent root() {
        return root;
    }
}
