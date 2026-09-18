package br.com.immersionhub.generator.desktop.preparation;

import br.com.immersionhub.generator.desktop.model.MediaCut;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class PreparationIds {
    private PreparationIds() {}
    public static String from(MediaCut cut, String pipelineVersion, String asrVersion) {
        try {
            String value = cut.sourceId()+"|"+cut.startMs()+"|"+cut.endMs()+"|"+pipelineVersion+"|"+asrVersion;
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
