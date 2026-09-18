package br.com.immersionhub.generator.desktop;

import br.com.immersionhub.generator.desktop.infrastructure.AppDirectories;
import br.com.immersionhub.generator.desktop.navigation.NavigationController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class GeneratorDesktopApp extends Application {
    static void launchApp(String[] args) {
        Application.launch(GeneratorDesktopApp.class, args);
    }

    @Override
    public void start(Stage primaryStage) {
        AppDirectories.prepare();
        NavigationController navigation = new NavigationController();

        Scene scene = new Scene(navigation.root(), 1440, 900);
        scene.getStylesheets().add(
            GeneratorDesktopApp.class.getResource("/desktop.css").toExternalForm()
        );

        primaryStage.setTitle("ImmersionHub Generator");
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(720);
        primaryStage.setScene(scene);
        primaryStage.show();

        navigation.showSource();
    }
}
