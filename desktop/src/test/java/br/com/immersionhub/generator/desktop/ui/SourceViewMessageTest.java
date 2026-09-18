package br.com.immersionhub.generator.desktop.ui;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SourceViewMessageTest {
    @Test
    void preservesOnlyKnownInputValidationMessages() {
        assertEquals(
            "Informe uma URL do YouTube.",
            SourceView.userFacingSourceError(new IllegalArgumentException("Informe uma URL do YouTube."))
        );
    }

    @Test
    void hidesInternalDownloaderDetails() {
        String message = SourceView.userFacingSourceError(
            new IOException("ERROR: extractor failed at C:\\internal\\tool.exe --flag")
        );

        assertEquals(
            "Não foi possível preparar essa fonte. Verifique o link e tente novamente.",
            message
        );
        assertFalse(message.contains("extractor"));
        assertFalse(message.contains("tool.exe"));
    }
}
