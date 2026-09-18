package br.com.immersionhub.generator.desktop.ui;

import br.com.immersionhub.generator.desktop.infrastructure.AppDirectories;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public final class SourceView {
    private final VBox root = new VBox(18);

    public SourceView(Runnable continueAction) {
        root.setPadding(new Insets(42));
        root.getStyleClass().add("content-page");

        Label eyebrow = new Label("ETAPA 01 · SOURCE");
        eyebrow.getStyleClass().add("eyebrow");

        Label title = new Label("Fonte do conteúdo");
        title.getStyleClass().add("page-title");

        Label copy = new Label(
            "Primeiro build Java nativo. A próxima etapa substitui este conteúdo sem abrir outra janela."
        );
        copy.getStyleClass().add("page-copy");
        copy.setWrapText(true);

        Label cache = new Label("Source Cache: " + AppDirectories.sourceCacheDir());
        cache.getStyleClass().add("path-chip");
        cache.setWrapText(true);

        TextField url = new TextField();
        url.setPromptText("Cole uma URL do YouTube");
        url.setPrefWidth(720);
        url.getStyleClass().add("source-field");

        Button next = new Button("Continuar para Wave");
        next.getStyleClass().add("primary-button");
        next.setOnAction(event -> continueAction.run());

        root.getChildren().addAll(eyebrow, title, copy, cache, url, next);
    }

    public Parent root() { return root; }
}
