package br.com.immersionhub.generator.desktop.project;

import br.com.immersionhub.generator.desktop.editorial.EditorialMaterial;
import br.com.immersionhub.generator.desktop.editorial.WordReviewPosition;
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
    String editorialMaterialId,
    String editorialTranslationMaterialId,
    String editorialSchemaVersion,
    EditorialProjectStatus editorialStatus,
    EditorialProjectPhase editorialPhase,
    Integer editorialCueOrder,
    Integer editorialWordCueOrder,
    Integer editorialWordIndex,
    String editorialUpdatedAt,
    Instant createdAt,
    Instant updatedAt
) {
    public static final int CURRENT_SCHEMA_VERSION = 4;

    public ProjectState(
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
        this(
            schemaVersion,
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
            currentStage,
            completedStages,
            preparedMaterialId,
            alignedMaterialId,
            translationMaterialId,
            translationSource,
            null,
            null,
            null,
            EditorialProjectStatus.NOT_STARTED,
            EditorialProjectPhase.CUE_REVIEW,
            null,
            null,
            null,
            null,
            createdAt,
            updatedAt
        );
    }

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
        editorialStatus = editorialStatus == null ? EditorialProjectStatus.NOT_STARTED : editorialStatus;
        editorialPhase = editorialPhase == null ? EditorialProjectPhase.CUE_REVIEW : editorialPhase;
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        if (sourceDurationMs <= 0) throw new IllegalArgumentException("Duração da fonte inválida.");
        if (editorialCueOrder != null && editorialCueOrder <= 0) {
            throw new IllegalArgumentException("Cue editorial inválida.");
        }
        if ((editorialWordCueOrder == null) != (editorialWordIndex == null)) {
            throw new IllegalArgumentException("Cursor Word by Word incompleto.");
        }
        if (editorialWordCueOrder != null
            && (editorialWordCueOrder <= 0 || editorialWordIndex <= 0)) {
            throw new IllegalArgumentException("Cursor Word by Word inválido.");
        }
        if (editorialStatus == EditorialProjectStatus.COMPLETED
            && editorialPhase != EditorialProjectPhase.COMPLETE) {
            throw new IllegalArgumentException("Revisão concluída exige fase COMPLETE.");
        }
    }

    public static ProjectState start(SourceMedia source) {
        Instant now = Instant.now();
        return new ProjectState(
            CURRENT_SCHEMA_VERSION, UUID.randomUUID().toString(), source.title(),
            source.sourceId(), source.canonicalUrl(), source.localPath().toString(),
            source.title(), source.durationMs(), source.fetchedAt().toString(),
            null, null, null, null,
            ProjectStage.WAVE, EnumSet.of(ProjectStage.SOURCE),
            null, null, null, null,
            null, null, null,
            EditorialProjectStatus.NOT_STARTED, EditorialProjectPhase.CUE_REVIEW,
            null, null, null, null,
            now, now
        );
    }

    public ProjectState withSource(SourceMedia source) {
        return new ProjectState(
            CURRENT_SCHEMA_VERSION, projectId, source.title(),
            source.sourceId(), source.canonicalUrl(), source.localPath().toString(),
            source.title(), source.durationMs(), source.fetchedAt().toString(),
            null, null, null, null,
            ProjectStage.WAVE, EnumSet.of(ProjectStage.SOURCE),
            null, null, null, null,
            null, null, null,
            EditorialProjectStatus.NOT_STARTED, EditorialProjectPhase.CUE_REVIEW,
            null, null, null, null,
            createdAt, Instant.now()
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
            null, null, null, null,
            null, null, null,
            EditorialProjectStatus.NOT_STARTED, EditorialProjectPhase.CUE_REVIEW,
            null, null, null, null,
            createdAt, Instant.now()
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
            null, null,
            null, null, null,
            EditorialProjectStatus.NOT_STARTED, EditorialProjectPhase.CUE_REVIEW,
            null, null, null, null,
            createdAt, Instant.now()
        );
    }

    public ProjectState withTranslation(TranslationMaterial material) {
        Set<ProjectStage> completed = EnumSet.noneOf(ProjectStage.class);
        completed.addAll(completedStages);
        completed.add(ProjectStage.TRANSLATION);
        completed.remove(ProjectStage.EDITORIAL_REVIEW);
        return new ProjectState(
            CURRENT_SCHEMA_VERSION, projectId, title,
            sourceId, canonicalUrl, sourcePath, sourceTitle, sourceDurationMs, sourceFetchedAt,
            cutStartMs, cutEndMs, cutPath, cutCreatedAt,
            ProjectStage.EDITORIAL_REVIEW, completed,
            preparedMaterialId, alignedMaterialId,
            material.id(), material.source().name(),
            null, null, null,
            EditorialProjectStatus.NOT_STARTED, EditorialProjectPhase.CUE_REVIEW,
            null, null, null, null,
            createdAt, Instant.now()
        );
    }

    public ProjectState withEditorialProgress(
        EditorialMaterial material,
        int cueOrder,
        WordReviewPosition wordPosition
    ) {
        Objects.requireNonNull(material, "material");
        if (!translationCompleted()) {
            throw new IllegalStateException("Tradução precisa estar concluída antes da revisão.");
        }
        if (!material.translationMaterialId().equals(translationMaterialId)) {
            throw new IllegalArgumentException("A revisão não pertence à tradução atual.");
        }
        if (cueOrder <= 0 || cueOrder > material.cues().size()) {
            throw new IllegalArgumentException("Cue editorial inválida.");
        }

        EditorialProjectPhase phase;
        EditorialProjectStatus status;
        if (material.fullyApproved()) {
            phase = EditorialProjectPhase.COMPLETE;
            status = EditorialProjectStatus.COMPLETED;
        } else if (material.cuesApproved()) {
            phase = EditorialProjectPhase.WORD_REVIEW;
            status = EditorialProjectStatus.IN_PROGRESS;
        } else {
            phase = EditorialProjectPhase.CUE_REVIEW;
            status = EditorialProjectStatus.IN_PROGRESS;
        }

        Set<ProjectStage> completed = EnumSet.noneOf(ProjectStage.class);
        completed.addAll(completedStages);
        if (status == EditorialProjectStatus.COMPLETED) {
            completed.add(ProjectStage.EDITORIAL_REVIEW);
        } else {
            completed.remove(ProjectStage.EDITORIAL_REVIEW);
        }

        Instant now = Instant.now();
        return new ProjectState(
            CURRENT_SCHEMA_VERSION, projectId, title,
            sourceId, canonicalUrl, sourcePath, sourceTitle, sourceDurationMs, sourceFetchedAt,
            cutStartMs, cutEndMs, cutPath, cutCreatedAt,
            ProjectStage.EDITORIAL_REVIEW, completed,
            preparedMaterialId, alignedMaterialId,
            translationMaterialId, translationSource,
            material.id(), material.translationMaterialId(), material.schemaVersion(),
            status, phase,
            cueOrder,
            wordPosition == null ? null : wordPosition.cueOrder(),
            wordPosition == null ? null : wordPosition.wordIndex(),
            now.toString(),
            createdAt, now
        );
    }

    public ProjectState withEditorialInvalid() {
        Set<ProjectStage> completed = EnumSet.noneOf(ProjectStage.class);
        completed.addAll(completedStages);
        completed.remove(ProjectStage.EDITORIAL_REVIEW);
        Instant now = Instant.now();
        return new ProjectState(
            CURRENT_SCHEMA_VERSION, projectId, title,
            sourceId, canonicalUrl, sourcePath, sourceTitle, sourceDurationMs, sourceFetchedAt,
            cutStartMs, cutEndMs, cutPath, cutCreatedAt,
            ProjectStage.EDITORIAL_REVIEW, completed,
            preparedMaterialId, alignedMaterialId,
            translationMaterialId, translationSource,
            editorialMaterialId, editorialTranslationMaterialId, editorialSchemaVersion,
            EditorialProjectStatus.INVALID, editorialPhase,
            editorialCueOrder, editorialWordCueOrder, editorialWordIndex,
            editorialUpdatedAt,
            createdAt, now
        );
    }

    public ProjectState withEditorialNotStarted() {
        Set<ProjectStage> completed = EnumSet.noneOf(ProjectStage.class);
        completed.addAll(completedStages);
        completed.remove(ProjectStage.EDITORIAL_REVIEW);
        Instant now = Instant.now();
        return new ProjectState(
            CURRENT_SCHEMA_VERSION, projectId, title,
            sourceId, canonicalUrl, sourcePath, sourceTitle, sourceDurationMs, sourceFetchedAt,
            cutStartMs, cutEndMs, cutPath, cutCreatedAt,
            ProjectStage.EDITORIAL_REVIEW, completed,
            preparedMaterialId, alignedMaterialId,
            translationMaterialId, translationSource,
            null, null, null,
            EditorialProjectStatus.NOT_STARTED, EditorialProjectPhase.CUE_REVIEW,
            null, null, null, null,
            createdAt, now
        );
    }

    public boolean editorialDependencyMatches(TranslationMaterial translation) {
        if (translation == null || !translationCompleted()) return false;
        if (!translation.id().equals(translationMaterialId)) return false;
        return editorialTranslationMaterialId == null
            || editorialTranslationMaterialId.isBlank()
            || editorialTranslationMaterialId.equals(translation.id());
    }

    public Optional<WordReviewPosition> editorialWordPosition() {
        if (editorialWordCueOrder == null || editorialWordIndex == null) return Optional.empty();
        return Optional.of(new WordReviewPosition(editorialWordCueOrder, editorialWordIndex));
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
            null, null, null, null,
            null, null, null,
            EditorialProjectStatus.NOT_STARTED, EditorialProjectPhase.CUE_REVIEW,
            null, null, null, null,
            createdAt, Instant.now()
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
            null, null,
            null, null, null,
            EditorialProjectStatus.NOT_STARTED, EditorialProjectPhase.CUE_REVIEW,
            null, null, null, null,
            createdAt, Instant.now()
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
        if (stage == ProjectStage.EDITORIAL_REVIEW) {
            if (editorialStatus == EditorialProjectStatus.INVALID) {
                return ProjectStageStatus.NEEDS_REPROCESSING;
            }
            if (editorialStatus == EditorialProjectStatus.COMPLETED) {
                return ProjectStageStatus.COMPLETED;
            }
            if (editorialStatus == EditorialProjectStatus.IN_PROGRESS) {
                return ProjectStageStatus.IN_PROGRESS;
            }
            return ProjectStageStatus.NOT_STARTED;
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
        if (editorialStatus == EditorialProjectStatus.INVALID && translationCompleted()) {
            return "A revisão salva precisa ser reconstruída. As etapas anteriores foram preservadas.";
        }
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
