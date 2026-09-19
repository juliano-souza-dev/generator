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
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.application.Platform;

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
    private final TextArea progressLog = new TextArea();
    private final Button retry = new Button("Tentar novamente");
    private int attempt;

    public PreparationView(
        MediaCut cut,
        PreparationPipeline pipeline,
        Consumer<AlignedMaterial> completedAction,
        Runnable backAction
    ) {
        this(cut, pipeline, completedAction, backAction, null, true);
    }

    public PreparationView(
        MediaCut cut,
        PreparationPipeline pipeline,
        Consumer<AlignedMaterial> completedAction,
        Runnable backAction,
        AlignedMaterial existingMaterial,
        boolean startAutomatically
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

        progressLog.setEditable(false);
        progressLog.setWrapText(true);
        progressLog.setPrefRowCount(7);
        progressLog.setFocusTraversable(false);
        progressLog.getStyleClass().add("preparation-progress-log");

        Button back = new Button("← Wave");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(event -> backAction.run());

        retry.getStyleClass().add("primary-button");
        retry.setVisible(false);
        retry.setManaged(false);
        retry.setOnAction(event -> start());

        HBox actions = new HBox(10, back, retry);
        actions.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(eyebrow, title, source, progress, status, detail, progressLog, actions);

        if (existingMaterial != null) {
            progress.setVisible(false);
            status.setText("Material pronto.");
            detail.setText(
                existingMaterial.timingSource() == br.com.immersionhub.generator.desktop.preparation.TimingSource.DTW_REFINED
                    ? "Transcrição e tempos preparados para a próxima etapa."
                    : "Material preparado com os tempos disponíveis."
            );
            appendProgress("Material já preparado. Nenhum processamento foi repetido.");
        } else if (startAutomatically) {
            start();
        } else {
            progress.setVisible(false);
            status.setText("Preparação pausada.");
            detail.setText("Continue quando estiver pronto. As etapas anteriores não serão refeitas.");
            retry.setText("Continuar preparação");
            retry.setVisible(true);
            retry.setManaged(true);
            appendProgress("Preparação pronta para continuar.");
        }
    }

    public Parent root() {
        return root;
    }

    private void start() {
        attempt++;
        retry.setText("Tentar novamente");
        appendProgress((attempt == 1 ? "" : System.lineSeparator()) + "Tentativa " + attempt);
        retry.setVisible(false);
        retry.setManaged(false);
        progress.setVisible(true);
        status.setText(PreparationStage.PREPARING.userMessage());
        detail.setText("Isso pode levar alguns instantes.");

        Task<AlignedMaterial> task = new Task<>() {
            @Override
            protected AlignedMaterial call() throws Exception {
                return pipeline.prepare(cut, stage -> {
                    updateMessage(stage.userMessage());
                    Platform.runLater(() -> appendProgress(stage.userMessage()));
                });
            }
        };

        task.messageProperty().addListener((obs, oldMessage, newMessage) -> {
            if (newMessage != null && !newMessage.isBlank()) status.setText(newMessage);
        });

        task.setOnSucceeded(event -> {
            AlignedMaterial material = task.getValue();
            progress.setVisible(false);
            status.setText("Material pronto.");
            detail.setText(
                material.timingSource() == br.com.immersionhub.generator.desktop.preparation.TimingSource.DTW_REFINED
                    ? "Transcrição e tempos preparados para a próxima etapa."
                    : "Material preparado com os tempos disponíveis."
            );
            completedAction.accept(material);
        });

        task.setOnFailed(event -> {
            progress.setVisible(false);
            status.setText("Não foi possível preparar o material.");
            detail.setText("Tente novamente. Os detalhes técnicos foram registrados no log.");
            appendProgress("Não foi possível concluir esta tentativa.");
            retry.setVisible(true);
            retry.setManaged(true);
        });

        Thread worker = new Thread(task, "material-preparation");
        worker.setDaemon(true);
        worker.start();
    }

    private void appendProgress(String message) {
        if (message == null || message.isBlank()) return;
        if (!progressLog.getText().isEmpty()) progressLog.appendText(System.lineSeparator());
        progressLog.appendText(message);
        progressLog.positionCaret(progressLog.getLength());
    }
}
