package br.com.immersionhub.generator.desktop.project;

import br.com.immersionhub.generator.desktop.model.MediaCut;
import br.com.immersionhub.generator.desktop.model.SourceMedia;
import br.com.immersionhub.generator.desktop.preparation.AlignedMaterial;
import br.com.immersionhub.generator.desktop.translation.TranslationMaterial;

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
    String translationMaterialId,
    String translationSource,
    Instant createdAt,
    Instant updatedAt
) {
    public static final int CURRENT_SCHEMA_VERSION = 3;

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
        completedStages = completedStages == null ? Set.of() : Set.copyOf(completedStages);
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        if (sourceDurationMs <= 0) throw new IllegalArgumentException("Duração da fonte inválida.");
    }

    public static ProjectState start(SourceMedia source) {
        Instant now = Instant.now();
        return new ProjectState(
            CURRENT_SCHEMA_VERSION, UUID.randomUUID().toString(), source.title(),
            source.sourceId(), source.canonicalUrl(), source.localPath().toString(),
            source.title(), source.durationMs(), source.fetchedAt().toString(),
            null, null, null, null,
            ProjectStage.WAVE, EnumSet.of(ProjectStage.SOURCE),
            null, null, null, null, now, now
        );
    }

    public ProjectState withSource(SourceMedia source) {
        return new ProjectState(
            CURRENT_SCHEMA_VERSION, projectId, source.title(),
            source.sourceId(), source.canonicalUrl(), source.localPath().toString(),
            source.title(), source.durationMs(), source.fetchedAt().toString(),
            null, null, null, null,
            ProjectStage.WAVE, EnumSet.of(ProjectStage.SOURCE),
            null, null, null, null, createdAt, Instant.now()
        );
    }

    public ProjectState withCut(MediaCut cut) {
        if (!sourceId.equals(cut.sourceId())) {
            throw new IllegalArgumentException("O recorte não pertence à fonte do projeto.");
        }
        return new ProjectState(
            CURRENT_SCHEMA_VERSION, projectId, title,
            sourceId, canonicalUrl, sourcePath, sourceTitle, sourceDurationMs, sourceFetchedAt,
            cut.startMs(), cut.endMs(), cut.outputPath().toString(), cut.createdAt().toString(),
            ProjectStage.PREPARATION, EnumSet.of(ProjectStage.SOURCE, ProjectStage.WAVE),
            null, null, null, null, createdAt, Instant.now()
        );
    }

    public ProjectState withPrepared(AlignedMaterial material) {
        MediaCut cut = material.preparedMaterial().mediaCut();
        return new ProjectState(
            CURRENT_SCHEMA_VERSION, projectId, title,
            sourceId, canonicalUrl, sourcePath, sourceTitle, sourceDurationMs, sourceFetchedAt,
            cut.startMs(), cut.endMs(), cut.outputPath().toString(), cut.createdAt().toString(),
            ProjectStage.TRANSLATION,
            EnumSet.of(ProjectStage.SOURCE, ProjectStage.WAVE, ProjectStage.PREPARATION),
            material.preparedMaterial().id(), material.id(),
            null, null, createdAt, Instant.now()
        );
    }

    public ProjectState withTranslation(TranslationMaterial material) {
        Set<ProjectStage> completed = EnumSet.noneOf(ProjectStage.class);
        completed.addAll(completedStages);
        completed.add(ProjectStage.TRANSLATION);
        return new ProjectState(
            CURRENT_SCHEMA_VERSION, projectId, title,
            sourceId, canonicalUrl, sourcePath, sourceTitle, sourceDurationMs, sourceFetchedAt,
            cutStartMs, cutEndMs, cutPath, cutCreatedAt,
            ProjectStage.EDITORIAL_REVIEW, completed,
            preparedMaterialId, alignedMaterialId,
            material.id(), material.source().name(),
            createdAt, Instant.now()
        );
    }

    public ProjectState withEditorialActivity() {
        return new ProjectState(
            CURRENT_SCHEMA_VERSION, projectId, title,
            sourceId, canonicalUrl, sourcePath, sourceTitle, sourceDurationMs, sourceFetchedAt,
            cutStartMs, cutEndMs, cutPath, cutCreatedAt,
            ProjectStage.EDITORIAL_REVIEW, completedStages,
            preparedMaterialId, alignedMaterialId,
            translationMaterialId, translationSource,
            createdAt, Instant.now()
        );
    }

    public ProjectState withoutPreparation() {
        Set<ProjectStage> completed = EnumSet.noneOf(ProjectStage.class);
        completed.addAll(completedStages);
        completed.remove(ProjectStage.PREPARATION);
        completed.remove(ProjectStage.TRANSLATION);
        completed.remove(ProjectStage.EDITORIAL_REVIEW);
        return new ProjectState(
            CURRENT_SCHEMA_VERSION, projectId, title,
            sourceId, canonicalUrl, sourcePath, sourceTitle, sourceDurationMs, sourceFetchedAt,
            cutStartMs, cutEndMs, cutPath, cutCreatedAt,
            ProjectStage.PREPARATION, completed,
            null, null, null, null, createdAt, Instant.now()
        );
    }

    public ProjectState withoutTranslation() {
        Set<ProjectStage> completed = EnumSet.noneOf(ProjectStage.class);
        completed.addAll(completedStages);
        completed.remove(ProjectStage.TRANSLATION);
        completed.remove(ProjectStage.EDITORIAL_REVIEW);
        return new ProjectState(
            CURRENT_SCHEMA_VERSION, projectId, title,
            sourceId, canonicalUrl, sourcePath, sourceTitle, sourceDurationMs, sourceFetchedAt,
            cutStartMs, cutEndMs, cutPath, cutCreatedAt,
            ProjectStage.TRANSLATION, completed,
            preparedMaterialId, alignedMaterialId,
            null, null, createdAt, Instant.now()
        );
    }

    public boolean preparationCompleted() {
        return completedStages.contains(ProjectStage.PREPARATION)
            && preparedMaterialId != null && !preparedMaterialId.isBlank()
            && alignedMaterialId != null && !alignedMaterialId.isBlank();
    }

    public boolean translationCompleted() {
        return completedStages.contains(ProjectStage.TRANSLATION)
            && translationMaterialId != null && !translationMaterialId.isBlank()
            && translationSource != null && !translationSource.isBlank();
    }

    public Optional<SourceMedia> sourceMedia() {
        try {
            Path path = Path.of(sourcePath).toAbsolutePath().normalize();
            if (!Files.isRegularFile(path)) return Optional.empty();
            return Optional.of(new SourceMedia(
                sourceId, canonicalUrl, path, sourceTitle, sourceDurationMs,
                Instant.parse(sourceFetchedAt), true
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
                sourceId, source.get().localPath(), output,
                cutStartMs, cutEndMs, cutEndMs - cutStartMs, Instant.parse(cutCreatedAt)
            ));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public ProjectStageStatus stageStatus(ProjectStage stage) {
        Objects.requireNonNull(stage, "stage");

        if (stage == ProjectStage.SOURCE && completedStages.contains(stage) && sourceMedia().isEmpty()) {
            return ProjectStageStatus.NEEDS_REPROCESSING;
        }
        if (stage == ProjectStage.WAVE && completedStages.contains(stage) && mediaCut().isEmpty()) {
            return ProjectStageStatus.NEEDS_REPROCESSING;
        }
        if (stage == ProjectStage.PREPARATION && completedStages.contains(stage) && !preparationCompleted()) {
            return ProjectStageStatus.NEEDS_REPROCESSING;
        }
        if (stage == ProjectStage.TRANSLATION && completedStages.contains(stage) && !translationCompleted()) {
            return ProjectStageStatus.NEEDS_REPROCESSING;
        }
        if (completedStages.contains(stage)) return ProjectStageStatus.COMPLETED;
        if (currentStage == stage) return ProjectStageStatus.IN_PROGRESS;
        return ProjectStageStatus.NOT_STARTED;
    }

    public ProjectStage minimumResumeStage() {
        if (sourceMedia().isEmpty()) return ProjectStage.SOURCE;
        if (currentStage == ProjectStage.SOURCE) return ProjectStage.SOURCE;
        if (mediaCut().isEmpty()) return ProjectStage.WAVE;
        if (!preparationCompleted()) return ProjectStage.PREPARATION;
        if (!translationCompleted()) return ProjectStage.TRANSLATION;
        return ProjectStage.EDITORIAL_REVIEW;
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
