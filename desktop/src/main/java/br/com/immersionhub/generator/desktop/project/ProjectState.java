package br.com.immersionhub.generator.desktop.project;

import br.com.immersionhub.generator.desktop.model.MediaCut;
import br.com.immersionhub.generator.desktop.model.SourceMedia;
import br.com.immersionhub.generator.desktop.preparation.AlignedMaterial;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public record ProjectState(
    int schemaVersion,
    String projectId,
    String title,
    String sourceId,
    String canonicalUrl,
    String sourcePath,
    String sourceTitle,
    long sourceDurationMs,
    String sourceFetchedAt,
    Long cutStartMs,
    Long cutEndMs,
    String cutPath,
    String cutCreatedAt,
    ProjectStage currentStage,
    Set<ProjectStage> completedStages,
    String preparedMaterialId,
    String alignedMaterialId,
    Instant createdAt,
    Instant updatedAt
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public ProjectState {
        if (schemaVersion <= 0) schemaVersion = CURRENT_SCHEMA_VERSION;
        projectId = requireText(projectId, "projectId");
        title = requireText(title, "title");
        sourceId = requireText(sourceId, "sourceId");
        canonicalUrl = requireText(canonicalUrl, "canonicalUrl");
        sourcePath = requireText(sourcePath, "sourcePath");
        sourceTitle = requireText(sourceTitle, "sourceTitle");
        sourceFetchedAt = requireText(sourceFetchedAt, "sourceFetchedAt");
        currentStage = Objects.requireNonNull(currentStage, "currentStage");
        completedStages = completedStages == null
            ? Set.of()
            : Set.copyOf(completedStages);
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        if (sourceDurationMs <= 0) throw new IllegalArgumentException("Duração da fonte inválida.");
    }

    public static ProjectState start(SourceMedia source) {
        Instant now = Instant.now();
        return new ProjectState(
            CURRENT_SCHEMA_VERSION,
            UUID.randomUUID().toString(),
            source.title(),
            source.sourceId(),
            source.canonicalUrl(),
            source.localPath().toString(),
            source.title(),
            source.durationMs(),
            source.fetchedAt().toString(),
            null,
            null,
            null,
            null,
            ProjectStage.WAVE,
            EnumSet.of(ProjectStage.SOURCE),
            null,
            null,
            now,
            now
        );
    }

    public ProjectState withCut(MediaCut cut) {
        if (!sourceId.equals(cut.sourceId())) {
            throw new IllegalArgumentException("O recorte não pertence à fonte do projeto.");
        }
        return new ProjectState(
            CURRENT_SCHEMA_VERSION,
            projectId,
            title,
            sourceId,
            canonicalUrl,
            sourcePath,
            sourceTitle,
            sourceDurationMs,
            sourceFetchedAt,
            cut.startMs(),
            cut.endMs(),
            cut.outputPath().toString(),
            cut.createdAt().toString(),
            ProjectStage.PREPARATION,
            EnumSet.of(ProjectStage.SOURCE, ProjectStage.WAVE),
            null,
            null,
            createdAt,
            Instant.now()
        );
    }

    public ProjectState withPrepared(AlignedMaterial material) {
        MediaCut cut = material.preparedMaterial().mediaCut();
        return new ProjectState(
            CURRENT_SCHEMA_VERSION,
            projectId,
            title,
            sourceId,
            canonicalUrl,
            sourcePath,
            sourceTitle,
            sourceDurationMs,
            sourceFetchedAt,
            cut.startMs(),
            cut.endMs(),
            cut.outputPath().toString(),
            cut.createdAt().toString(),
            ProjectStage.PREPARATION,
            EnumSet.of(ProjectStage.SOURCE, ProjectStage.WAVE, ProjectStage.PREPARATION),
            material.preparedMaterial().id(),
            material.id(),
            createdAt,
            Instant.now()
        );
    }

    public boolean preparationCompleted() {
        return completedStages.contains(ProjectStage.PREPARATION)
            && preparedMaterialId != null && !preparedMaterialId.isBlank()
            && alignedMaterialId != null && !alignedMaterialId.isBlank();
    }

    public Optional<SourceMedia> sourceMedia() {
        try {
            Path path = Path.of(sourcePath).toAbsolutePath().normalize();
            if (!Files.isRegularFile(path)) return Optional.empty();
            return Optional.of(new SourceMedia(
                sourceId,
                canonicalUrl,
                path,
                sourceTitle,
                sourceDurationMs,
                Instant.parse(sourceFetchedAt),
                true
            ));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public Optional<MediaCut> mediaCut() {
        if (cutStartMs == null || cutEndMs == null || cutPath == null || cutPath.isBlank() || cutCreatedAt == null) {
            return Optional.empty();
        }
        Optional<SourceMedia> source = sourceMedia();
        if (source.isEmpty()) return Optional.empty();

        try {
            Path output = Path.of(cutPath).toAbsolutePath().normalize();
            if (!Files.isRegularFile(output)) return Optional.empty();
            return Optional.of(new MediaCut(
                sourceId,
                source.get().localPath(),
                output,
                cutStartMs,
                cutEndMs,
                cutEndMs - cutStartMs,
                Instant.parse(cutCreatedAt)
            ));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public ProjectStage minimumResumeStage() {
        if (sourceMedia().isEmpty()) return ProjectStage.SOURCE;
        if (currentStage == ProjectStage.SOURCE) return ProjectStage.SOURCE;
        if (currentStage == ProjectStage.WAVE) return ProjectStage.WAVE;
        return mediaCut().isPresent() ? ProjectStage.PREPARATION : ProjectStage.WAVE;
    }

    public String recoveryMessage() {
        ProjectStage minimum = minimumResumeStage();
        if (minimum == currentStage) return "";
        return "Alguns dados precisam ser refeitos a partir de " + minimum.userLabel() + ".";
    }

    private static String requireText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " não pode ser vazio.");
        return normalized;
    }
}
