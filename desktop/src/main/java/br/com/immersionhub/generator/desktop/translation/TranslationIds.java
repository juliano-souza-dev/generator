package br.com.immersionhub.generator.desktop.translation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class TranslationIds {
    private TranslationIds() {}

    public static String from(String alignedMaterialId) {
        try {
            String value = alignedMaterialId + "|" + TranslationMaterial.SCHEMA_VERSION;
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
