package br.com.immersionhub.generator.desktop.translation;

import br.com.immersionhub.generator.desktop.preparation.TimedText;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TranslationDocumentCodec {
    public static final String SCHEMA = "immersionhub-translation-material";

    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public String toJson(TranslationMaterial material) throws Exception {
        return mapper.writeValueAsString(Document.from(material));
    }

    public TranslationMaterial parseAndValidate(
        String json,
        TranslationMaterial expected,
        TranslationSource source
    ) throws Exception {
        Document document = mapper.readValue(json, Document.class);
        validateProtected(document, expected);

        List<TranslatedCue> translated = new ArrayList<>();
        for (Cue cue : document.cues) {
            if (cue.pt == null || cue.pt.isBlank()) {
                throw new IllegalArgumentException("Cue " + cue.order + " sem tradução PT.");
            }
            translated.add(new TranslatedCue(cue.order, cue.pt));
        }
        return TranslationMaterialFactory.apply(expected, translated, source);
    }

    private static void validateProtected(Document document, TranslationMaterial expected) {
        if (!SCHEMA.equals(document.schema)) {
            throw new IllegalArgumentException("Schema de tradução incompatível.");
        }
        if (!TranslationMaterial.SCHEMA_VERSION.equals(document.schemaVersion)) {
            throw new IllegalArgumentException("Versão do material incompatível.");
        }
        if (!expected.alignedMaterialId().equals(document.alignedMaterialId)) {
            throw new IllegalArgumentException("O retorno pertence a outra versão do material preparado.");
        }
        if (document.cues == null || document.cues.size() != expected.cues().size()) {
            throw new IllegalArgumentException("Quantidade de cues incompatível.");
        }

        for (int index = 0; index < expected.cues().size(); index++) {
            TranslationCue expectedCue = expected.cues().get(index);
            Cue actual = document.cues.get(index);

            if (actual.order != expectedCue.order()
                || actual.speechStartMs != expectedCue.speechStartMs()
                || actual.speechEndMs != expectedCue.speechEndMs()
                || actual.subtitleStartMs != expectedCue.subtitleStartMs()
                || actual.subtitleEndMs != expectedCue.subtitleEndMs()
                || !Objects.equals(actual.speaker, expectedCue.speaker())
                || !Objects.equals(actual.originalEn, expectedCue.originalEn())
                || !Objects.equals(actual.approvedEn, expectedCue.approvedEn())) {
                throw new IllegalArgumentException(
                    "Campos protegidos da cue " + expectedCue.order() + " foram alterados."
                );
            }

            if (actual.words == null || actual.words.size() != expectedCue.words().size()) {
                throw new IllegalArgumentException(
                    "Words protegidas da cue " + expectedCue.order() + " foram alteradas."
                );
            }
            for (int wordIndex = 0; wordIndex < expectedCue.words().size(); wordIndex++) {
                TimedText expectedWord = expectedCue.words().get(wordIndex);
                Word actualWord = actual.words.get(wordIndex);
                if (!Objects.equals(actualWord.text, expectedWord.text())
                    || actualWord.startMs != expectedWord.startMs()
                    || actualWord.endMs != expectedWord.endMs()
                    || !Objects.equals(actualWord.confidence, expectedWord.confidence())) {
                    throw new IllegalArgumentException(
                        "Timing/texto protegido de word foi alterado na cue " + expectedCue.order() + "."
                    );
                }
            }
        }
    }

    public static final class Document {
        public String schema;
        public String schemaVersion;
        public String alignedMaterialId;
        public List<Cue> cues;

        public Document() {}

        static Document from(TranslationMaterial material) {
            Document document = new Document();
            document.schema = SCHEMA;
            document.schemaVersion = material.schemaVersion();
            document.alignedMaterialId = material.alignedMaterialId();
            document.cues = material.cues().stream().map(Cue::from).toList();
            return document;
        }
    }

    public static final class Cue {
        public int order;
        public long speechStartMs;
        public long speechEndMs;
        public long subtitleStartMs;
        public long subtitleEndMs;
        public String speaker;
        public String originalEn;
        public String approvedEn;
        public String pt;
        public List<Word> words;

        public Cue() {}

        static Cue from(TranslationCue cue) {
            Cue item = new Cue();
            item.order = cue.order();
            item.speechStartMs = cue.speechStartMs();
            item.speechEndMs = cue.speechEndMs();
            item.subtitleStartMs = cue.subtitleStartMs();
            item.subtitleEndMs = cue.subtitleEndMs();
            item.speaker = cue.speaker();
            item.originalEn = cue.originalEn();
            item.approvedEn = cue.approvedEn();
            item.pt = cue.pt();
            item.words = cue.words().stream().map(Word::from).toList();
            return item;
        }
    }

    public static final class Word {
        public String text;
        public long startMs;
        public long endMs;
        public Double confidence;

        public Word() {}

        static Word from(TimedText word) {
            Word item = new Word();
            item.text = word.text();
            item.startMs = word.startMs();
            item.endMs = word.endMs();
            item.confidence = word.confidence();
            return item;
        }
    }
}
