package br.com.immersionhub.generator.desktop.translation;

import java.util.List;

public final class TokenEstimator {
    private static final long REQUEST_OVERHEAD = 450;

    public long estimateCue(TranslationCue cue) {
        // Deliberately conservative without binding the product to a model-specific tokenizer.
        long chars = cue.approvedEn().length() + 40L;
        return Math.max(24L, (chars + 2L) / 3L);
    }

    public long estimateRequest(List<TranslationCue> cues) {
        return REQUEST_OVERHEAD + cues.stream().mapToLong(this::estimateCue).sum();
    }

    public long estimateCompletion(List<TranslationCue> cues) {
        long input = cues.stream().mapToLong(this::estimateCue).sum();
        return Math.max(256L, (long) Math.ceil(input * 1.35d));
    }
}
