package br.com.immersionhub.generator.desktop.translation;

import java.util.List;

public interface GroqClient {
    GroqModelInfo modelInfo() throws Exception;
    GroqCompletion translate(List<TranslationCue> cues) throws Exception;
}
