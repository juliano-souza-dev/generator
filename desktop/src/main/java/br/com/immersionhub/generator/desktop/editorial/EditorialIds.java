package br.com.immersionhub.generator.desktop.editorial;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

final class EditorialIds {
    private EditorialIds() {}

    static String materialId(
        String translationMaterialId,
        String schemaVersion,
        List<EditorialCue> cues
    ) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, "immersionhub-editorial-material");
            update(digest, schemaVersion);
            update(digest, translationMaterialId);

            for (EditorialCue cue : cues) {
                update(digest, Integer.toString(cue.order()));
                update(digest, Long.toString(cue.speechStartMs()));
                update(digest, Long.toString(cue.speechEndMs()));
                update(digest, Long.toString(cue.subtitleStartMs()));
                update(digest, Long.toString(cue.subtitleEndMs()));
                update(digest, cue.speaker());
                update(digest, cue.originalEn());
                update(digest, cue.approvedEn());
                update(digest, cue.pt());
                update(digest, cue.reviewStatus().name());

                for (EditorialWord word : cue.words()) {
                    update(digest, Integer.toString(word.index()));
                    update(digest, word.originalEn());
                    update(digest, word.approvedEn());
                    update(digest, word.pt());
                    update(digest, Long.toString(word.startMs()));
                    update(digest, Long.toString(word.endMs()));
                    update(digest, word.confidence() == null ? "" : word.confidence().toString());
                    update(digest, word.semanticGroupId());
                    update(digest, word.semanticGroupRole().name());
                    update(digest, word.reviewStatus().name());
                }
            }

            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível identificar o material editorial.", exception);
        }
    }

    private static void update(MessageDigest digest, String value) {
        digest.update(value.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
    }
}
