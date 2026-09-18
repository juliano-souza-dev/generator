package br.com.immersionhub.generator.desktop.ui;

import br.com.immersionhub.generator.desktop.model.SourceMedia;
import br.com.immersionhub.generator.desktop.source.SourceAcquisitionService;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public final class SourceView {
    private final VBox root = new VBox(18);
    private SourceMedia readyMedia;

    public SourceView(
        SourceAcquisitionService sourceService,
        SourceMedia existingMedia,
        Consumer<SourceMedia> continueAction,
        Runnable invalidateAction
    ) {
        readyMedia = existingMedia;

        root.setPadding(new Insets(42));
        root.getStyleClass().add("content-page");

        Label eyebrow = new Label("ETAPA 01 · SOURCE");
        eyebrow.getStyleClass().add("eyebrow");

        Label title = new Label("Fonte do conteúdo");
        title.getStyleClass().add("page-title");

        Label copy = new Label(
            "Cole o link do vídeo. O Generator prepara a fonte uma vez e reutiliza o conteúdo quando você usar o mesmo link novamente."
        );
        copy.getStyleClass().add("page-copy");
        copy.setWrapText(true);

        TextField url = new TextField();
        url.setPromptText("https://www.youtube.com/watch?v=...");
        url.setPrefWidth(720);
        url.getStyleClass().add("source-field");

        Button prepare = new Button("Preparar fonte");
        prepare.getStyleClass().add("secondary-button");

        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(24, 24);
        progress.setVisible(false);

        Label status = new Label("Informe uma URL para preparar a fonte.");
        status.getStyleClass().add("page-copy");
        status.setWrapText(true);

        Button next = new Button("Continuar para Wave");
        next.getStyleClass().add("primary-button");
        next.setDisable(existingMedia == null);

        if (existingMedia != null) {
            url.setText(existingMedia.canonicalUrl());
            status.setText("Fonte pronta: " + existingMedia.title());
        }

        url.textProperty().addListener((observable, previous, current) -> {
            if (readyMedia == null) return;
            if (current != null && current.trim().equals(readyMedia.canonicalUrl())) return;

            readyMedia = null;
            next.setDisable(true);
            status.setText("A URL mudou. Prepare a nova fonte antes de continuar.");
            invalidateAction.run();
        });

        prepare.setOnAction(event -> {
            String rawUrl = url.getText();
            readyMedia = null;
            next.setDisable(true);
            invalidateAction.run();
            prepare.setDisable(true);
            url.setDisable(true);
            progress.setVisible(true);
            status.setText("Preparando a fonte…");

            Task<SourceMedia> task = new Task<>() {
                @Override
                protected SourceMedia call() throws Exception {
                    return sourceService.acquire(rawUrl);
                }
            };

            task.setOnSucceeded(done -> {
                readyMedia = task.getValue();
                status.setText(readyMedia.cacheHit()
                    ? "Fonte pronta. O conteúdo já estava disponível e foi reutilizado."
                    : "Fonte pronta. O conteúdo foi preparado com sucesso.");
                prepare.setDisable(false);
                url.setDisable(false);
                progress.setVisible(false);
                next.setDisable(false);
            });

            task.setOnFailed(done -> {
                Throwable failure = task.getException();
                String message = failure == null || failure.getMessage() == null || failure.getMessage().isBlank()
                    ? "Não foi possível preparar essa fonte."
                    : failure.getMessage();
                status.setText(message);
                prepare.setDisable(false);
                url.setDisable(false);
                progress.setVisible(false);
            });

            Thread worker = new Thread(task, "source-acquisition");
            worker.setDaemon(true);
            worker.start();
        });

        next.setOnAction(event -> {
            if (readyMedia != null) continueAction.accept(readyMedia);
        });

        HBox actions = new HBox(10, prepare, progress);
        root.getChildren().addAll(eyebrow, title, copy, url, actions, status, next);
    }

    public Parent root() { return root; }
}
