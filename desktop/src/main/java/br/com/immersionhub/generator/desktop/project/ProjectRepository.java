package br.com.immersionhub.generator.desktop.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class ProjectRepository {
    private static final String FILE_NAME = "project.json";

    private final Path root;
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public ProjectRepository(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    public void save(ProjectState project) throws Exception {
        Path dir = root.resolve(project.projectId());
        Files.createDirectories(dir);

        Path temporary = dir.resolve(FILE_NAME + ".tmp");
        Path target = dir.resolve(FILE_NAME);
        mapper.writeValue(temporary.toFile(), Snapshot.from(project));

        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public ProjectListing list() {
        if (!Files.isDirectory(root)) return new ProjectListing(List.of(), 0);

        List<ProjectState> projects = new ArrayList<>();
        int unreadable = 0;

        try (var entries = Files.list(root)) {
            for (Path dir : entries.filter(Files::isDirectory).toList()) {
                Path json = dir.resolve(FILE_NAME);
                if (!Files.isRegularFile(json)) continue;

                try {
                    Snapshot snapshot = mapper.readValue(json.toFile(), Snapshot.class);
                    projects.add(snapshot.toProjectState());
                } catch (Exception ignored) {
                    unreadable++;
                }
            }
        } catch (Exception ignored) {
            unreadable++;
        }

        projects.sort(Comparator.comparing(ProjectState::updatedAt).reversed());
        return new ProjectListing(projects, unreadable);
    }

    public Path projectDirectory(String projectId) {
        return root.resolve(projectId).toAbsolutePath().normalize();
    }

    public static final class Snapshot {
        public Integer schemaVersion;
        public String projectId;
        public String title;
        public String sourceId;
        public String canonicalUrl;
        public String sourcePath;
        public String sourceTitle;
        public long sourceDurationMs;
        public String sourceFetchedAt;
        public Long cutStartMs;
        public Long cutEndMs;
        public String cutPath;
        public String cutCreatedAt;
        public String currentStage;
        public List<String> completedStages;
        public String preparedMaterialId;
        public String alignedMaterialId;
        public String translationMaterialId;
        public String translationSource;
        public String createdAt;
        public String updatedAt;

        public Snapshot() {}

        static Snapshot from(ProjectState project) {
            Snapshot snapshot = new Snapshot();
            snapshot.schemaVersion = ProjectState.CURRENT_SCHEMA_VERSION;
            snapshot.projectId = project.projectId();
            snapshot.title = project.title();
            snapshot.sourceId = project.sourceId();
            snapshot.canonicalUrl = project.canonicalUrl();
            snapshot.sourcePath = project.sourcePath();
            snapshot.sourceTitle = project.sourceTitle();
            snapshot.sourceDurationMs = project.sourceDurationMs();
            snapshot.sourceFetchedAt = project.sourceFetchedAt();
            snapshot.cutStartMs = project.cutStartMs();
            snapshot.cutEndMs = project.cutEndMs();
            snapshot.cutPath = project.cutPath();
            snapshot.cutCreatedAt = project.cutCreatedAt();
            snapshot.currentStage = project.currentStage().name();
            snapshot.completedStages = project.completedStages().stream().map(Enum::name).sorted().toList();
            snapshot.preparedMaterialId = project.preparedMaterialId();
            snapshot.alignedMaterialId = project.alignedMaterialId();
            snapshot.translationMaterialId = project.translationMaterialId();
            snapshot.translationSource = project.translationSource();
            snapshot.createdAt = project.createdAt().toString();
            snapshot.updatedAt = project.updatedAt().toString();
            return snapshot;
        }

        ProjectState toProjectState() {
            int version = schemaVersion == null || schemaVersion <= 0
                ? ProjectState.CURRENT_SCHEMA_VERSION
                : schemaVersion;
            if (version > ProjectState.CURRENT_SCHEMA_VERSION) {
                throw new IllegalArgumentException("Versão de projeto ainda não suportada.");
            }

            Set<ProjectStage> completed = EnumSet.noneOf(ProjectStage.class);
            if (completedStages != null) {
                for (String value : completedStages) completed.add(ProjectStage.valueOf(value));
            }

            ProjectStage migratedStage = ProjectStage.valueOf(currentStage);
            if (version < 2
                && migratedStage == ProjectStage.PREPARATION
                && completed.contains(ProjectStage.PREPARATION)) {
                migratedStage = ProjectStage.TRANSLATION;
            }
            if (version < 3 && completed.contains(ProjectStage.TRANSLATION)) {
                migratedStage = ProjectStage.EDITORIAL_REVIEW;
            }

            return new ProjectState(
                ProjectState.CURRENT_SCHEMA_VERSION,
                projectId,
                title,
                sourceId,
                canonicalUrl,
                sourcePath,
                sourceTitle,
                sourceDurationMs,
                sourceFetchedAt,
                cutStartMs,
                cutEndMs,
                cutPath,
                cutCreatedAt,
                migratedStage,
                completed,
                preparedMaterialId,
                alignedMaterialId,
                translationMaterialId,
                translationSource,
                Instant.parse(createdAt),
                Instant.parse(updatedAt)
            );
        }
    }
}
