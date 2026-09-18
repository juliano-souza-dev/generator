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
    private final Label sectionLabel = new Label("SOURCE");
    private final Button sourceButton = navButton("Source");
    private final Button waveButton = navButton("Wave");
    private final Button preparationButton = navButton("Prepare");

    public AppShell() {
        root.getStyleClass().add("app-root");
        root.setTop(buildTopBar());
        root.setLeft(buildSidebar());
        setWaveEnabled(false);
        setPreparationEnabled(false);
    }

    public Parent root() { return root; }

    public void setSourceAction(Runnable action) {
        sourceButton.setOnAction(event -> action.run());
    }

    public void setWaveAction(Runnable action) {
        waveButton.setOnAction(event -> action.run());
    }

    public void setPreparationAction(Runnable action) {
        preparationButton.setOnAction(event -> action.run());
    }

    public void setWaveEnabled(boolean enabled) {
        waveButton.setDisable(!enabled);
    }

    public void setPreparationEnabled(boolean enabled) {
        preparationButton.setDisable(!enabled);
    }

    public void show(Node content, ScreenId screen) {
        root.setCenter(content);
        sectionLabel.setText(screen.name());
        setActive(sourceButton, screen == ScreenId.SOURCE);
        setActive(waveButton, screen == ScreenId.WAVE);
        setActive(preparationButton, screen == ScreenId.PREPARATION);
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

        sectionLabel.getStyleClass().add("section-label");
        HBox bar = new HBox(16, product, spacer, sectionLabel);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(16, 22, 16, 22));
        bar.getStyleClass().add("top-bar");
        return bar;
    }

    private Node buildSidebar() {
        Label flow = new Label("FLOW");
        flow.getStyleClass().add("sidebar-kicker");

        sourceButton.setMaxWidth(Double.MAX_VALUE);
        waveButton.setMaxWidth(Double.MAX_VALUE);
        preparationButton.setMaxWidth(Double.MAX_VALUE);

        VBox sidebar = new VBox(8, flow, sourceButton, waveButton, preparationButton);
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
