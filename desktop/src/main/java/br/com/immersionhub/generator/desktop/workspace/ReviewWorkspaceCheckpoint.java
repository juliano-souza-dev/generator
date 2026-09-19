package br.com.immersionhub.generator.desktop.workspace;

import java.util.Objects;

public record ReviewWorkspaceCheckpoint(
    String materialId,
    ReviewWorkspaceCursor cursor,
    ReviewAutosaveState autosave
) {
    public ReviewWorkspaceCheckpoint {
        materialId = Objects.requireNonNull(materialId, "materialId").trim();
        if (materialId.isEmpty()) throw new IllegalArgumentException("materialId vazio.");
        cursor = Objects.requireNonNull(cursor, "cursor");
        autosave = Objects.requireNonNull(autosave, "autosave");
    }

    public boolean canResume(ReviewWorkspaceMaterial material) {
        return material != null
            && material.id().equals(materialId)
            && material.containsCursor(cursor)
            && (autosave.status() == AutosaveStatus.CLEAN
                || autosave.status() == AutosaveStatus.SAVED);
    }

    public ReviewWorkspaceCheckpoint dirty() {
        return new ReviewWorkspaceCheckpoint(materialId, cursor, autosave.dirty());
    }

    public ReviewWorkspaceCheckpoint saved(java.time.Instant at) {
        return new ReviewWorkspaceCheckpoint(materialId, cursor, autosave.saved(at));
    }

    public ReviewWorkspaceCheckpoint failed(String message) {
        return new ReviewWorkspaceCheckpoint(materialId, cursor, autosave.failed(message));
    }

    public ReviewWorkspaceCheckpoint moveTo(ReviewWorkspaceCursor next) {
        return new ReviewWorkspaceCheckpoint(
            materialId,
            Objects.requireNonNull(next, "next"),
            autosave
        );
    }
}
