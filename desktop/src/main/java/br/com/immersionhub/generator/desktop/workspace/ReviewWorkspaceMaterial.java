package br.com.immersionhub.generator.desktop.workspace;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record ReviewWorkspaceMaterial(
    String id,
    String editorialMaterialId,
    String schemaVersion,
    long mediaDurationMs,
    List<ReviewSpeaker> speakers,
    List<ReviewCue> cues,
    Instant createdAt
) {
    public static final String SCHEMA_VERSION = "1.0";

    public ReviewWorkspaceMaterial {
        id = require(id, "id");
        editorialMaterialId = require(editorialMaterialId, "editorialMaterialId");
        schemaVersion = require(schemaVersion, "schemaVersion");
        if (mediaDurationMs <= 0) throw new IllegalArgumentException("Duração de mídia inválida.");

        speakers = List.copyOf(Objects.requireNonNull(speakers, "speakers"));
        cues = List.copyOf(Objects.requireNonNull(cues, "cues"));
        createdAt = Objects.requireNonNull(createdAt, "createdAt");

        if (cues.isEmpty()) throw new IllegalArgumentException("Review Workspace sem cues.");

        Set<String> speakerIds = new HashSet<>();
        for (ReviewSpeaker speaker : speakers) {
            if (!speakerIds.add(speaker.id())) {
                throw new IllegalArgumentException("Speaker duplicado.");
            }
        }

        for (int index = 0; index < cues.size(); index++) {
            ReviewCue cue = cues.get(index);
            if (cue.order() != index + 1) {
                throw new IllegalArgumentException("Ordem de cues inválida.");
            }
            validateTiming(cue.speechTiming(), mediaDurationMs);
            validateTiming(cue.subtitleTiming(), mediaDurationMs);

            String speakerId = cue.speaker().speakerId();
            if (!speakerId.isEmpty() && !speakerIds.contains(speakerId)) {
                throw new IllegalArgumentException("Cue aponta para speaker inexistente.");
            }

            for (ReviewUnit unit : cue.units()) {
                validateTiming(unit.timing(), mediaDurationMs);
            }
        }

        String expectedId = ReviewWorkspaceIds.materialId(
            editorialMaterialId,
            schemaVersion,
            mediaDurationMs,
            speakers,
            cues
        );
        if (!expectedId.equals(id)) {
            throw new IllegalArgumentException("Identidade do Review Workspace não corresponde ao conteúdo.");
        }
    }

    public static ReviewWorkspaceMaterial create(
        String editorialMaterialId,
        long mediaDurationMs,
        List<ReviewSpeaker> speakers,
        List<ReviewCue> cues,
        Instant createdAt
    ) {
        String id = ReviewWorkspaceIds.materialId(
            editorialMaterialId,
            SCHEMA_VERSION,
            mediaDurationMs,
            speakers,
            cues
        );
        return new ReviewWorkspaceMaterial(
            id,
            editorialMaterialId,
            SCHEMA_VERSION,
            mediaDurationMs,
            speakers,
            cues,
            createdAt
        );
    }

    public boolean fullyApproved() {
        return cues.stream().allMatch(cue ->
            cue.temporalApproved()
                && cue.speaker().approvalStatus() == ReviewApprovalStatus.APPROVED
        );
    }

    public boolean dependsOn(String editorialId) {
        return editorialMaterialId.equals(editorialId);
    }

    public boolean containsCursor(ReviewWorkspaceCursor cursor) {
        if (cursor == null || cursor.cueOrder() > cues.size()) return false;
        ReviewCue cue = cues.get(cursor.cueOrder() - 1);
        if (cursor.selectionKind() == ReviewSelectionKind.CUE) return true;
        return cue.units().stream().anyMatch(unit -> unit.id().equals(cursor.unitId()));
    }

    private static void validateTiming(TemporalReview review, long mediaDurationMs) {
        review.reference().ifPresent(bounds -> bounds.requireWithin(mediaDurationMs));
        review.current().ifPresent(bounds -> bounds.requireWithin(mediaDurationMs));
    }

    private static String require(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " não pode ser vazio.");
        return normalized;
    }
}
