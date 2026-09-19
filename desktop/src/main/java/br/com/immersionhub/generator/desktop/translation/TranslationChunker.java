package br.com.immersionhub.generator.desktop.translation;

import java.util.ArrayList;
import java.util.List;

public final class TranslationChunker {
    private final TokenEstimator estimator;

    public TranslationChunker(TokenEstimator estimator) {
        this.estimator = estimator;
    }

    public List<List<TranslationCue>> chunk(TranslationMaterial material, GroqModelInfo model) {
        long context = model.contextWindow();
        long maxCompletion = Math.min(model.maxCompletionTokens(), Math.max(1024L, context / 3L));
        long safeTotal = Math.max(2048L, (long) Math.floor(context * 0.72d));
        long maxPrompt = Math.max(1024L, safeTotal - Math.min(maxCompletion, safeTotal / 2L));

        List<List<TranslationCue>> chunks = new ArrayList<>();
        List<TranslationCue> current = new ArrayList<>();

        for (TranslationCue cue : material.cues()) {
            long cuePrompt = estimator.estimateRequest(List.of(cue));
            long cueCompletion = estimator.estimateCompletion(List.of(cue));
            if (cuePrompt + cueCompletion > safeTotal) {
                throw new IllegalArgumentException(
                    "Uma unidade de tradução excede a capacidade segura do modelo atual."
                );
            }

            List<TranslationCue> candidate = new ArrayList<>(current);
            candidate.add(cue);
            long prompt = estimator.estimateRequest(candidate);
            long completion = estimator.estimateCompletion(candidate);

            if (!current.isEmpty() && (prompt > maxPrompt || prompt + completion > safeTotal)) {
                chunks.add(List.copyOf(current));
                current.clear();
            }
            current.add(cue);
        }

        if (!current.isEmpty()) chunks.add(List.copyOf(current));
        return List.copyOf(chunks);
    }
}
