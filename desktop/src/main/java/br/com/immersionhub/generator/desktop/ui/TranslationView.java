package br.com.immersionhub.generator.desktop.ui;

import br.com.immersionhub.generator.desktop.preparation.AlignedMaterial;
import br.com.immersionhub.generator.desktop.translation.ExternalAiPackage;
import br.com.immersionhub.generator.desktop.translation.TranslationMaterial;
import br.com.immersionhub.generator.desktop.translation.TranslationModule;
import br.com.immersionhub.generator.desktop.translation.TranslationOutcome;
import br.com.immersionhub.generator.desktop.translation.TranslationService;
import javafx.application.Platform;
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
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class TranslationView {
    private final VBox root = new VBox(16);
    private final String projectId;
    private final AlignedMaterial aligned;
    private final Consumer<TranslationMaterial> completedAction;
    private final BooleanSupplier configureGroq;

    private final Label status = new Label("Preparando tradução…");
    private final Label detail = new Label("O pacote externo ficará disponível independentemente da tradução automática.");
    private final ProgressIndicator progress = new ProgressIndicator();
    private final TextArea progressLog = new TextArea();
    private final Button translate = new Button("Traduzir com Groq");
    private final Button openPackage = new Button("Abrir pacote externo");
    private final Button importExternal = new Button("Importar retorno externo");

    private ExternalAiPackage externalPackage;
    private TranslationMaterial baseMaterial;
    private TranslationMaterial translatedMaterial;

    public TranslationView(
        String projectId,
        AlignedMaterial aligned,
        TranslationMaterial existingTranslation,
        Consumer<TranslationMaterial> completedAction,
        Runnable backAction,
        BooleanSupplier configureGroq
    ) {
        this.projectId = Objects.requireNonNull(projectId);
        this.aligned = Objects.requireNonNull(aligned);
        this.completedAction = Objects.requireNonNull(completedAction);
        this.configureGroq = Objects.requireNonNull(configureGroq);
        this.translatedMaterial = existingTranslation;

        root.setPadding(new Insets(28, 34, 28, 34));
        root.getStyleClass().add("content-page");

        Label eyebrow = new Label("ETAPA 04 · TRADUÇÃO");
        eyebrow.getStyleClass().add("eyebrow");

        Label title = new Label("Tradução das legendas");
        title.getStyleClass().add("page-title");

        Label copy = new Label(
            "A tradução automática é o caminho principal. O pacote para processamento externo permanece disponível o tempo todo."
        );
        copy.getStyleClass().add("page-copy");
        copy.setWrapText(true);

        status.getStyleClass().add("page-copy");
        status.setWrapText(true);
        detail.getStyleClass().add("page-copy");
        detail.setWrapText(true);

        progress.setPrefSize(32, 32);
        progress.setVisible(false);

        progressLog.setEditable(false);
        progressLog.setWrapText(true);
        progressLog.setPrefRowCount(6);
        progressLog.setFocusTraversable(false);
        progressLog.getStyleClass().add("preparation-progress-log");

        translate.getStyleClass().add("primary-button");
        openPackage.getStyleClass().add("secondary-button");
        importExternal.getStyleClass().add("secondary-button");
        openPackage.setDisable(true);
        importExternal.setDisable(true);

        Button back = new Button("← Preparação");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(event -> backAction.run());

        translate.setOnAction(event -> translate());
        openPackage.setOnAction(event -> openExternalPackage());
        importExternal.setOnAction(event -> importExternal());

        HBox actions = new HBox(10, back, translate, openPackage, importExternal);
        actions.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(
            eyebrow, title, copy, progress, status, detail, progressLog, actions
        );

        if (existingTranslation != null && existingTranslation.complete()) {
            status.setText("Traduções prontas.");
            detail.setText(
                existingTranslation.source() == br.com.immersionhub.generator.desktop.translation.TranslationSource.GROQ
                    ? "O material foi traduzido automaticamente e validado."
                    : "O retorno externo foi validado e aplicado."
            );
            translate.setDisable(true);
            importExternal.setDisable(true);
        }

        prepareExternalPackage();
    }

    public Parent root() {
        return root;
    }

    private void prepareExternalPackage() {
        progress.setVisible(true);
        appendProgress("Preparando opção externa…");

        Task<ExternalAiPackage> task = new Task<>() {
            @Override
            protected ExternalAiPackage call() throws Exception {
                return TranslationModule.prepareExternalPackage(projectId, aligned);
            }
        };

        task.setOnSucceeded(event -> {
            externalPackage = task.getValue();
            baseMaterial = br.com.immersionhub.generator.desktop.translation.TranslationMaterialFactory.base(aligned);
            progress.setVisible(false);
            openPackage.setDisable(false);
            importExternal.setDisable(translatedMaterial != null);
            appendProgress("Pacote externo pronto.");
            if (translatedMaterial == null) {
                status.setText("Pronto para traduzir.");
                detail.setText("Você pode usar a tradução automática ou o pacote externo.");
            }
        });

        task.setOnFailed(event -> {
            progress.setVisible(false);
            status.setText("Não foi possível preparar a opção externa.");
            detail.setText("A tradução automática não será iniciada até o pacote externo estar seguro.");
            translate.setDisable(true);
            appendProgress("Falha ao preparar a opção externa.");
        });

        Thread worker = new Thread(task, "translation-external-package");
        worker.setDaemon(true);
        worker.start();
    }

    private void translate() {
        if (externalPackage == null) {
            status.setText("Aguarde a preparação do pacote externo.");
            return;
        }

        if (!TranslationModule.hasApiKey()) {
            boolean configured = configureGroq.getAsBoolean();
            if (!configured || !TranslationModule.hasApiKey()) {
                status.setText("Configure sua chave Groq para continuar.");
                detail.setText("A chave é configurada uma única vez e vale para todos os projetos.");
                return;
            }
        }

        translate.setDisable(true);
        importExternal.setDisable(true);
        progress.setVisible(true);
        appendProgress("Iniciando tradução…");

        TranslationService service = TranslationModule.createService(projectId);
        Task<TranslationOutcome> task = new Task<>() {
            @Override
            protected TranslationOutcome call() throws Exception {
                return service.translate(aligned, stage -> {
                    updateMessage(stage.userMessage());
                    Platform.runLater(() -> appendProgress(stage.userMessage()));
                });
            }
        };

        task.messageProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && !newValue.isBlank()) status.setText(newValue);
        });

        task.setOnSucceeded(event -> {
            progress.setVisible(false);
            TranslationOutcome outcome = task.getValue();
            baseMaterial = outcome.baseMaterial();
            externalPackage = outcome.externalPackage();

            if (outcome.translated()) {
                translatedMaterial = outcome.translatedMaterial();
                status.setText("Traduções prontas.");
                detail.setText("A tradução foi concluída e validada.");
                completedAction.accept(translatedMaterial);
                importExternal.setDisable(true);
            } else {
                status.setText("A tradução automática não concluiu todo o material.");
                detail.setText("O pacote externo já está pronto para continuar sem perder o trabalho.");
                translate.setDisable(false);
                importExternal.setDisable(false);
            }
        });

        task.setOnFailed(event -> {
            progress.setVisible(false);
            translate.setDisable(false);
            importExternal.setDisable(false);
            appendProgress("A tradução automática foi interrompida.");

            if (TranslationModule.authenticationFailure(task.getException())) {
                status.setText("Sua chave Groq precisa ser atualizada.");
                detail.setText("Abra a configuração, valide uma nova chave e tente novamente.");
                configureGroq.getAsBoolean();
            } else {
                status.setText("Não foi possível concluir a tradução.");
                detail.setText("O pacote externo continua disponível.");
            }
        });

        Thread worker = new Thread(task, "groq-translation");
        worker.setDaemon(true);
        worker.start();
    }

    private void importExternal() {
        if (baseMaterial == null) {
            status.setText("Aguarde a preparação do material.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Importar retorno externo");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Retorno JSON ou ZIP", "*.json", "*.zip")
        );
        var selected = chooser.showOpenDialog(root.getScene().getWindow());
        if (selected == null) return;

        importExternal.setDisable(true);
        progress.setVisible(true);
        status.setText("Validando retorno externo…");

        Task<TranslationMaterial> task = new Task<>() {
            @Override
            protected TranslationMaterial call() throws Exception {
                return TranslationModule.createService(projectId)
                    .importExternal(selected.toPath(), baseMaterial);
            }
        };

        task.setOnSucceeded(event -> {
            translatedMaterial = task.getValue();
            progress.setVisible(false);
            status.setText("Traduções prontas.");
            detail.setText("O retorno externo foi validado e aplicado.");
            translate.setDisable(true);
            importExternal.setDisable(true);
            appendProgress("Retorno externo validado.");
            completedAction.accept(translatedMaterial);
        });

        task.setOnFailed(event -> {
            progress.setVisible(false);
            status.setText("O retorno externo não passou na validação.");
            detail.setText("Corrija ou gere novamente o retorno. O último material válido foi preservado.");
            importExternal.setDisable(false);
            appendProgress("Retorno externo recusado.");
        });

        Thread worker = new Thread(task, "external-translation-import");
        worker.setDaemon(true);
        worker.start();
    }

    private void openExternalPackage() {
        if (externalPackage == null) return;
        try {
            Path directory = externalPackage.directory();
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(directory.toFile());
            } else {
                status.setText("O pacote externo está pronto.");
            }
        } catch (Exception exception) {
            status.setText("O pacote externo está pronto, mas a pasta não pôde ser aberta automaticamente.");
        }
    }

    private void appendProgress(String message) {
        if (message == null || message.isBlank()) return;
        if (!progressLog.getText().isEmpty()) progressLog.appendText(System.lineSeparator());
        progressLog.appendText(message);
        progressLog.positionCaret(progressLog.getLength());
    }
}
