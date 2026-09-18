package br.com.immersionhub.generator.desktop.ui;

import br.com.immersionhub.generator.desktop.infrastructure.AppDirectories;
import br.com.immersionhub.generator.desktop.model.MediaCut;
import br.com.immersionhub.generator.desktop.model.SourceMedia;
import br.com.immersionhub.generator.desktop.timing.Boundary;
import br.com.immersionhub.generator.desktop.timing.MediaCutRepository;
import br.com.immersionhub.generator.desktop.timing.MediaProcessor;
import br.com.immersionhub.generator.desktop.timing.Timecode;
import br.com.immersionhub.generator.desktop.timing.TimingDraftRepository;
import br.com.immersionhub.generator.desktop.timing.TimingFeedback;
import br.com.immersionhub.generator.desktop.timing.TimingHistory;
import br.com.immersionhub.generator.desktop.timing.TimingRange;
import br.com.immersionhub.generator.desktop.timing.TimingSelection;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class WaveView {
    private final VBox root = new VBox(14);
    private final SourceMedia sourceMedia;
    private final MediaProcessor mediaProcessor;
    private final MediaCutRepository cutRepository;
    private final TimingDraftRepository draftRepository;
    private final Consumer<MediaCut> cutSavedAction;
    private final TimingSelection selection;
    private final TimingHistory history;
    private final WaveformPane waveformPane = new WaveformPane();

    private MediaPlayer player;
    private Boundary activeBoundary = Boundary.IN;
    private boolean selectionPlayback;
    private TextField inField;
    private TextField outField;
    private Label durationLabel;
    private final Label playheadLabel = new Label(Timecode.format(0));
    private Label status;
    private Label saveStateLabel;
    private Button saveButton;
    private Button undoButton;
    private Button redoButton;

    public WaveView(
        SourceMedia sourceMedia,
        MediaProcessor mediaProcessor,
        MediaCutRepository cutRepository,
        TimingDraftRepository draftRepository,
        MediaCut existingCut,
        Consumer<MediaCut> cutSavedAction,
        Runnable backAction
    ) {
        this.sourceMedia = sourceMedia;
        this.mediaProcessor = mediaProcessor;
        this.cutRepository = cutRepository;
        this.draftRepository = draftRepository;
        this.cutSavedAction = cutSavedAction;
        this.selection = new TimingSelection(sourceMedia.durationMs());

        Optional<TimingRange> restoredDraft = draftRepository.load(
            sourceMedia.sourceId(),
            sourceMedia.durationMs()
        );

        if (restoredDraft.isPresent()) {
            TimingRange range = restoredDraft.get();
            selection.setRange(range.startMs(), range.endMs());
        } else if (existingCut != null && existingCut.sourceId().equals(sourceMedia.sourceId())) {
            selection.setRange(existingCut.startMs(), existingCut.endMs());
        }

        this.history = new TimingHistory(selection.startMs(), selection.endMs());

        root.setPadding(new Insets(28, 34, 28, 34));
        root.getStyleClass().add("content-page");
        root.setFocusTraversable(true);

        Label eyebrow = new Label("ETAPA 02 · WAVE");
        eyebrow.getStyleClass().add("eyebrow");

        Label title = new Label("Wave Editor");
        title.getStyleClass().add("page-title");

        Label source = new Label(
            "Fonte · " + sourceMedia.title() + " · " + Timecode.format(sourceMedia.durationMs())
        );
        source.getStyleClass().add("path-chip");

        status = new Label("Preparando waveform…");
        status.getStyleClass().add("page-copy");
        status.setWrapText(true);

        Node playerNode = buildPlayer();
        Node timingControls = buildTimingControls(backAction);

        if (restoredDraft.isPresent()) {
            saveStateLabel.setText("Timing recuperado do salvamento automático.");
        } else if (existingCut != null && existingCut.sourceId().equals(sourceMedia.sourceId())) {
            saveStateLabel.setText("Timing carregado do último recorte salvo.");
        } else {
            saveStateLabel.setText("Sem alterações de timing.");
        }

        waveformPane.setWaveform(List.of(), sourceMedia.durationMs());
        waveformPane.setRange(selection.startMs(), selection.endMs());
        waveformPane.onRangeChanged((start, end) -> {
            selection.setRange(start, end);
            refreshRange();
        });
        waveformPane.onRangeCommitted(this::commitTimingEdit);
        waveformPane.onSeek(this::seek);
        waveformPane.onBoundarySelected(boundary -> activeBoundary = boundary);

        VBox waveColumn = new VBox(10, playheadHeader(), waveformPane, timingControls);
        HBox.setHgrow(waveColumn, Priority.ALWAYS);
        waveColumn.getStyleClass().add("wave-column");

        VBox playerColumn = new VBox(8, playerNode);
        playerColumn.getStyleClass().add("video-column");

        HBox workspace = new HBox(18, playerColumn, waveColumn);
        workspace.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(waveColumn, Priority.ALWAYS);
        workspace.getStyleClass().add("wave-workspace-java");

        root.getChildren().addAll(eyebrow, title, source, status, workspace);
        bindShortcuts();
        loadWaveform();

        root.sceneProperty().addListener((obs, previous, current) -> {
            if (previous != null && current == null) dispose();
        });

        Platform.runLater(root::requestFocus);
    }

    public Parent root() {
        return root;
    }

    private Node buildPlayer() {
        try {
            Media media = new Media(sourceMedia.localPath().toUri().toString());
            player = new MediaPlayer(media);
            MediaView view = new MediaView(player);
            view.setPreserveRatio(true);
            view.setFitWidth(560);
            view.setFitHeight(315);

            player.currentTimeProperty().addListener((obs, previous, current) -> {
                long currentMs = Math.max(0, Math.round(current.toMillis()));
                playheadLabel.setText(Timecode.format(currentMs));
                waveformPane.setPlayheadMs(currentMs);

                if (selectionPlayback && currentMs >= selection.endMs()) {
                    selectionPlayback = false;
                    player.pause();
                    player.seek(Duration.millis(selection.endMs()));
                }
            });

            player.setOnError(() -> status.setText(TimingFeedback.playbackFailure()));

            StackPane shell = new StackPane(view);
            shell.getStyleClass().add("video-shell-java");
            return shell;
        } catch (Exception exception) {
            Label fallback = new Label("A prévia de vídeo não pôde ser aberta.");
            fallback.getStyleClass().add("wave-placeholder");
            status.setText(TimingFeedback.playbackFailure());
            return fallback;
        }
    }

    private Node playheadHeader() {
        playheadLabel.getStyleClass().add("playhead-label");

        Label label = new Label("WAVEFORM · ARRASTE IN/OUT · SCROLL PARA PAN");
        label.getStyleClass().add("eyebrow");

        HBox row = new HBox(12, label, playheadLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(label, Priority.ALWAYS);
        return row;
    }

    private Node buildTimingControls(Runnable backAction) {
        inField = timeField(Timecode.format(selection.startMs()));
        outField = timeField(Timecode.format(selection.endMs()));
        durationLabel = new Label();
        durationLabel.getStyleClass().add("duration-label");

        bindTimeField(inField, Boundary.IN);
        bindTimeField(outField, Boundary.OUT);

        VBox inBox = fieldBox("IN", inField);
        VBox outBox = fieldBox("OUT", outField);
        VBox durationBox = fieldBox("DURAÇÃO", durationLabel);

        HBox timeRow = new HBox(10, inBox, outBox, durationBox);
        timeRow.setAlignment(Pos.CENTER_LEFT);

        Button markIn = secondary("Marcar IN");
        markIn.setOnAction(event -> mark(Boundary.IN));

        Button markOut = secondary("Marcar OUT");
        markOut.setOnAction(event -> mark(Boundary.OUT));

        Button playSelection = secondary("▶ Seleção");
        playSelection.setOnAction(event -> toggleSelectionPlayback());

        Button playAll = secondary("▶ Fonte inteira");
        playAll.setOnAction(event -> toggleFullPlayback());

        undoButton = secondary("Desfazer");
        undoButton.setOnAction(event -> undoTiming());

        redoButton = secondary("Refazer");
        redoButton.setOnAction(event -> redoTiming());

        Label zoomLabel = new Label("Zoom 1.0×");
        zoomLabel.getStyleClass().add("page-copy");

        Slider zoom = new Slider(1, 8, 1);
        zoom.setPrefWidth(150);

        ToggleButton superZoom = new ToggleButton("Super Zoom");
        superZoom.getStyleClass().add("secondary-button");
        superZoom.setOnAction(event -> {
            if (superZoom.isSelected()) {
                zoom.setMax(64);
                zoomLabel.setText("Super Zoom %.1f×".formatted(zoom.getValue()));
            } else {
                zoom.setMax(8);
                if (zoom.getValue() > 8) zoom.setValue(8);
                zoomLabel.setText("Zoom %.1f×".formatted(zoom.getValue()));
            }
        });

        zoom.valueProperty().addListener((obs, oldValue, newValue) -> {
            double value = newValue.doubleValue();
            waveformPane.setZoom(value);
            zoomLabel.setText(
                (superZoom.isSelected() ? "Super Zoom " : "Zoom ") + "%.1f×".formatted(value)
            );
        });

        HBox transport = new HBox(8, markIn, markOut, playSelection, playAll, undoButton, redoButton);
        transport.setAlignment(Pos.CENTER_LEFT);

        HBox zoomRow = new HBox(8, zoom, zoomLabel, superZoom);
        zoomRow.setAlignment(Pos.CENTER_LEFT);

        Label shortcuts = new Label(
            "Space seleção · Shift+Space fonte inteira · A IN · S OUT · " +
            "←/→ 10ms · Shift+←/→ 100ms · Alt+←/→ 1ms · " +
            "Ctrl+Z desfazer · Ctrl+Y refazer · Ctrl+S salvar recorte"
        );
        shortcuts.getStyleClass().add("shortcut-label");
        shortcuts.setWrapText(true);

        saveStateLabel = new Label();
        saveStateLabel.getStyleClass().add("save-state-label");
        saveStateLabel.setWrapText(true);

        Button back = secondary("← Source");
        back.setOnAction(event -> {
            if (autosaveSelection()) backAction.run();
        });

        saveButton = new Button("Salvar recorte");
        saveButton.getStyleClass().add("primary-button");
        saveButton.setOnAction(event -> saveCut());

        HBox actions = new HBox(10, back, saveButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox box = new VBox(10, timeRow, transport, zoomRow, shortcuts, saveStateLabel, actions);
        refreshRange();
        refreshHistoryButtons();
        return box;
    }

    private VBox fieldBox(String labelText, Node control) {
        Label label = new Label(labelText);
        label.getStyleClass().add("eyebrow");
        return new VBox(5, label, control);
    }

    private TextField timeField(String value) {
        TextField field = new TextField(value);
        field.getStyleClass().add("time-field");
        field.setPrefColumnCount(13);
        return field;
    }

    private Button secondary(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("secondary-button");
        return button;
    }

    private void bindTimeField(TextField field, Boundary boundary) {
        field.setOnAction(event -> applyTimeField(field, boundary));
        field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (wasFocused && !isFocused) applyTimeField(field, boundary);
        });
        field.setOnMousePressed(event -> activeBoundary = boundary);
    }

    private void applyTimeField(TextField field, Boundary boundary) {
        try {
            long value = Timecode.parse(field.getText());
            if (boundary == Boundary.IN) selection.setStartMs(value);
            else selection.setEndMs(value);
            activeBoundary = boundary;
            commitTimingEdit();
        } catch (Exception exception) {
            status.setText(messageOf(exception));
            refreshRange();
        }
    }

    private void mark(Boundary boundary) {
        long playhead = player == null ? 0 : Math.round(player.getCurrentTime().toMillis());
        selection.mark(boundary, playhead);
        activeBoundary = boundary;
        commitTimingEdit();
    }

    private void nudge(long deltaMs) {
        selection.nudge(activeBoundary, deltaMs);
        commitTimingEdit();
    }

    private void commitTimingEdit() {
        history.record(selection.startMs(), selection.endMs());
        refreshRange();
        refreshHistoryButtons();
        autosaveSelection();
    }

    private void undoTiming() {
        history.undo().ifPresent(this::applyHistoryRange);
    }

    private void redoTiming() {
        history.redo().ifPresent(this::applyHistoryRange);
    }

    private void applyHistoryRange(TimingRange range) {
        selection.setRange(range.startMs(), range.endMs());
        refreshRange();
        refreshHistoryButtons();
        autosaveSelection();
    }

    private boolean autosaveSelection() {
        try {
            draftRepository.save(sourceMedia.sourceId(), selection.startMs(), selection.endMs());
            if (saveStateLabel != null) {
                saveStateLabel.setText("Timing salvo automaticamente.");
            }
            return true;
        } catch (Exception exception) {
            if (saveStateLabel != null) {
                saveStateLabel.setText("Não foi possível salvar o timing automaticamente.");
            }
            status.setText("Não foi possível proteger as alterações de timing. Tente novamente.");
            return false;
        }
    }

    private void refreshHistoryButtons() {
        if (undoButton != null) undoButton.setDisable(!history.canUndo());
        if (redoButton != null) redoButton.setDisable(!history.canRedo());
    }

    private void refreshRange() {
        if (inField == null || outField == null || durationLabel == null) return;
        inField.setText(Timecode.format(selection.startMs()));
        outField.setText(Timecode.format(selection.endMs()));
        durationLabel.setText(Timecode.format(selection.durationMs()));
        waveformPane.setRange(selection.startMs(), selection.endMs());
    }

    private void seek(long ms) {
        if (player != null) player.seek(Duration.millis(ms));
    }

    private void toggleSelectionPlayback() {
        if (player == null) return;
        if (player.getStatus() == MediaPlayer.Status.PLAYING) {
            player.pause();
            selectionPlayback = false;
            return;
        }
        selectionPlayback = true;
        player.seek(Duration.millis(selection.startMs()));
        player.play();
    }

    private void toggleFullPlayback() {
        if (player == null) return;
        if (player.getStatus() == MediaPlayer.Status.PLAYING) {
            player.pause();
            selectionPlayback = false;
            return;
        }
        selectionPlayback = false;
        if (player.getCurrentTime().toMillis() >= sourceMedia.durationMs() - 50) {
            player.seek(Duration.ZERO);
        }
        player.play();
    }

    private void bindShortcuts() {
        root.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            KeyCode code = event.getCode();
            boolean command = event.isControlDown() || event.isMetaDown();

            if (command && code == KeyCode.S) {
                saveCut();
                event.consume();
                return;
            }
            if (command && code == KeyCode.Z) {
                if (event.isShiftDown()) redoTiming();
                else undoTiming();
                event.consume();
                return;
            }
            if (command && code == KeyCode.Y) {
                redoTiming();
                event.consume();
                return;
            }

            if (event.getTarget() instanceof TextInputControl) return;

            if (code == KeyCode.SPACE) {
                if (event.isShiftDown()) toggleFullPlayback();
                else toggleSelectionPlayback();
                event.consume();
                return;
            }
            if (code == KeyCode.A) {
                mark(Boundary.IN);
                event.consume();
                return;
            }
            if (code == KeyCode.S) {
                mark(Boundary.OUT);
                event.consume();
                return;
            }
            if (code == KeyCode.LEFT || code == KeyCode.RIGHT) {
                long step = event.isAltDown() ? 1 : (event.isShiftDown() ? 100 : 10);
                nudge(code == KeyCode.LEFT ? -step : step);
                event.consume();
            }
        });
    }

    private void loadWaveform() {
        Task<List<Double>> task = new Task<>() {
            @Override
            protected List<Double> call() throws Exception {
                return mediaProcessor.waveform(sourceMedia, 2400);
            }
        };

        task.setOnSucceeded(event -> {
            waveformPane.setWaveform(task.getValue(), sourceMedia.durationMs());
            waveformPane.setRange(selection.startMs(), selection.endMs());
            status.setText("Waveform pronta. Ajuste IN e OUT.");
        });

        task.setOnFailed(event -> status.setText(TimingFeedback.waveformFailure()));

        Thread worker = new Thread(task, "waveform-reader");
        worker.setDaemon(true);
        worker.start();
    }

    private void saveCut() {
        if (saveButton == null || saveButton.isDisabled()) return;

        saveButton.setDisable(true);
        status.setText("Salvando recorte…");
        autosaveSelection();

        Task<MediaCut> task = new Task<>() {
            @Override
            protected MediaCut call() throws Exception {
                MediaCut cut = mediaProcessor.cut(
                    sourceMedia,
                    selection.startMs(),
                    selection.endMs(),
                    AppDirectories.workspaceDir().resolve("timing")
                );
                cutRepository.save(cut);
                return cut;
            }
        };

        task.setOnSucceeded(event -> {
            MediaCut cut = task.getValue();
            cutSavedAction.accept(cut);
            status.setText("Recorte salvo · " + Timecode.format(cut.durationMs()));
            saveStateLabel.setText("Timing salvo automaticamente · recorte gerado.");
            saveButton.setDisable(false);
        });

        task.setOnFailed(event -> {
            status.setText(TimingFeedback.cutFailure());
            saveButton.setDisable(false);
        });

        Thread worker = new Thread(task, "media-cut");
        worker.setDaemon(true);
        worker.start();
    }

    private void dispose() {
        autosaveSelection();
        if (player != null) {
            player.stop();
            player.dispose();
            player = null;
        }
    }

    private static String messageOf(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()) {
            return "Falha inesperada.";
        }
        return throwable.getMessage();
    }
}
