package br.com.immersionhub.generator.desktop.ui;

import br.com.immersionhub.generator.desktop.editorial.EditorialCue;
import br.com.immersionhub.generator.desktop.editorial.EditorialMaterial;
import br.com.immersionhub.generator.desktop.editorial.EditorialReviewStatus;
import br.com.immersionhub.generator.desktop.editorial.WordReviewMutation;
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
    private final Label groupState = new Label();
    private final Label status = new Label();

    private final TextArea cueContext = readOnlyArea();
    private final TextArea originalReference = readOnlyArea();
    private final TextArea english = editableArea();
    private final TextArea pt = editableArea();

    private final Button previous = secondary("← Anterior");
    private final Button next = secondary("Próxima →");
    private final Button nextPending = secondary("Próxima pendente");
    private final Button groupPrevious = secondary("Agrupar com anterior");
    private final Button groupNext = secondary("Agrupar com próxima");
    private final Button ungroup = secondary("Desagrupar");
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
            "Revise palavras ou unidades semânticas. Você pode agrupar unidades vizinhas sem alterar a ordem da cue."
        );
        copy.getStyleClass().add("page-copy");
        copy.setWrapText(true);

        progressLabel.getStyleClass().add("review-progress");
        cueProgressLabel.getStyleClass().add("project-card-meta");
        groupState.getStyleClass().add("project-card-meta");
        reviewState.getStyleClass().add("review-state");
        status.getStyleClass().add("page-copy");
        status.setWrapText(true);

        approve.getStyleClass().add("primary-button");

        previous.setOnAction(event -> navigate(service.previous(material, position)));
        next.setOnAction(event -> navigate(service.next(material, position)));
        nextPending.setOnAction(event -> navigateToNextPending());
        groupPrevious.setOnAction(event -> groupWithPrevious());
        groupNext.setOnAction(event -> groupWithNext());
        ungroup.setOnAction(event -> ungroup());
        save.setOnAction(event -> saveDraft());
        approve.setOnAction(event -> approveUnit());

        english.textProperty().addListener((obs, oldValue, newValue) -> markDirtyIfNeeded());
        pt.textProperty().addListener((obs, oldValue, newValue) -> markDirtyIfNeeded());

        Button back = secondary("← Revisão de cues");
        back.setOnAction(event -> {
            if (persistDraftIfChanged()) backAction.run();
        });

        HBox nav = new HBox(8, previous, next, nextPending);
        nav.setAlignment(Pos.CENTER_LEFT);

        HBox grouping = new HBox(8, groupPrevious, groupNext, ungroup);
        grouping.setAlignment(Pos.CENTER_LEFT);

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
            groupState,
            nav,
            grouping,
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

        loadingFields = true;
        cueContext.setText(cue.approvedEn() + System.lineSeparator() + cue.pt());
        originalReference.setText(service.unitOriginalReference(material, position));
        english.setText(service.unitEnglish(material, position));
        pt.setText(service.unitPt(material, position));
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

    private void approveUnit() {
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

    private void groupWithPrevious() {
        if (!persistDraftIfChanged()) return;
        try {
            applyMutation(service.groupWithPrevious(material, position));
            status.setText("Unidades agrupadas. Revise e aprove a tradução do grupo.");
        } catch (Exception exception) {
            status.setText("Não foi possível agrupar com a unidade anterior.");
        }
    }

    private void groupWithNext() {
        if (!persistDraftIfChanged()) return;
        try {
            applyMutation(service.groupWithNext(material, position));
            status.setText("Unidades agrupadas. Revise e aprove a tradução do grupo.");
        } catch (Exception exception) {
            status.setText("Não foi possível agrupar com a próxima unidade.");
        }
    }

    private void ungroup() {
        if (!persistDraftIfChanged()) return;
        try {
            applyMutation(service.ungroup(material, position));
            status.setText("Grupo desfeito. As traduções individuais conhecidas foram restauradas.");
        } catch (Exception exception) {
            status.setText("Esta unidade não pode ser desagrupada.");
        }
    }

    private void applyMutation(WordReviewMutation mutation) {
        material = mutation.material();
        position = mutation.position();
        progressAction.accept(material, position);
        load(position);
    }

    private boolean persistDraftIfChanged() {
        String nextEn = normalized(english.getText());
        String nextPt = normalized(pt.getText());

        if (nextEn.equals(service.unitEnglish(material, position))
            && nextPt.equals(service.unitPt(material, position))) {
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
        boolean changed = !normalized(english.getText()).equals(service.unitEnglish(material, position))
            || !normalized(pt.getText()).equals(service.unitPt(material, position));

        if (changed) {
            reviewState.setText("Alterada · pendente");
            reviewState.getStyleClass().removeAll("review-approved", "review-pending");
            reviewState.getStyleClass().add("review-pending");
        } else {
            updateReviewState();
        }
    }

    private void updateLabels() {
        long approved = service.approvedCount(material);
        int total = service.totalCount(material);
        EditorialCue cue = currentCue();

        progressLabel.setText(
            "Unidade %d · %d de %d aprovadas".formatted(
                service.ordinal(material, position),
                approved,
                total
            )
        );
        cueProgressLabel.setText(
            "Cue %d de %d · %d de %d unidades aprovadas".formatted(
                cue.order(),
                material.cues().size(),
                service.cueApprovedCount(material, cue.order()),
                service.cueTotalCount(material, cue.order())
            )
        );

        int size = service.unitSize(material, position);
        groupState.setText(
            size > 1
                ? "Unidade semântica · " + size + " palavras"
                : "Unidade individual"
        );

        updateReviewState();
        previous.setDisable(service.previous(material, position).isEmpty());
        next.setDisable(service.next(material, position).isEmpty());
        nextPending.setDisable(approved == total);
        groupPrevious.setDisable(!service.canGroupPrevious(material, position));
        groupNext.setDisable(!service.canGroupNext(material, position));
        ungroup.setDisable(!service.isGrouped(material, position));
    }

    private void updateReviewState() {
        boolean approved = service.unitStatus(material, position) == EditorialReviewStatus.APPROVED;
        reviewState.setText(approved ? "Aprovada" : "Pendente");
        reviewState.getStyleClass().removeAll("review-approved", "review-pending");
        reviewState.getStyleClass().add(approved ? "review-approved" : "review-pending");
    }

    private EditorialCue currentCue() {
        return material.cues().get(position.cueOrder() - 1);
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
            return "Para trocar palavras em English, volte para a revisão da cue.";
        }
        return "Revise English e Português antes de salvar.";
    }
}
