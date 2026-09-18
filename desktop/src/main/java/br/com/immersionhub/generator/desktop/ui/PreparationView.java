package br.com.immersionhub.generator.desktop.ui;

import br.com.immersionhub.generator.desktop.model.MediaCut;
import br.com.immersionhub.generator.desktop.preparation.AlignedMaterial;
import br.com.immersionhub.generator.desktop.preparation.PreparationPipeline;
import br.com.immersionhub.generator.desktop.preparation.PreparationStage;
import br.com.immersionhub.generator.desktop.timing.Timecode;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Objects;
import java.util.function.Consumer;

public final class PreparationView {
    private final VBox root = new VBox(16);
    private final MediaCut cut;
    private final PreparationPipeline pipeline;
    private final Consumer<AlignedMaterial> completedAction;
    private final Label status = new Label("Preparando material…");
    private final Label detail = new Label("Isso pode levar alguns instantes.");
    private final ProgressIndicator progress = new ProgressIndicator();
    private final Button retry = new Button("Tentar novamente");

    public PreparationView(
        MediaCut cut,
        PreparationPipeline pipeline,
        Consumer<AlignedMaterial> completedAction,
        Runnable backAction
    ) {
        this.cut = Objects.requireNonNull(cut);
        this.pipeline = Objects.requireNonNull(pipeline);
        this.completedAction = Objects.requireNonNull(completedAction);

        root.setPadding(new Insets(28, 34, 28, 34));
        root.getStyleClass().add("content-page");

        Label eyebrow = new Label("ETAPA 03 · PREPARAÇÃO");
        eyebrow.getStyleClass().add("eyebrow");

        Label title = new Label("Preparação do material");
        title.getStyleClass().add("page-title");

        Label source = new Label(
            "Recorte aprovado · " + Timecode.format(cut.startMs()) + " → " + Timecode.format(cut.endMs())
        );
        source.getStyleClass().add("path-chip");

        status.getStyleClass().add("page-copy");
        status.setWrapText(true);
        detail.getStyleClass().add("page-copy");
        detail.setWrapText(true);

        progress.setPrefSize(42, 42);

        Button back = new Button("← Wave");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(event -> backAction.run());

        retry.getStyleClass().add("primary-button");
        retry.setVisible(false);
        retry.setManaged(false);
        retry.setOnAction(event -> start());

        HBox actions = new HBox(10, back, retry);
        actions.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(eyebrow, title, source, progress, status, detail, actions);
        start();
    }

    public Parent root() {
        return root;
    }

    private void start() {
        retry.setVisible(false);
        retry.setManaged(false);
        progress.setVisible(true);
        status.setText(PreparationStage.PREPARING.userMessage());
        detail.setText("Isso pode levar alguns instantes.");

        Task<AlignedMaterial> task = new Task<>() {
            @Override
            protected AlignedMaterial call() throws Exception {
                return pipeline.prepare(cut, stage -> updateMessage(stage.userMessage()));
            }
        };

        task.messageProperty().addListener((obs, oldMessage, newMessage) -> {
            if (newMessage != null && !newMessage.isBlank()) status.setText(newMessage);
        });

        task.setOnSucceeded(event -> {
            AlignedMaterial material = task.getValue();
            progress.setVisible(false);
            status.setText("Material pronto.");
            detail.setText("Transcrição e tempos preparados para a próxima etapa.");
            completedAction.accept(material);
        });

        task.setOnFailed(event -> {
            progress.setVisible(false);
            status.setText("Não foi possível preparar o material.");
            detail.setText("Tente novamente. Os detalhes técnicos foram registrados no log.");
            retry.setVisible(true);
            retry.setManaged(true);
        });

        Thread worker = new Thread(task, "material-preparation");
        worker.setDaemon(true);
        worker.start();
    }
}
