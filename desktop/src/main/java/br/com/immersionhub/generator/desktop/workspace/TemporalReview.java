package br.com.immersionhub.generator.desktop.workspace;

import java.util.Objects;
import java.util.Optional;

public record TemporalReview(
    TemporalBounds automaticReference,
    TemporalBounds draft,
    TimingOrigin draftOrigin,
    ReviewApprovalStatus approvalStatus
) {
    public TemporalReview {
        draftOrigin = Objects.requireNonNull(draftOrigin, "draftOrigin");
        approvalStatus = Objects.requireNonNull(approvalStatus, "approvalStatus");

        if (draft == null && approvalStatus == ReviewApprovalStatus.APPROVED) {
            throw new IllegalArgumentException("Timing sem bounds não pode ser aprovado.");
        }
        if (draft == null && draftOrigin == TimingOrigin.HUMAN) {
            throw new IllegalArgumentException("Timing humano exige bounds.");
        }
    }

    public static TemporalReview fromAutomatic(TemporalBounds reference) {
        Objects.requireNonNull(reference, "reference");
        return new TemporalReview(
            reference,
            reference,
            TimingOrigin.AUTOMATIC_REFERENCE,
            ReviewApprovalStatus.PENDING
        );
    }

    public static TemporalReview untimed() {
        return new TemporalReview(
            null,
            null,
            TimingOrigin.AUTOMATIC_REFERENCE,
            ReviewApprovalStatus.PENDING
        );
    }

    public Optional<TemporalBounds> reference() {
        return Optional.ofNullable(automaticReference);
    }

    public Optional<TemporalBounds> current() {
        return Optional.ofNullable(draft);
    }

    public TemporalReview withHumanDraft(TemporalBounds next) {
        return new TemporalReview(
            automaticReference,
            Objects.requireNonNull(next, "next"),
            TimingOrigin.HUMAN,
            ReviewApprovalStatus.PENDING
        );
    }

    public TemporalReview approve() {
        if (draft == null) throw new IllegalStateException("Defina o timing antes de aprovar.");
        return new TemporalReview(
            automaticReference,
            draft,
            draftOrigin,
            ReviewApprovalStatus.APPROVED
        );
    }

    public TemporalReview invalidateApproval() {
        if (approvalStatus == ReviewApprovalStatus.PENDING) return this;
        return new TemporalReview(
            automaticReference,
            draft,
            draftOrigin,
            ReviewApprovalStatus.PENDING
        );
    }

    public boolean humanEdited() {
        return draftOrigin == TimingOrigin.HUMAN;
    }
}
