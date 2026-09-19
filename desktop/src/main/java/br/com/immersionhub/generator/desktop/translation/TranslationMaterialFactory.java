package br.com.immersionhub.generator.desktop.translation;

import br.com.immersionhub.generator.desktop.preparation.AlignedMaterial;
import br.com.immersionhub.generator.desktop.preparation.AsrResult;
import br.com.immersionhub.generator.desktop.preparation.TimedText;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class TranslationMaterialFactory {
    private TranslationMaterialFactory() {}

    public static TranslationMaterial base(AlignedMaterial alignedMaterial) {
        AsrResult transcription = alignedMaterial.transcription();
        List<TranslationCue> cues = new ArrayList<>();

        for (int index = 0; index < transcription.segments().size(); index++) {
            TimedText segment = transcription.segments().get(index);
            List<TimedText> words = transcription.words().stream()
                .filter(word -> belongsTo(word, segment))
                .toList();

            cues.add(new TranslationCue(
                index + 1,
                segment.startMs(),
                segment.endMs(),
                segment.startMs(),
                segment.endMs(),
                "",
                segment.text(),
                segment.text(),
                "",
                words
            ));
        }

        return new TranslationMaterial(
            TranslationIds.from(alignedMaterial.id()),
            alignedMaterial.id(),
            TranslationMaterial.SCHEMA_VERSION,
            cues,
            TranslationSource.GROQ,
            Instant.now()
        );
    }

    public static TranslationMaterial apply(
        TranslationMaterial base,
        List<TranslatedCue> translations,
        TranslationSource source
    ) {
        if (translations.size() != base.cues().size()) {
            throw new IllegalArgumentException("Cobertura de tradução incompleta.");
        }

        List<TranslationCue> translated = new ArrayList<>(base.cues().size());
        for (int index = 0; index < base.cues().size(); index++) {
            TranslationCue cue = base.cues().get(index);
            TranslatedCue result = translations.get(index);
            if (result.order() != cue.order()) {
                throw new IllegalArgumentException("Ordem de tradução incompatível.");
            }
            translated.add(cue.withPt(result.pt()));
        }

        TranslationMaterial material = new TranslationMaterial(
            base.id(),
            base.alignedMaterialId(),
            base.schemaVersion(),
            translated,
            source,
            Instant.now()
        );
        if (!material.complete()) throw new IllegalArgumentException("Tradução incompleta.");
        return material;
    }

    private static boolean belongsTo(TimedText word, TimedText segment) {
        long midpoint = word.startMs() + ((word.endMs() - word.startMs()) / 2L);
        return midpoint >= segment.startMs() && midpoint <= segment.endMs();
    }
}
