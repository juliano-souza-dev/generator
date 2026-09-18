package br.com.immersionhub.generator.desktop.preparation;

import br.com.immersionhub.generator.desktop.model.MediaCut;
import java.nio.file.Path;

public interface TechnicalAudioExtractor {
    Path extract(MediaCut cut, Path outputDirectory) throws Exception;
}
