package br.com.immersionhub.generator.desktop.ui;

import br.com.immersionhub.generator.desktop.editorial.EditorialCue;
import br.com.immersionhub.generator.desktop.editorial.EditorialMaterial;
import br.com.immersionhub.generator.desktop.editorial.EditorialReviewService;
import br.com.immersionhub.generator.desktop.editorial.EditorialReviewStatus;
import br.com.immersionhub.generator.desktop.translation.TranslationMaterial;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.nio.file.Path;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.BiConsumer;

public final class EditorialReviewView {
    private final VBox root = new VBox(14);
    private final EditorialReviewService service;
    private final TranslationMaterial translation;
    private final Path mediaPath;
    private final BiConsumer<EditorialMaterial, Integer> progressAction;
    private final Runnable backAction;

    private EditorialMaterial material;
    private int currentOrder;
    private MediaPlayer player;
    private long playUntilMs;

    private final Label progressLabel = new Label();
    private final Label reviewState = new Label();
    private final Label status = new Label();

    private final TextArea originalEn = readOnlyArea();
    private final TextArea approvedEn = editableArea();
    private final TextArea pt = editableArea();

    private final Button previous = secondary("← Anterior");
    private final Button next = secondary("Próxima →");
    private final Button nextPending = secondary("Próxima pendente");
    private final Button listen = secondary("▶ Ouvir cue");
    private final Button restore = secondary("Restaurar sugestão");
    private final Button save = secondary("Salvar");
    private final Button approve = new Button("Aprovar");

    private boolean loadingFields;

    public EditorialReviewView(
        EditorialReviewService service,
        TranslationMaterial translation,
        EditorialMaterial material,
        int initialCueOrder,
        Path mediaPath,
        BiConsumer<EditorialMaterial, Integer> progressAction,
        Runnable backAction
    ) {
        this.service = Objects.requireNonNull(service);
        this.translation = Objects.requireNonNull(translation);
        this.material = Objects.requireNonNull(material);
        this.mediaPath = Objects.requireNonNull(mediaPath).toAbsolutePath().normalize();
        this.progressAction = Objects.requireNonNull(progressAction);
        this.backAction = Objects.requireNonNull(backAction);
        this.currentOrder = Math.max(1, Math.min(material.cues().size(), initialCueOrder));

        root.setPadding(new Insets(28, 34, 28, 34));
        root.getStyleClass().add("content-page");

        Label eyebrow = new Label("ETAPA 05 · REVISÃO");
        eyebrow.getStyleClass().add("eyebrow");

        Label title = new Label("Revisão das legendas");
        title.getStyleClass().add("page-title");

        Label copy = new Label(
            "Revise cada cue antes de seguir. O original fica protegido e as alterações só são aprovadas quando você confirmar."
        );
        copy.getStyleClass().add("page-copy");
        copy.setWrapText(true);

        progressLabel.getStyleClass().add("review-progress");
        reviewState.getStyleClass().add("review-state");
        status.getStyleClass().add("page-copy");
        status.setWrapText(true);

        approve.getStyleClass().add("primary-button");

        previous.setOnAction(event -> navigate(currentOrder - 1));
        next.setOnAction(event -> navigate(currentOrder + 1));
        nextPending.setOnAction(event -> navigateToNextPending());
        listen.setOnAction(event -> playCurrentCue());
        restore.setOnAction(event -> restoreSuggestion());
        save.setOnAction(event -> saveDraft());
        approve.setOnAction(event -> approveCue());

        approvedEn.textProperty().addListener((obs, oldValue, newValue) -> markDirtyIfNeeded());
        pt.textProperty().addListener((obs, oldValue, newValue) -> markDirtyIfNeeded());

        Button back = secondary("← Tradução");
        back.setOnAction(event -> {
            if (persistDraftIfChanged()) {
                disposePlayer();
                backAction.run();
            }
        });

        HBox nav = new HBox(8, previous, next, nextPending);
        nav.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(10, back, listen, restore, spacer, save, approve);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox originalBox = field("Original EN · protegido", originalEn);
        VBox approvedBox = field("English aprovado", approvedEn);
        VBox ptBox = field("Português", pt);

        root.getChildren().addAll(
            eyebrow,
            title,
            copy,
            new HBox(12, progressLabel, reviewState),
            nav,
            originalBox,
            approvedBox,
            ptBox,
            status,
            actions
        );

        loadCue(currentOrder);
    }

    public Parent root() {
        return root;
    }

    private void loadCue(int cueOrder) {
        if (cueOrder < 1 || cueOrder > material.cues().size()) return;

        currentOrder = cueOrder;
        EditorialCue cue = currentCue();

        loadingFields = true;
        originalEn.setText(cue.originalEn());
        approvedEn.setText(cue.approvedEn());
        pt.setText(cue.pt());
        loadingFields = false;

        updateLabels();
        try {
            service.saveCursor(material, currentOrder);
            progressAction.accept(material, currentOrder);
        } catch (Exception exception) {
            status.setText("Não foi possível salvar o ponto atual da revisão.");
        }
    }

    private void navigate(int targetOrder) {
        if (targetOrder < 1 || targetOrder > material.cues().size()) return;
        if (!persistDraftIfChanged()) return;
        stopPlayback();
        loadCue(targetOrder);
    }

    private void navigateToNextPending() {
        if (!persistDraftIfChanged()) return;
        OptionalInt pending = service.nextPending(material, currentOrder);
        if (pending.isPresent()) {
            stopPlayback();
            loadCue(pending.getAsInt());
        } else {
            status.setText("Todas as cues estão aprovadas.");
            updateLabels();
        }
    }

    private void saveDraft() {
        try {
            material = service.saveDraft(
                material,
                currentOrder,
                approvedEn.getText(),
                pt.getText()
            );
            status.setText("Alterações salvas. Esta cue ainda precisa ser aprovada.");
            updateLabels();
            progressAction.accept(material, currentOrder);
        } catch (Exception exception) {
            status.setText("Revise os campos antes de salvar.");
        }
    }

    private void approveCue() {
        try {
            material = service.approve(
                material,
                currentOrder,
                approvedEn.getText(),
                pt.getText()
            );
            status.setText("Cue aprovada.");
            updateLabels();
            progressAction.accept(material, currentOrder);

            OptionalInt pending = service.nextPending(material, currentOrder);
            if (pending.isPresent()) {
                stopPlayback();
                loadCue(pending.getAsInt());
            } else {
                status.setText("Todas as cues estão aprovadas. A revisão de cues foi concluída.");
                updateLabels();
            }
        } catch (Exception exception) {
            status.setText("Preencha English e Português antes de aprovar.");
        }
    }

    private void restoreSuggestion() {
        try {
            material = service.restoreSuggestion(material, translation, currentOrder);
            EditorialCue cue = currentCue();

            loadingFields = true;
            approvedEn.setText(cue.approvedEn());
            pt.setText(cue.pt());
            loadingFields = false;

            status.setText("Sugestão restaurada. Revise e aprove novamente.");
            updateLabels();
            progressAction.accept(material, currentOrder);
        } catch (Exception exception) {
            status.setText("Não foi possível restaurar a sugestão.");
        }
    }

    private boolean persistDraftIfChanged() {
        EditorialCue cue = currentCue();
        String currentEn = normalized(approvedEn.getText());
        String currentPt = normalized(pt.getText());

        if (currentEn.equals(cue.approvedEn()) && currentPt.equals(cue.pt())) {
            return true;
        }

        try {
            material = service.saveDraft(material, currentOrder, currentEn, currentPt);
            progressAction.accept(material, currentOrder);
            return true;
        } catch (Exception exception) {
            status.setText("Revise os campos antes de continuar.");
            return false;
        }
    }

    private void markDirtyIfNeeded() {
        if (loadingFields) return;
        EditorialCue cue = currentCue();
        boolean changed = !normalized(approvedEn.getText()).equals(cue.approvedEn())
            || !normalized(pt.getText()).equals(cue.pt());

        if (changed) {
            reviewState.setText("Alterada · pendente");
            reviewState.getStyleClass().removeAll("review-approved");
            if (!reviewState.getStyleClass().contains("review-pending")) {
                reviewState.getStyleClass().add("review-pending");
            }
        } else {
            updateReviewState(cue);
        }
    }

    private void updateLabels() {
        long approvedCount = material.cues().stream()
            .filter(cue -> cue.reviewStatus() == EditorialReviewStatus.APPROVED)
            .count();

        progressLabel.setText(
            "Cue %d de %d · %d aprovadas".formatted(currentOrder, material.cues().size(), approvedCount)
        );
        updateReviewState(currentCue());

        previous.setDisable(currentOrder <= 1);
        next.setDisable(currentOrder >= material.cues().size());
        nextPending.setDisable(material.cuesApproved());

        if (material.cuesApproved()) {
            status.setText("Todas as cues estão aprovadas.");
        }
    }

    private void updateReviewState(EditorialCue cue) {
        boolean approved = cue.reviewStatus() == EditorialReviewStatus.APPROVED;
        reviewState.setText(approved ? "Aprovada" : "Pendente");
        reviewState.getStyleClass().removeAll("review-approved", "review-pending");
        reviewState.getStyleClass().add(approved ? "review-approved" : "review-pending");
    }

    private void playCurrentCue() {
        try {
            if (player == null) {
                Media media = new Media(mediaPath.toUri().toString());
                player = new MediaPlayer(media);
                player.setOnError(() -> status.setText("Não foi possível reproduzir este trecho."));

                ChangeListener<Duration> listener = (obs, oldValue, newValue) -> {
                    if (player != null
                        && player.getStatus() == MediaPlayer.Status.PLAYING
                        && newValue.toMillis() >= playUntilMs) {
                        player.pause();
                    }
                };
                player.currentTimeProperty().addListener(listener);
            }

            EditorialCue cue = currentCue();
            playUntilMs = cue.speechEndMs();
            player.seek(Duration.millis(cue.speechStartMs()));
            player.play();
            status.setText("Reproduzindo cue " + currentOrder + "…");
        } catch (Exception exception) {
            status.setText("Não foi possível reproduzir este trecho.");
        }
    }

    private void stopPlayback() {
        if (player != null) player.pause();
    }

    private void disposePlayer() {
        if (player != null) {
            player.stop();
            player.dispose();
            player = null;
        }
    }

    private EditorialCue currentCue() {
        return material.cues().get(currentOrder - 1);
    }

    private VBox field(String labelText, TextArea area) {
        Label label = new Label(labelText);
        label.getStyleClass().add("eyebrow");
        VBox box = new VBox(6, label, area);
        VBox.setVgrow(area, Priority.NEVER);
        return box;
    }

    private static TextArea readOnlyArea() {
        TextArea area = editableArea();
        area.setEditable(false);
        area.getStyleClass().add("review-original");
        return area;
    }

    private static TextArea editableArea() {
        TextArea area = new TextArea();
        area.setWrapText(true);
        area.setPrefRowCount(3);
        area.getStyleClass().add("review-text-area");
        return area;
    }

    private static Button secondary(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("secondary-button");
        return button;
    }

    private static String normalized(String value) {
        return value == null ? "" : value.trim();
    }
}
