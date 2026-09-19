package br.com.immersionhub.generator.desktop.translation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public final class GroqTranslationResponseCodec {
    private final ObjectMapper mapper = new ObjectMapper();

    public List<TranslatedCue> parse(String json, List<TranslationCue> expected) throws Exception {
        JsonNode root = mapper.readTree(json);
        JsonNode translations = root.path("translations");
        if (!translations.isArray() || translations.size() != expected.size()) {
            throw new IllegalArgumentException("Resposta Groq com cobertura incompleta.");
        }

        List<TranslatedCue> result = new ArrayList<>();
        for (int index = 0; index < expected.size(); index++) {
            TranslationCue expectedCue = expected.get(index);
            JsonNode item = translations.get(index);
            TranslatedCue translated = new TranslatedCue(
                item.path("order").asInt(0),
                item.path("pt").asText("")
            );
            if (translated.order() != expectedCue.order()) {
                throw new IllegalArgumentException(
                    "Resposta Groq alterou a ordem esperada da cue " + expectedCue.order() + "."
                );
            }
            result.add(translated);
        }
        return List.copyOf(result);
    }
}
