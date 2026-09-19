package br.com.immersionhub.generator.desktop.ui;

import br.com.immersionhub.generator.desktop.project.ProjectListing;
import br.com.immersionhub.generator.desktop.project.ProjectState;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public final class HomeView {
    private static final DateTimeFormatter DATE =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final VBox root = new VBox(18);

    public HomeView(
        ProjectListing listing,
        Consumer<ProjectState> continueAction,
        Runnable newProjectAction
    ) {
        root.setPadding(new Insets(42));
        root.getStyleClass().add("content-page");

        Label eyebrow = new Label("PROJETOS");
        eyebrow.getStyleClass().add("eyebrow");

        Label title = new Label("Continuar de onde parou");
        title.getStyleClass().add("page-title");

        Label copy = new Label(
            "Seus projetos em andamento ficam salvos aqui. Escolha um para continuar ou comece um novo."
        );
        copy.getStyleClass().add("page-copy");
        copy.setWrapText(true);

        Button newProject = new Button("Novo projeto");
        newProject.getStyleClass().add("primary-button");
        newProject.setOnAction(event -> newProjectAction.run());

        VBox projects = new VBox(10);
        if (listing.projects().isEmpty()) {
            Label empty = new Label("Nenhum projeto em andamento.");
            empty.getStyleClass().add("page-copy");
            projects.getChildren().add(empty);
        } else {
            for (ProjectState project : listing.projects()) {
                projects.getChildren().add(projectRow(project, continueAction));
            }
        }

        if (listing.unreadableCount() > 0) {
            Label warning = new Label(
                listing.unreadableCount() == 1
                    ? "Um projeto salvo não pôde ser carregado."
                    : listing.unreadableCount() + " projetos salvos não puderam ser carregados."
            );
            warning.getStyleClass().add("page-copy");
            projects.getChildren().add(warning);
        }

        root.getChildren().addAll(eyebrow, title, copy, newProject, projects);
    }

    public Parent root() {
        return root;
    }

    private static HBox projectRow(ProjectState project, Consumer<ProjectState> continueAction) {
        Label name = new Label(project.title());
        name.getStyleClass().add("project-card-title");

        String stageText = project.preparationCompleted()
            ? "Preparação concluída"
            : "Etapa atual · " + project.minimumResumeStage().userLabel();

        Label stage = new Label(stageText);
        stage.getStyleClass().add("page-copy");

        Label updated = new Label("Última atividade · " + DATE.format(project.updatedAt()));
        updated.getStyleClass().add("project-card-meta");

        VBox text = new VBox(4, name, stage, updated);
        String recovery = project.recoveryMessage();
        if (!recovery.isBlank()) {
            Label recoveryLabel = new Label(recovery);
            recoveryLabel.getStyleClass().add("project-card-warning");
            text.getChildren().add(recoveryLabel);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button open = new Button("Continuar");
        open.getStyleClass().add("secondary-button");
        open.setOnAction(event -> continueAction.accept(project));

        HBox row = new HBox(14, text, spacer, open);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14));
        row.getStyleClass().add("project-card");
        return row;
    }
}
