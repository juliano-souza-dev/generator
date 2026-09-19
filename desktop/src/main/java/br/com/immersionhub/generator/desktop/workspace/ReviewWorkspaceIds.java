package br.com.immersionhub.generator.desktop.workspace;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

final class ReviewWorkspaceIds {
    private ReviewWorkspaceIds() {}

    static String materialId(
        String editorialMaterialId,
        String schemaVersion,
        long mediaDurationMs,
        List<ReviewSpeaker> speakers,
        List<ReviewCue> cues
    ) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, "immersionhub-review-workspace");
            update(digest, schemaVersion);
            update(digest, editorialMaterialId);
            update(digest, Long.toString(mediaDurationMs));

            for (ReviewSpeaker speaker : speakers) {
                update(digest, speaker.id());
                update(digest, speaker.name());
            }

            for (ReviewCue cue : cues) {
                update(digest, Integer.toString(cue.order()));
                update(digest, cue.originalEn());
                update(digest, cue.approvedEn());
                update(digest, cue.pt());
                updateTiming(digest, cue.speechTiming());
                updateTiming(digest, cue.subtitleTiming());
                update(digest, cue.speaker().speakerId());
                update(digest, cue.speaker().approvalStatus().name());

                for (ReviewUnit unit : cue.units()) {
                    update(digest, unit.id());
                    for (int wordIndex : unit.wordIndexes()) {
                        update(digest, Integer.toString(wordIndex));
                    }
                    update(digest, unit.approvedEn());
                    update(digest, unit.pt());
                    updateTiming(digest, unit.timing());
                }
            }

            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível identificar o Review Workspace.", exception);
        }
    }

    private static void updateTiming(MessageDigest digest, TemporalReview timing) {
        updateBounds(digest, timing.automaticReference());
        updateBounds(digest, timing.draft());
        update(digest, timing.draftOrigin().name());
        update(digest, timing.approvalStatus().name());
    }

    private static void updateBounds(MessageDigest digest, TemporalBounds bounds) {
        if (bounds == null) {
            update(digest, "");
            update(digest, "");
            return;
        }
        update(digest, Long.toString(bounds.startMs()));
        update(digest, Long.toString(bounds.endMs()));
    }

    private static void update(MessageDigest digest, String value) {
        digest.update(value.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
    }
}
