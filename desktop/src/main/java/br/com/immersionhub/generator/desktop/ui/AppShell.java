package br.com.immersionhub.generator.desktop.ui;

import br.com.immersionhub.generator.desktop.navigation.ScreenId;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class AppShell {
    private final BorderPane root = new BorderPane();
    private final Label sectionLabel = new Label("HOME");
    private final Button homeButton = navButton("Home");
    private final Button sourceButton = navButton("Source");
    private final Button waveButton = navButton("Wave");
    private final Button preparationButton = navButton("Prepare");
    private final Button translationButton = navButton("Translation");
    private final Button reviewButton = navButton("Review");
    private final Button wordReviewButton = navButton("Words");
    private final Button configButton = new Button("Config");

    public AppShell() {
        root.getStyleClass().add("app-root");
        root.setTop(buildTopBar());
        root.setLeft(buildSidebar());
        setWaveEnabled(false);
        setPreparationEnabled(false);
        setTranslationEnabled(false);
        setReviewEnabled(false);
        setWordReviewEnabled(false);
    }

    public Parent root() { return root; }

    public void setHomeAction(Runnable action) {
        homeButton.setOnAction(event -> action.run());
    }

    public void setSourceAction(Runnable action) {
        sourceButton.setOnAction(event -> action.run());
    }

    public void setWaveAction(Runnable action) {
        waveButton.setOnAction(event -> action.run());
    }

    public void setPreparationAction(Runnable action) {
        preparationButton.setOnAction(event -> action.run());
    }

    public void setTranslationAction(Runnable action) {
        translationButton.setOnAction(event -> action.run());
    }

    public void setReviewAction(Runnable action) {
        reviewButton.setOnAction(event -> action.run());
    }

    public void setWordReviewAction(Runnable action) {
        wordReviewButton.setOnAction(event -> action.run());
    }

    public void setConfigAction(Runnable action) {
        configButton.setOnAction(event -> action.run());
    }

    public void setWaveEnabled(boolean enabled) {
        waveButton.setDisable(!enabled);
    }

    public void setPreparationEnabled(boolean enabled) {
        preparationButton.setDisable(!enabled);
    }

    public void setTranslationEnabled(boolean enabled) {
        translationButton.setDisable(!enabled);
    }

    public void setReviewEnabled(boolean enabled) {
        reviewButton.setDisable(!enabled);
    }

    public void setWordReviewEnabled(boolean enabled) {
        wordReviewButton.setDisable(!enabled);
    }

    public void show(Node content, ScreenId screen) {
        root.setCenter(content);
        sectionLabel.setText(
            screen == ScreenId.EDITORIAL_REVIEW ? "REVIEW"
                : screen == ScreenId.WORD_REVIEW ? "WORDS"
                : screen.name()
        );
        setActive(homeButton, screen == ScreenId.HOME);
        setActive(sourceButton, screen == ScreenId.SOURCE);
        setActive(waveButton, screen == ScreenId.WAVE);
        setActive(preparationButton, screen == ScreenId.PREPARATION);
        setActive(translationButton, screen == ScreenId.TRANSLATION);
        setActive(reviewButton, screen == ScreenId.EDITORIAL_REVIEW);
        setActive(wordReviewButton, screen == ScreenId.WORD_REVIEW);
    }

    private void setActive(Button button, boolean active) {
        button.getStyleClass().remove("nav-active");
        if (active) button.getStyleClass().add("nav-active");
    }

    private Node buildTopBar() {
        Label product = new Label("ImmersionHub Generator");
        product.getStyleClass().add("product-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        configButton.getStyleClass().add("secondary-button");
        sectionLabel.getStyleClass().add("section-label");

        HBox bar = new HBox(12, product, spacer, configButton, sectionLabel);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(16, 22, 16, 22));
        bar.getStyleClass().add("top-bar");
        return bar;
    }

    private Node buildSidebar() {
        Label flow = new Label("FLOW");
        flow.getStyleClass().add("sidebar-kicker");

        homeButton.setMaxWidth(Double.MAX_VALUE);
        sourceButton.setMaxWidth(Double.MAX_VALUE);
        waveButton.setMaxWidth(Double.MAX_VALUE);
        preparationButton.setMaxWidth(Double.MAX_VALUE);
        translationButton.setMaxWidth(Double.MAX_VALUE);
        reviewButton.setMaxWidth(Double.MAX_VALUE);
        wordReviewButton.setMaxWidth(Double.MAX_VALUE);

        VBox sidebar = new VBox(
            8,
            flow,
            homeButton,
            sourceButton,
            waveButton,
            preparationButton,
            translationButton,
            reviewButton,
            wordReviewButton
        );
        sidebar.setPadding(new Insets(22, 14, 22, 14));
        sidebar.setPrefWidth(200);
        sidebar.getStyleClass().add("sidebar");
        return sidebar;
    }

    private Button navButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("nav-button");
        return button;
    }
}
