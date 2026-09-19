package br.com.immersionhub.generator.desktop.editorial;

import br.com.immersionhub.generator.desktop.translation.TranslationMaterial;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.time.Instant;
import java.util.List;

final class EditorialDocumentCodec {
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    String toJson(EditorialMaterial material) throws Exception {
        return mapper.writeValueAsString(Document.from(material));
    }

    EditorialMaterial parseAndValidate(String json, TranslationMaterial expectedTranslation) throws Exception {
        Document document = mapper.readValue(json, Document.class);
        EditorialMaterial material = document.toMaterial();

        if (!material.translationMaterialId().equals(expectedTranslation.id())) {
            throw new IllegalArgumentException("A revisão pertence a outra versão da tradução.");
        }
        if (material.cues().size() != expectedTranslation.cues().size()) {
            throw new IllegalArgumentException("Quantidade de cues não corresponde à tradução.");
        }

        for (int index = 0; index < material.cues().size(); index++) {
            EditorialCue editorial = material.cues().get(index);
            var translated = expectedTranslation.cues().get(index);

            if (editorial.order() != translated.order()
                || editorial.speechStartMs() != translated.speechStartMs()
                || editorial.speechEndMs() != translated.speechEndMs()
                || editorial.subtitleStartMs() != translated.subtitleStartMs()
                || editorial.subtitleEndMs() != translated.subtitleEndMs()
                || !editorial.speaker().equals(translated.speaker())
                || !editorial.originalEn().equals(translated.originalEn())) {
                throw new IllegalArgumentException("A revisão alterou campos protegidos da cue " + editorial.order() + ".");
            }

            if (editorial.words().size() != translated.words().size()) {
                throw new IllegalArgumentException("A revisão alterou words protegidas da cue " + editorial.order() + ".");
            }
            for (int wordIndex = 0; wordIndex < editorial.words().size(); wordIndex++) {
                EditorialWord editorialWord = editorial.words().get(wordIndex);
                var translatedWord = translated.words().get(wordIndex);
                if (editorialWord.index() != wordIndex + 1
                    || !editorialWord.originalEn().equals(translatedWord.text())
                    || !editorialWord.approvedEn().equals(translatedWord.text())
                    || !editorialWord.pt().isEmpty()
                    || editorialWord.startMs() != translatedWord.startMs()
                    || editorialWord.endMs() != translatedWord.endMs()
                    || !java.util.Objects.equals(editorialWord.confidence(), translatedWord.confidence())
                    || !editorialWord.semanticGroupId().isEmpty()
                    || editorialWord.semanticGroupRole() != SemanticGroupRole.NONE
                    || editorialWord.reviewStatus() != EditorialReviewStatus.PENDING) {
                    throw new IllegalArgumentException(
                        "A revisão de cues alterou words protegidas da cue " + editorial.order() + "."
                    );
                }
            }
        }

        return material;
    }

    static final class Document {
        public String id;
        public String translationMaterialId;
        public String schemaVersion;
        public List<CueDocument> cues;
        public String createdAt;

        public Document() {}

        static Document from(EditorialMaterial material) {
            Document document = new Document();
            document.id = material.id();
            document.translationMaterialId = material.translationMaterialId();
            document.schemaVersion = material.schemaVersion();
            document.cues = material.cues().stream().map(CueDocument::from).toList();
            document.createdAt = material.createdAt().toString();
            return document;
        }

        EditorialMaterial toMaterial() {
            return new EditorialMaterial(
                id,
                translationMaterialId,
                schemaVersion,
                cues.stream().map(CueDocument::toCue).toList(),
                Instant.parse(createdAt)
            );
        }
    }

    static final class CueDocument {
        public int order;
        public long speechStartMs;
        public long speechEndMs;
        public long subtitleStartMs;
        public long subtitleEndMs;
        public String speaker;
        public String originalEn;
        public String approvedEn;
        public String pt;
        public String reviewStatus;
        public List<WordDocument> words;

        public CueDocument() {}

        static CueDocument from(EditorialCue cue) {
            CueDocument document = new CueDocument();
            document.order = cue.order();
            document.speechStartMs = cue.speechStartMs();
            document.speechEndMs = cue.speechEndMs();
            document.subtitleStartMs = cue.subtitleStartMs();
            document.subtitleEndMs = cue.subtitleEndMs();
            document.speaker = cue.speaker();
            document.originalEn = cue.originalEn();
            document.approvedEn = cue.approvedEn();
            document.pt = cue.pt();
            document.reviewStatus = cue.reviewStatus().name();
            document.words = cue.words().stream().map(WordDocument::from).toList();
            return document;
        }

        EditorialCue toCue() {
            return new EditorialCue(
                order,
                speechStartMs,
                speechEndMs,
                subtitleStartMs,
                subtitleEndMs,
                speaker,
                originalEn,
                approvedEn,
                pt,
                EditorialReviewStatus.valueOf(reviewStatus),
                words.stream().map(WordDocument::toWord).toList()
            );
        }
    }

    static final class WordDocument {
        public int index;
        public String originalEn;
        public String approvedEn;
        public String pt;
        public long startMs;
        public long endMs;
        public Double confidence;
        public String semanticGroupId;
        public String semanticGroupRole;
        public String reviewStatus;

        public WordDocument() {}

        static WordDocument from(EditorialWord word) {
            WordDocument document = new WordDocument();
            document.index = word.index();
            document.originalEn = word.originalEn();
            document.approvedEn = word.approvedEn();
            document.pt = word.pt();
            document.startMs = word.startMs();
            document.endMs = word.endMs();
            document.confidence = word.confidence();
            document.semanticGroupId = word.semanticGroupId();
            document.semanticGroupRole = word.semanticGroupRole().name();
            document.reviewStatus = word.reviewStatus().name();
            return document;
        }

        EditorialWord toWord() {
            return new EditorialWord(
                index,
                originalEn,
                approvedEn,
                pt,
                startMs,
                endMs,
                confidence,
                semanticGroupId,
                SemanticGroupRole.valueOf(semanticGroupRole),
                EditorialReviewStatus.valueOf(reviewStatus)
            );
        }
    }
}
