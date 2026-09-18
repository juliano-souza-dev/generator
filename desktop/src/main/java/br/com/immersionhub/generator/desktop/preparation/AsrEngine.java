package br.com.immersionhub.generator.desktop.preparation;

import java.nio.file.Path;

public interface AsrEngine {
    AsrResult transcribeEnglish(Path technicalAudio) throws Exception;
    String version();
}
