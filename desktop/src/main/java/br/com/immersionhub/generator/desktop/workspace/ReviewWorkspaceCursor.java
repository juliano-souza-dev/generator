package br.com.immersionhub.generator.desktop.workspace;

import java.util.Objects;

public record ReviewWorkspaceCursor(
    int cueOrder,
    ReviewSelectionKind selectionKind,
    String unitId
) {
    public ReviewWorkspaceCursor {
        if (cueOrder <= 0) throw new IllegalArgumentException("Cue do cursor inválida.");
        selectionKind = Objects.requireNonNull(selectionKind, "selectionKind");
        unitId = unitId == null ? "" : unitId.trim();

        if (selectionKind == ReviewSelectionKind.CUE && !unitId.isEmpty()) {
            throw new IllegalArgumentException("Seleção de cue não possui unitId.");
        }
        if (selectionKind == ReviewSelectionKind.UNIT && unitId.isEmpty()) {
            throw new IllegalArgumentException("Seleção de unidade exige unitId.");
        }
    }

    public static ReviewWorkspaceCursor cue(int cueOrder) {
        return new ReviewWorkspaceCursor(cueOrder, ReviewSelectionKind.CUE, "");
    }

    public static ReviewWorkspaceCursor unit(int cueOrder, String unitId) {
        return new ReviewWorkspaceCursor(cueOrder, ReviewSelectionKind.UNIT, unitId);
    }
}
