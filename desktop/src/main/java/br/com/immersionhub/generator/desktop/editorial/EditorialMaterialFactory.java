package br.com.immersionhub.generator.desktop.editorial;

import br.com.immersionhub.generator.desktop.translation.TranslationCue;
import br.com.immersionhub.generator.desktop.translation.TranslationMaterial;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class EditorialMaterialFactory {
    private EditorialMaterialFactory() {}

    public static EditorialMaterial fromTranslation(TranslationMaterial translation) {
        Objects.requireNonNull(translation, "translation");
        if (!translation.complete()) {
            throw new IllegalArgumentException("A revisão editorial exige tradução completa.");
        }

        List<EditorialCue> cues = translation.cues().stream()
            .map(EditorialMaterialFactory::cue)
            .toList();

        return EditorialMaterial.create(translation.id(), cues, Instant.now());
    }

    private static EditorialCue cue(TranslationCue source) {
        List<EditorialWord> words = new ArrayList<>();
        for (int index = 0; index < source.words().size(); index++) {
            var word = source.words().get(index);
            words.add(new EditorialWord(
                index + 1,
                word.text(),
                word.text(),
                "",
                word.startMs(),
                word.endMs(),
                word.confidence(),
                "",
                SemanticGroupRole.NONE,
                EditorialReviewStatus.PENDING
            ));
        }

        return new EditorialCue(
            source.order(),
            source.speechStartMs(),
            source.speechEndMs(),
            source.subtitleStartMs(),
            source.subtitleEndMs(),
            source.speaker(),
            source.originalEn(),
            source.approvedEn(),
            source.pt(),
            EditorialReviewStatus.PENDING,
            words
        );
    }
}
