package br.com.immersionhub.generator.desktop.timing;

import br.com.immersionhub.generator.desktop.model.MediaCut;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MediaCutRepository {
    private final Path metadataPath;
    private final ObjectMapper mapper = new ObjectMapper();

    public MediaCutRepository(Path metadataPath) {
        this.metadataPath = metadataPath.toAbsolutePath().normalize();
    }

    public void save(MediaCut cut) throws Exception {
        Files.createDirectories(metadataPath.getParent());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sourceId", cut.sourceId());
        payload.put("sourcePath", cut.sourcePath().toString());
        payload.put("outputPath", cut.outputPath().toString());
        payload.put("startMs", cut.startMs());
        payload.put("endMs", cut.endMs());
        payload.put("durationMs", cut.durationMs());
        payload.put("createdAt", cut.createdAt().toString());
        mapper.writerWithDefaultPrettyPrinter().writeValue(metadataPath.toFile(), payload);
    }

    public Path metadataPath() {
        return metadataPath;
    }
}
