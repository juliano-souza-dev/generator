package br.com.immersionhub.generator.desktop.preparation;

import java.nio.file.Path;
import java.util.List;

public interface WordAligner {
    List<TimedText> align(Path technicalAudio, AsrResult transcription, long durationMs) throws Exception;
    String version();
}
