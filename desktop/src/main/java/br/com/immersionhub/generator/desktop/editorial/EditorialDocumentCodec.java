package br.com.immersionhub.generator.desktop.editorial;

import br.com.immersionhub.generator.desktop.translation.TranslationMaterial;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
                throw new IllegalArgumentException(
                    "A revisão alterou campos protegidos da cue " + editorial.order() + "."
                );
            }

            validateWordSequence(editorial);
            validateWordOrigins(editorial, translated.words());
        }

        return material;
    }

    private static void validateWordSequence(EditorialCue cue) {
        List<String> tokens = EditorialWordReconciler.tokens(cue.approvedEn());
        if (tokens.size() != cue.words().size()) {
            throw new IllegalArgumentException(
                "Words não correspondem ao English aprovado da cue " + cue.order() + "."
            );
        }

        for (int index = 0; index < tokens.size(); index++) {
            String expected = EditorialWordReconciler.normalizedToken(tokens.get(index));
            String actual = EditorialWordReconciler.normalizedToken(cue.words().get(index).approvedEn());
            if (!expected.equals(actual)) {
                throw new IllegalArgumentException(
                    "Ordem de words não corresponde ao English aprovado da cue " + cue.order() + "."
                );
            }
        }
    }

    private static void validateWordOrigins(
        EditorialCue cue,
        List<br.com.immersionhub.generator.desktop.preparation.TimedText> translatedWords
    ) {
        boolean[] used = new boolean[translatedWords.size()];

        for (EditorialWord word : cue.words()) {
            if (!word.timed()) {
                if (!word.originalEn().isEmpty() || word.confidence() != null) {
                    throw new IllegalArgumentException(
                        "Word reconciliada herdou metadado automático inválido na cue " + cue.order() + "."
                    );
                }
                continue;
            }

            boolean found = false;
            for (int index = 0; index < translatedWords.size(); index++) {
                if (used[index]) continue;
                var original = translatedWords.get(index);
                if (word.originalEn().equals(original.text())
                    && word.startMs().longValue() == original.startMs()
                    && word.endMs().longValue() == original.endMs()
                    && Objects.equals(word.confidence(), original.confidence())) {
                    used[index] = true;
                    found = true;
                    break;
                }
            }

            if (!found) {
                throw new IllegalArgumentException(
                    "Word reconciliada herdou timing que não pertence à tradução na cue " + cue.order() + "."
                );
            }
        }
    }

    static final class Document {
        public String id;
        public String translationMaterialId;
        public String schemaVersion;
        public List<CueDocument> cues;
        public List<ReconciliationDocument> reconciliations;
        public String createdAt;

        public Document() {}

        static Document from(EditorialMaterial material) {
            Document document = new Document();
            document.id = material.id();
            document.translationMaterialId = material.translationMaterialId();
            document.schemaVersion = material.schemaVersion();
            document.cues = material.cues().stream().map(CueDocument::from).toList();
            document.reconciliations = material.reconciliations().stream()
                .map(ReconciliationDocument::from)
                .toList();
            document.createdAt = material.createdAt().toString();
            return document;
        }

        EditorialMaterial toMaterial() {
            List<EditorialReconciliation> history = reconciliations == null
                ? List.of()
                : reconciliations.stream().map(ReconciliationDocument::toReconciliation).toList();

            return new EditorialMaterial(
                id,
                translationMaterialId,
                schemaVersion,
                cues.stream().map(CueDocument::toCue).toList(),
                history,
                Instant.parse(createdAt)
            );
        }
    }

    static final class ReconciliationDocument {
        public String sourceMaterialId;
        public int cueOrder;
        public String reason;
        public int preservedWords;
        public int insertedWords;
        public int removedWords;
        public int invalidatedGroups;

        public ReconciliationDocument() {}

        static ReconciliationDocument from(EditorialReconciliation reconciliation) {
            ReconciliationDocument document = new ReconciliationDocument();
            document.sourceMaterialId = reconciliation.sourceMaterialId();
            document.cueOrder = reconciliation.cueOrder();
            document.reason = reconciliation.reason().name();
            document.preservedWords = reconciliation.preservedWords();
            document.insertedWords = reconciliation.insertedWords();
            document.removedWords = reconciliation.removedWords();
            document.invalidatedGroups = reconciliation.invalidatedGroups();
            return document;
        }

        EditorialReconciliation toReconciliation() {
            return new EditorialReconciliation(
                sourceMaterialId,
                cueOrder,
                EditorialReconciliationReason.valueOf(reason),
                preservedWords,
                insertedWords,
                removedWords,
                invalidatedGroups
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
        public Long startMs;
        public Long endMs;
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
