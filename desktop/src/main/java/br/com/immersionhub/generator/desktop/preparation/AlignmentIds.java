package br.com.immersionhub.generator.desktop.preparation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class AlignmentIds {
    private AlignmentIds() {}

    public static String from(PreparedMaterial material, String alignerVersion) {
        try {
            String value = material.id() + "|" + alignerVersion;
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
