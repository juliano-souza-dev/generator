package br.com.immersionhub.generator.desktop.timing;

import br.com.immersionhub.generator.desktop.model.MediaCut;
import br.com.immersionhub.generator.desktop.model.SourceMedia;

import java.nio.file.Path;
import java.util.List;

public interface MediaProcessor {
    List<Double> waveform(SourceMedia sourceMedia, int points) throws Exception;

    default Path preview(SourceMedia sourceMedia, Path outputDirectory) throws Exception {
        return sourceMedia.localPath();
    }

    MediaCut cut(SourceMedia sourceMedia, long startMs, long endMs, Path outputDirectory) throws Exception;
}
