package br.com.immersionhub.generator.desktop.workspace;

public record SpeakerAssignment(
    String speakerId,
    ReviewApprovalStatus approvalStatus
) {
    public SpeakerAssignment {
        speakerId = speakerId == null ? "" : speakerId.trim();
        approvalStatus = approvalStatus == null ? ReviewApprovalStatus.PENDING : approvalStatus;
        if (speakerId.isEmpty() && approvalStatus == ReviewApprovalStatus.APPROVED) {
            throw new IllegalArgumentException("Speaker vazio não pode ser aprovado.");
        }
    }

    public static SpeakerAssignment pending(String speakerId) {
        return new SpeakerAssignment(speakerId, ReviewApprovalStatus.PENDING);
    }

    public SpeakerAssignment approve() {
        if (speakerId.isEmpty()) throw new IllegalStateException("Selecione um speaker antes de aprovar.");
        return new SpeakerAssignment(speakerId, ReviewApprovalStatus.APPROVED);
    }
}
