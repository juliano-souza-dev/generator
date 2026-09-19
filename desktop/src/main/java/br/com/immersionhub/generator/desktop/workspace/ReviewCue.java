package br.com.immersionhub.generator.desktop.workspace;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record ReviewCue(
    int order,
    String originalEn,
    String approvedEn,
    String pt,
    TemporalReview speechTiming,
    TemporalReview subtitleTiming,
    SpeakerAssignment speaker,
    List<ReviewUnit> units
) {
    public ReviewCue {
        if (order <= 0) throw new IllegalArgumentException("Cue inválida.");
        originalEn = require(originalEn, "originalEn");
        approvedEn = require(approvedEn, "approvedEn");
        pt = require(pt, "pt");
        speechTiming = Objects.requireNonNull(speechTiming, "speechTiming");
        subtitleTiming = Objects.requireNonNull(subtitleTiming, "subtitleTiming");
        speaker = Objects.requireNonNull(speaker, "speaker");
        units = List.copyOf(Objects.requireNonNull(units, "units"));
        if (units.isEmpty()) throw new IllegalArgumentException("Cue sem unidades.");

        Set<Integer> seen = new HashSet<>();
        int expected = 1;
        for (ReviewUnit unit : units) {
            for (int wordIndex : unit.wordIndexes()) {
                if (wordIndex != expected || !seen.add(wordIndex)) {
                    throw new IllegalArgumentException("Unidades não cobrem words em ordem.");
                }
                expected++;
            }
        }
    }

    public boolean temporalApproved() {
        return speechTiming.approvalStatus() == ReviewApprovalStatus.APPROVED
            && subtitleTiming.approvalStatus() == ReviewApprovalStatus.APPROVED
            && units.stream().allMatch(unit ->
                unit.timing().approvalStatus() == ReviewApprovalStatus.APPROVED
            );
    }

    private static String require(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " não pode ser vazio.");
        return normalized;
    }
}
