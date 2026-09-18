package br.com.immersionhub.generator.desktop.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SourceViewFeedbackTest {
    @Test
    void preservesProductSafeInvalidUrlFeedback() {
        assertEquals(
            "Informe uma URL válida do YouTube.",
            SourceView.userFacingSourceError(new IllegalArgumentException("Informe uma URL válida do YouTube."))
        );
    }

    @Test
    void hidesTechnicalFailureDetails() {
        assertEquals(
            "Não foi possível preparar essa fonte agora. Tente novamente.",
            SourceView.userFacingSourceError(new RuntimeException("yt-dlp HTTP 503 player client web failed"))
        );
    }
}
