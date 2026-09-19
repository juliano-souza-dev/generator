package br.com.immersionhub.generator.desktop.workspace;

import java.time.Instant;

public record ReviewAutosaveState(
    AutosaveStatus status,
    long revision,
    Instant lastSavedAt,
    String errorMessage
) {
    public ReviewAutosaveState {
        status = status == null ? AutosaveStatus.CLEAN : status;
        if (revision < 0) throw new IllegalArgumentException("Revisão de autosave inválida.");
        errorMessage = errorMessage == null ? "" : errorMessage.trim();
        if (status == AutosaveStatus.ERROR && errorMessage.isEmpty()) {
            throw new IllegalArgumentException("Falha de autosave exige mensagem.");
        }
    }

    public static ReviewAutosaveState clean() {
        return new ReviewAutosaveState(AutosaveStatus.CLEAN, 0, null, "");
    }

    public ReviewAutosaveState dirty() {
        return new ReviewAutosaveState(AutosaveStatus.DIRTY, revision + 1, lastSavedAt, "");
    }

    public ReviewAutosaveState saved(Instant at) {
        if (status != AutosaveStatus.DIRTY) {
            throw new IllegalStateException("Somente draft alterado pode ser marcado como salvo.");
        }
        return new ReviewAutosaveState(
            AutosaveStatus.SAVED,
            revision,
            java.util.Objects.requireNonNull(at, "at"),
            ""
        );
    }

    public ReviewAutosaveState failed(String message) {
        return new ReviewAutosaveState(
            AutosaveStatus.ERROR,
            revision,
            lastSavedAt,
            message
        );
    }
}
