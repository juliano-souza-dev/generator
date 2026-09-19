package br.com.immersionhub.generator.desktop.workspace;

import java.util.Objects;

public final class ReviewWorkspacePolicy {
    private ReviewWorkspacePolicy() {}

    public static ReviewInvalidationScope invalidationFor(ReviewMutationKind kind) {
        return switch (Objects.requireNonNull(kind, "kind")) {
            case CUE_TEXT -> ReviewInvalidationScope.CUE_AND_AFFECTED_UNITS;
            case CUE_TIMING -> ReviewInvalidationScope.CUE_TIMING_ONLY;
            case UNIT_TEXT -> ReviewInvalidationScope.UNIT_ONLY;
            case UNIT_TIMING -> ReviewInvalidationScope.UNIT_TIMING_ONLY;
            case SEMANTIC_GROUP -> ReviewInvalidationScope.AFFECTED_GROUP_ONLY;
            case SPEAKER -> ReviewInvalidationScope.SPEAKER_ONLY;
            case EDITORIAL_DEPENDENCY -> ReviewInvalidationScope.WORKSPACE_DEPENDENCY;
        };
    }
}
