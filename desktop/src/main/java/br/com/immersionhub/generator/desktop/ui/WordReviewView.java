package br.com.immersionhub.generator.desktop.ui;

import br.com.immersionhub.generator.desktop.editorial.EditorialCue;
import br.com.immersionhub.generator.desktop.editorial.EditorialMaterial;
import br.com.immersionhub.generator.desktop.editorial.EditorialReviewStatus;
import br.com.immersionhub.generator.desktop.editorial.EditorialWord;
import br.com.immersionhub.generator.desktop.editorial.WordReviewPosition;
import br.com.immersionhub.generator.desktop.editorial.WordReviewService;
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

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

public final class WordReviewView {
    private final VBox root = new VBox(14);
    private final WordReviewService service;
    private final BiConsumer<EditorialMaterial, WordReviewPosition> progressAction;
    private final Runnable backAction;

    private EditorialMaterial material;
    private WordReviewPosition position;

    private final Label progressLabel = new Label();
    private final Label cueProgressLabel = new Label();
    private final Label reviewState = new Label();
    private final Label status = new Label();

    private final TextArea cueContext = readOnlyArea();
    private final TextArea originalReference = readOnlyArea();
    private final TextArea english = editableArea();
    private final TextArea pt = editableArea();

    private final Button previous = secondary("← Anterior");
    private final Button next = secondary("Próxima →");
    private final Button nextPending = secondary("Próxima pendente");
    private final Button save = secondary("Salvar");
    private final Button approve = new Button("Aprovar");

    private boolean loadingFields;

    public WordReviewView(
        WordReviewService service,
        EditorialMaterial material,
        WordReviewPosition initialPosition,
        BiConsumer<EditorialMaterial, WordReviewPosition> progressAction,
        Runnable backAction
    ) {
        this.service = Objects.requireNonNull(service);
        this.material = Objects.requireNonNull(material);
        this.position = Objects.requireNonNull(initialPosition);
        this.progressAction = Objects.requireNonNull(progressAction);
        this.backAction = Objects.requireNonNull(backAction);

        root.setPadding(new Insets(28, 34, 28, 34));
        root.getStyleClass().add("content-page");

        Label eyebrow = new Label("ETAPA 05 · WORD BY WORD");
        eyebrow.getStyleClass().add("eyebrow");

        Label title = new Label("Revisão Word by Word");
        title.getStyleClass().add("page-title");

        Label copy = new Label(
            "Revise uma unidade por vez. Para trocar a palavra em English, volte à revisão da cue; aqui você preserva a estrutura já reconciliada."
        );
        copy.getStyleClass().add("page-copy");
        copy.setWrapText(true);

        progressLabel.getStyleClass().add("review-progress");
        cueProgressLabel.getStyleClass().add("project-card-meta");
        reviewState.getStyleClass().add("review-state");
        status.getStyleClass().add("page-copy");
        status.setWrapText(true);

        approve.getStyleClass().add("primary-button");

        previous.setOnAction(event -> navigate(service.previous(material, position)));
        next.setOnAction(event -> navigate(service.next(material, position)));
        nextPending.setOnAction(event -> navigateToNextPending());
        save.setOnAction(event -> saveDraft());
        approve.setOnAction(event -> approveWord());

        english.textProperty().addListener((obs, oldValue, newValue) -> markDirtyIfNeeded());
        pt.textProperty().addListener((obs, oldValue, newValue) -> markDirtyIfNeeded());

        Button back = secondary("← Revisão de cues");
        back.setOnAction(event -> {
            if (persistDraftIfChanged()) backAction.run();
        });

        HBox nav = new HBox(8, previous, next, nextPending);
        nav.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(10, back, spacer, save, approve);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox contextBox = field("Contexto da cue", cueContext);
        VBox originalBox = field("Referência original", originalReference);
        VBox englishBox = field("English", english);
        VBox ptBox = field("Português", pt);

        root.getChildren().addAll(
            eyebrow,
            title,
            copy,
            new HBox(12, progressLabel, reviewState),
            cueProgressLabel,
            nav,
            contextBox,
            originalBox,
            englishBox,
            ptBox,
            status,
            actions
        );

        root.parentProperty().addListener((obs, oldParent, newParent) -> {
            if (oldParent != null && newParent == null) {
                persistDraftIfChanged();
            }
        });

        load(position);
    }

    public Parent root() {
        return root;
    }

    private void load(WordReviewPosition nextPosition) {
        position = nextPosition;
        EditorialCue cue = currentCue();
        EditorialWord word = currentWord();

        loadingFields = true;
        cueContext.setText(cue.approvedEn() + System.lineSeparator() + cue.pt());
        originalReference.setText(
            word.originalEn().isBlank()
                ? "Nova unidade da revisão editorial"
                : word.originalEn()
        );
        english.setText(word.approvedEn());
        pt.setText(word.pt());
        loadingFields = false;

        try {
            service.saveCursor(material, position);
            progressAction.accept(material, position);
            status.setText("");
        } catch (Exception exception) {
            status.setText("Não foi possível salvar o ponto atual.");
        }
        updateLabels();
    }

    private void navigate(Optional<WordReviewPosition> target) {
        if (target.isEmpty()) return;
        if (!persistDraftIfChanged()) return;
        load(target.get());
    }

    private void navigateToNextPending() {
        if (!persistDraftIfChanged()) return;
        Optional<WordReviewPosition> pending = service.nextPending(material, position);
        if (pending.isPresent()) {
            load(pending.get());
        } else {
            status.setText("Todas as unidades estão aprovadas.");
            updateLabels();
        }
    }

    private void saveDraft() {
        try {
            material = service.saveDraft(
                material,
                position,
                english.getText(),
                pt.getText()
            );
            status.setText("Alterações salvas. Esta unidade ainda precisa ser aprovada.");
            progressAction.accept(material, position);
            updateLabels();
        } catch (Exception exception) {
            status.setText(productMessage(exception));
        }
    }

    private void approveWord() {
        try {
            material = service.approve(
                material,
                position,
                english.getText(),
                pt.getText()
            );
            status.setText("Unidade aprovada.");
            progressAction.accept(material, position);
            updateLabels();

            Optional<WordReviewPosition> pending = service.nextPending(material, position);
            if (pending.isPresent()) {
                load(pending.get());
            } else {
                status.setText("Word by Word concluído.");
                updateLabels();
            }
        } catch (Exception exception) {
            status.setText(productMessage(exception));
        }
    }

    private boolean persistDraftIfChanged() {
        EditorialWord word = currentWord();
        String nextEn = normalized(english.getText());
        String nextPt = normalized(pt.getText());

        if (nextEn.equals(word.approvedEn()) && nextPt.equals(word.pt())) {
            return true;
        }

        try {
            material = service.saveDraft(material, position, nextEn, nextPt);
            progressAction.accept(material, position);
            return true;
        } catch (Exception exception) {
            status.setText(productMessage(exception));
            return false;
        }
    }

    private void markDirtyIfNeeded() {
        if (loadingFields) return;
        EditorialWord word = currentWord();
        boolean changed = !normalized(english.getText()).equals(word.approvedEn())
            || !normalized(pt.getText()).equals(word.pt());

        if (changed) {
            reviewState.setText("Alterada · pendente");
            reviewState.getStyleClass().removeAll("review-approved", "review-pending");
            reviewState.getStyleClass().add("review-pending");
        } else {
            updateReviewState(word);
        }
    }

    private void updateLabels() {
        long approved = service.approvedCount(material);
        int total = service.totalCount(material);
        EditorialCue cue = currentCue();
        long cueApproved = cue.words().stream()
            .filter(word -> word.reviewStatus() == EditorialReviewStatus.APPROVED)
            .count();

        progressLabel.setText(
            "Unidade %d · %d de %d aprovadas".formatted(
                absolutePosition(),
                approved,
                total
            )
        );
        cueProgressLabel.setText(
            "Cue %d de %d · %d de %d unidades aprovadas".formatted(
                cue.order(),
                material.cues().size(),
                cueApproved,
                cue.words().size()
            )
        );
        updateReviewState(currentWord());
        previous.setDisable(service.previous(material, position).isEmpty());
        next.setDisable(service.next(material, position).isEmpty());
        nextPending.setDisable(approved == total);
    }

    private void updateReviewState(EditorialWord word) {
        boolean approved = word.reviewStatus() == EditorialReviewStatus.APPROVED;
        reviewState.setText(approved ? "Aprovada" : "Pendente");
        reviewState.getStyleClass().removeAll("review-approved", "review-pending");
        reviewState.getStyleClass().add(approved ? "review-approved" : "review-pending");
    }

    private int absolutePosition() {
        int count = 0;
        for (EditorialCue cue : material.cues()) {
            for (EditorialWord word : cue.words()) {
                count++;
                if (cue.order() == position.cueOrder() && word.index() == position.wordIndex()) {
                    return count;
                }
            }
        }
        return 1;
    }

    private EditorialCue currentCue() {
        return material.cues().get(position.cueOrder() - 1);
    }

    private EditorialWord currentWord() {
        return currentCue().words().get(position.wordIndex() - 1);
    }

    private static VBox field(String labelText, TextArea area) {
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
        area.setPrefRowCount(2);
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

    private static String productMessage(Exception exception) {
        String message = exception.getMessage();
        if (message != null && message.contains("volte para a revisão da cue")) {
            return "Para trocar a palavra em English, volte para a revisão da cue.";
        }
        return "Revise English e Português antes de salvar.";
    }
}
