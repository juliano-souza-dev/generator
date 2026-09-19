package br.com.immersionhub.generator.desktop.editorial;

import br.com.immersionhub.generator.desktop.preparation.TimedText;
import br.com.immersionhub.generator.desktop.translation.TranslationCue;
import br.com.immersionhub.generator.desktop.translation.TranslationMaterial;
import br.com.immersionhub.generator.desktop.translation.TranslationSource;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EditorialMaterialContractTest {
    @Test
    void translationBecomesPendingEditorialMaterialWithoutAutoApproval() {
        TranslationMaterial source = translation();
        EditorialMaterial material = EditorialMaterialFactory.fromTranslation(source);

        assertEquals(source.id(), material.translationMaterialId());
        assertFalse(material.fullyApproved());
        assertEquals("machine original", material.cues().getFirst().originalEn());
        assertEquals("human base", material.cues().getFirst().approvedEn());
        assertEquals("tradução", material.cues().getFirst().pt());
        assertEquals(EditorialReviewStatus.PENDING, material.cues().getFirst().reviewStatus());
        assertTrue(material.cues().getFirst().words().stream()
            .allMatch(word -> word.reviewStatus() == EditorialReviewStatus.PENDING));
    }

    @Test
    void identityIsDeterministicForSameReviewedContent() {
        EditorialMaterial first = EditorialMaterialFactory.fromTranslation(translation());
        EditorialMaterial second = EditorialMaterial.create(
            first.translationMaterialId(),
            first.cues(),
            first.createdAt().plusSeconds(30)
        );

        assertEquals(first.id(), second.id());
    }

    @Test
    void contractRejectsForgedIdentity() {
        EditorialMaterial valid = EditorialMaterialFactory.fromTranslation(translation());

        assertThrows(IllegalArgumentException.class, () -> new EditorialMaterial(
            "forged",
            valid.translationMaterialId(),
            valid.schemaVersion(),
            valid.cues(),
            valid.createdAt()
        ));
    }

    private static TranslationMaterial translation() {
        return new TranslationMaterial(
            "translation-1",
            "aligned-1",
            TranslationMaterial.SCHEMA_VERSION,
            List.of(new TranslationCue(
                1,
                100,
                900,
                100,
                900,
                "",
                "machine original",
                "human base",
                "tradução",
                List.of(
                    new TimedText("human", 100, 400, 0.9),
                    new TimedText("base", 410, 800, 0.8)
                )
            )),
            TranslationSource.GROQ,
            Instant.parse("2026-09-19T12:00:00Z")
        );
    }
}
