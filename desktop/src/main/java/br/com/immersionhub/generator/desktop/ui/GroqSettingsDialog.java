package br.com.immersionhub.generator.desktop.ui;

import br.com.immersionhub.generator.desktop.translation.TranslationModule;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

public final class GroqSettingsDialog {
    private GroqSettingsDialog() {}

    public static boolean show(Window owner) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Configurar Groq");
        if (owner != null) {
            dialog.initOwner(owner);
            dialog.initModality(Modality.WINDOW_MODAL);
        }

        Label title = new Label("Chave Groq");
        title.getStyleClass().add("page-title");

        Label copy = new Label(
            TranslationModule.hasApiKey()
                ? "Já existe uma chave configurada para o Generator. Informe uma nova chave para substituí-la."
                : "Configure uma chave uma única vez. Ela será reutilizada em todos os projetos."
        );
        copy.setWrapText(true);
        copy.getStyleClass().add("page-copy");

        PasswordField key = new PasswordField();
        key.setPromptText("API Key da Groq");
        key.setPrefWidth(430);
        key.getStyleClass().add("source-field");

        Label status = new Label();
        status.setWrapText(true);
        status.getStyleClass().add("page-copy");

        VBox content = new VBox(12, title, copy, key, status);
        content.setPadding(new Insets(10));
        content.setPrefWidth(480);

        ButtonType saveType = new ButtonType("Validar e salvar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, cancelType);
        dialog.getDialogPane().setContent(content);
        dialog.setResultConverter(button -> Boolean.FALSE);

        Node saveButton = dialog.getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            String value = key.getText() == null ? "" : key.getText().trim();
            if (value.isEmpty()) {
                status.setText("Informe sua chave Groq.");
                return;
            }

            saveButton.setDisable(true);
            key.setDisable(true);
            status.setText("Validando chave…");

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    TranslationModule.validateAndStoreApiKey(value);
                    return null;
                }
            };

            task.setOnSucceeded(done -> {
                key.clear();
                dialog.setResult(Boolean.TRUE);
                dialog.close();
            });

            task.setOnFailed(failed -> {
                key.clear();
                key.setDisable(false);
                saveButton.setDisable(false);
                status.setText("Não foi possível validar essa chave. Confira a chave e tente novamente.");
                key.requestFocus();
            });

            Thread worker = new Thread(task, "groq-key-validation");
            worker.setDaemon(true);
            worker.start();
        });

        return dialog.showAndWait().orElse(Boolean.FALSE);
    }
}
