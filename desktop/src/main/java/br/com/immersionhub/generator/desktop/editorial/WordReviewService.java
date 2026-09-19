package br.com.immersionhub.generator.desktop.editorial;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class WordReviewService {
    private final EditorialMaterialRepository repository;
    private final WordReviewCursorRepository cursorRepository;

    public WordReviewService(
        EditorialMaterialRepository repository,
        WordReviewCursorRepository cursorRepository
    ) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.cursorRepository = Objects.requireNonNull(cursorRepository, "cursorRepository");
    }

    public WordReviewPosition loadCursor(EditorialMaterial material) {
        ensureCueReviewComplete(material);
        WordReviewPosition stored = cursorRepository.load(material);
        return normalizePosition(material, stored);
    }

    public void saveCursor(EditorialMaterial material, WordReviewPosition position) throws Exception {
        ensureCueReviewComplete(material);
        cursorRepository.save(material, normalizePosition(material, position));
    }

    public EditorialMaterial saveDraft(
        EditorialMaterial material,
        WordReviewPosition position,
        String approvedEn,
        String pt
    ) throws Exception {
        return update(material, position, approvedEn, pt, EditorialReviewStatus.PENDING);
    }

    public EditorialMaterial approve(
        EditorialMaterial material,
        WordReviewPosition position,
        String approvedEn,
        String pt
    ) throws Exception {
        return update(material, position, approvedEn, pt, EditorialReviewStatus.APPROVED);
    }

    public WordReviewMutation groupWithPrevious(
        EditorialMaterial material,
        WordReviewPosition position
    ) throws Exception {
        ensureCueReviewComplete(material);
        EditorialMaterial upgraded = material.upgradeToCurrentSchema();
        UnitRange current = unitRange(upgraded, position);
        if (current.startIndex() <= 1) {
            throw new IllegalArgumentException("Não existe unidade anterior nesta cue.");
        }
        UnitRange previous = unitRange(
            upgraded,
            new WordReviewPosition(position.cueOrder(), current.startIndex() - 1)
        );
        return group(upgraded, position.cueOrder(), previous.startIndex(), current.endIndex());
    }

    public WordReviewMutation groupWithNext(
        EditorialMaterial material,
        WordReviewPosition position
    ) throws Exception {
        ensureCueReviewComplete(material);
        EditorialMaterial upgraded = material.upgradeToCurrentSchema();
        UnitRange current = unitRange(upgraded, position);
        EditorialCue cue = cue(upgraded, position);
        if (current.endIndex() >= cue.words().size()) {
            throw new IllegalArgumentException("Não existe unidade seguinte nesta cue.");
        }
        UnitRange next = unitRange(
            upgraded,
            new WordReviewPosition(position.cueOrder(), current.endIndex() + 1)
        );
        return group(upgraded, position.cueOrder(), current.startIndex(), next.endIndex());
    }

    public WordReviewMutation ungroup(
        EditorialMaterial material,
        WordReviewPosition position
    ) throws Exception {
        ensureCueReviewComplete(material);
        EditorialMaterial upgraded = material.upgradeToCurrentSchema();
        UnitRange range = unitRange(upgraded, position);
        if (range.startIndex() == range.endIndex()) {
            throw new IllegalArgumentException("Esta unidade não está agrupada.");
        }

        EditorialCue cue = cue(upgraded, position);
        List<EditorialWord> words = new ArrayList<>(cue.words());
        for (int index = range.startIndex(); index <= range.endIndex(); index++) {
            words.set(index - 1, words.get(index - 1).ungrouped());
        }

        EditorialMaterial updated = replaceCue(
            upgraded,
            cue.withReviewAndWords(cue.approvedEn(), cue.pt(), cue.reviewStatus(), words)
        );
        WordReviewPosition nextPosition = new WordReviewPosition(cue.order(), range.startIndex());
        repository.save(updated);
        cursorRepository.save(updated, nextPosition);
        return new WordReviewMutation(updated, nextPosition);
    }

    public boolean canGroupPrevious(EditorialMaterial material, WordReviewPosition position) {
        UnitRange range = unitRange(material, position);
        return range.startIndex() > 1;
    }

    public boolean canGroupNext(EditorialMaterial material, WordReviewPosition position) {
        UnitRange range = unitRange(material, position);
        return range.endIndex() < cue(material, position).words().size();
    }

    public boolean isGrouped(EditorialMaterial material, WordReviewPosition position) {
        UnitRange range = unitRange(material, position);
        return range.startIndex() != range.endIndex();
    }

    public int unitSize(EditorialMaterial material, WordReviewPosition position) {
        UnitRange range = unitRange(material, position);
        return range.endIndex() - range.startIndex() + 1;
    }

    public String unitEnglish(EditorialMaterial material, WordReviewPosition position) {
        UnitRange range = unitRange(material, position);
        EditorialCue cue = cue(material, position);
        return joinApprovedEnglish(cue.words(), range);
    }

    public String unitPt(EditorialMaterial material, WordReviewPosition position) {
        UnitRange range = unitRange(material, position);
        return cue(material, position).words().get(range.startIndex() - 1).pt();
    }

    public String unitOriginalReference(EditorialMaterial material, WordReviewPosition position) {
        UnitRange range = unitRange(material, position);
        EditorialCue cue = cue(material, position);
        List<String> values = new ArrayList<>();
        for (int index = range.startIndex(); index <= range.endIndex(); index++) {
            String value = cue.words().get(index - 1).originalEn();
            if (!value.isBlank()) values.add(value);
        }
        return values.isEmpty() ? "Nova unidade da revisão editorial" : String.join(" ", values);
    }

    public EditorialReviewStatus unitStatus(EditorialMaterial material, WordReviewPosition position) {
        UnitRange range = unitRange(material, position);
        EditorialCue cue = cue(material, position);
        for (int index = range.startIndex(); index <= range.endIndex(); index++) {
            if (cue.words().get(index - 1).reviewStatus() != EditorialReviewStatus.APPROVED) {
                return EditorialReviewStatus.PENDING;
            }
        }
        return EditorialReviewStatus.APPROVED;
    }

    public Optional<WordReviewPosition> nextPending(
        EditorialMaterial material,
        WordReviewPosition current
    ) {
        ensureCueReviewComplete(material);
        List<WordReviewPosition> positions = allPositions(material);
        WordReviewPosition normalized = normalizePosition(material, current);
        int currentIndex = positions.indexOf(normalized);
        if (currentIndex < 0) currentIndex = 0;

        for (int offset = 1; offset <= positions.size(); offset++) {
            WordReviewPosition candidate = positions.get((currentIndex + offset) % positions.size());
            if (unitStatus(material, candidate) == EditorialReviewStatus.PENDING) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    public Optional<WordReviewPosition> previous(
        EditorialMaterial material,
        WordReviewPosition current
    ) {
        List<WordReviewPosition> positions = allPositions(material);
        int index = positions.indexOf(normalizePosition(material, current));
        return index > 0 ? Optional.of(positions.get(index - 1)) : Optional.empty();
    }

    public Optional<WordReviewPosition> next(
        EditorialMaterial material,
        WordReviewPosition current
    ) {
        List<WordReviewPosition> positions = allPositions(material);
        int index = positions.indexOf(normalizePosition(material, current));
        return index >= 0 && index + 1 < positions.size()
            ? Optional.of(positions.get(index + 1))
            : Optional.empty();
    }

    public long approvedCount(EditorialMaterial material) {
        return allPositions(material).stream()
            .filter(position -> unitStatus(material, position) == EditorialReviewStatus.APPROVED)
            .count();
    }

    public int totalCount(EditorialMaterial material) {
        return allPositions(material).size();
    }

    public long cueApprovedCount(EditorialMaterial material, int cueOrder) {
        return cuePositions(material, cueOrder).stream()
            .filter(position -> unitStatus(material, position) == EditorialReviewStatus.APPROVED)
            .count();
    }

    public int cueTotalCount(EditorialMaterial material, int cueOrder) {
        return cuePositions(material, cueOrder).size();
    }

    public int ordinal(EditorialMaterial material, WordReviewPosition position) {
        List<WordReviewPosition> positions = allPositions(material);
        int index = positions.indexOf(normalizePosition(material, position));
        return index < 0 ? 1 : index + 1;
    }

    private EditorialMaterial update(
        EditorialMaterial material,
        WordReviewPosition position,
        String approvedEn,
        String pt,
        EditorialReviewStatus status
    ) throws Exception {
        ensureCueReviewComplete(material);
        WordReviewPosition normalizedPosition = normalizePosition(material, position);
        UnitRange range = unitRange(material, normalizedPosition);
        EditorialCue cue = cue(material, normalizedPosition);

        String nextEnglish = approvedEn == null ? "" : approvedEn.trim();
        if (nextEnglish.isEmpty()) throw new IllegalArgumentException("English não pode ser vazio.");

        List<String> currentTokens = tokens(unitEnglish(material, normalizedPosition));
        List<String> nextTokens = tokens(nextEnglish);
        if (currentTokens.size() != nextTokens.size()) {
            throw structuralEdit();
        }
        for (int index = 0; index < currentTokens.size(); index++) {
            if (!EditorialWordReconciler.normalizedToken(currentTokens.get(index))
                .equals(EditorialWordReconciler.normalizedToken(nextTokens.get(index)))) {
                throw structuralEdit();
            }
        }

        List<EditorialWord> words = new ArrayList<>(cue.words());
        boolean grouped = range.startIndex() != range.endIndex();

        for (int offset = 0; offset < nextTokens.size(); offset++) {
            int wordIndex = range.startIndex() + offset;
            EditorialWord current = words.get(wordIndex - 1);

            if (grouped) {
                String nextPt = offset == 0 ? normalized(pt) : "";
                words.set(
                    wordIndex - 1,
                    new EditorialWord(
                        current.index(),
                        current.originalEn(),
                        nextTokens.get(offset),
                        nextPt,
                        current.individualPt(),
                        current.startMs(),
                        current.endMs(),
                        current.confidence(),
                        current.semanticGroupId(),
                        current.semanticGroupRole(),
                        status
                    )
                );
            } else {
                words.set(
                    wordIndex - 1,
                    current.withReview(nextTokens.get(offset), normalized(pt), status)
                );
            }
        }

        String cueEnglish = replaceUnitTokensPreservingLayout(
            cue.approvedEn(),
            range.startIndex(),
            range.endIndex(),
            nextTokens
        );

        EditorialMaterial updated = replaceCue(
            material,
            cue.withReviewAndWords(cueEnglish, cue.pt(), cue.reviewStatus(), words)
        );

        repository.save(updated);
        cursorRepository.save(updated, normalizedPosition);
        return updated;
    }

    private WordReviewMutation group(
        EditorialMaterial material,
        int cueOrder,
        int startIndex,
        int endIndex
    ) throws Exception {
        if (endIndex - startIndex + 1 < 2) {
            throw new IllegalArgumentException("Grupo precisa conter ao menos duas unidades.");
        }

        EditorialCue cue = material.cues().get(cueOrder - 1);
        List<EditorialWord> words = new ArrayList<>(cue.words());
        String groupId = UUID.randomUUID().toString();

        boolean allTranslationsKnown = true;
        List<String> individualTranslations = new ArrayList<>();
        for (int index = startIndex; index <= endIndex; index++) {
            String individual = words.get(index - 1).individualPt();
            individualTranslations.add(individual);
            if (individual.isBlank()) allTranslationsKnown = false;
        }

        String initialGroupPt = allTranslationsKnown
            ? String.join(" ", individualTranslations)
            : "";

        for (int index = startIndex; index <= endIndex; index++) {
            EditorialWord current = words.get(index - 1);
            words.set(
                index - 1,
                current.grouped(
                    groupId,
                    index == startIndex ? SemanticGroupRole.LEAD : SemanticGroupRole.MEMBER,
                    index == startIndex ? initialGroupPt : "",
                    EditorialReviewStatus.PENDING
                )
            );
        }

        EditorialMaterial updated = replaceCue(
            material,
            cue.withReviewAndWords(cue.approvedEn(), cue.pt(), cue.reviewStatus(), words)
        );
        WordReviewPosition nextPosition = new WordReviewPosition(cueOrder, startIndex);
        repository.save(updated);
        cursorRepository.save(updated, nextPosition);
        return new WordReviewMutation(updated, nextPosition);
    }

    private static EditorialMaterial replaceCue(
        EditorialMaterial material,
        EditorialCue replacement
    ) {
        List<EditorialCue> cues = new ArrayList<>(material.cues());
        cues.set(replacement.order() - 1, replacement);
        return material.withCues(cues);
    }

    private static WordReviewPosition normalizePosition(
        EditorialMaterial material,
        WordReviewPosition position
    ) {
        UnitRange range = unitRange(material, position);
        return new WordReviewPosition(position.cueOrder(), range.startIndex());
    }

    private static UnitRange unitRange(
        EditorialMaterial material,
        WordReviewPosition position
    ) {
        if (!WordReviewCursorRepository.valid(material, position)) {
            throw new IllegalArgumentException("Word inválida.");
        }

        EditorialCue cue = cue(material, position);
        EditorialWord selected = cue.words().get(position.wordIndex() - 1);
        if (selected.semanticGroupRole() == SemanticGroupRole.NONE) {
            return new UnitRange(position.wordIndex(), position.wordIndex());
        }

        String groupId = selected.semanticGroupId();
        int start = position.wordIndex();
        int end = position.wordIndex();

        while (start > 1 && cue.words().get(start - 2).semanticGroupId().equals(groupId)) start--;
        while (end < cue.words().size() && cue.words().get(end).semanticGroupId().equals(groupId)) end++;

        return new UnitRange(start, end);
    }

    private static EditorialCue cue(
        EditorialMaterial material,
        WordReviewPosition position
    ) {
        return material.cues().get(position.cueOrder() - 1);
    }

    private static List<WordReviewPosition> allPositions(EditorialMaterial material) {
        ensureCueReviewComplete(material);
        List<WordReviewPosition> positions = new ArrayList<>();
        for (EditorialCue cue : material.cues()) {
            positions.addAll(cuePositions(material, cue.order()));
        }
        if (positions.isEmpty()) throw new IllegalArgumentException("Material editorial sem words.");
        return List.copyOf(positions);
    }

    private static List<WordReviewPosition> cuePositions(EditorialMaterial material, int cueOrder) {
        EditorialCue cue = material.cues().get(cueOrder - 1);
        List<WordReviewPosition> positions = new ArrayList<>();
        for (EditorialWord word : cue.words()) {
            if (word.semanticGroupRole() == SemanticGroupRole.MEMBER) continue;
            positions.add(new WordReviewPosition(cue.order(), word.index()));
        }
        return List.copyOf(positions);
    }

    private static void ensureCueReviewComplete(EditorialMaterial material) {
        if (!material.cuesApproved()) {
            throw new IllegalStateException("Conclua a revisão das cues antes do Word by Word.");
        }
    }

    private static List<String> tokens(String text) {
        return EditorialWordReconciler.tokens(text);
    }

    private static String replaceUnitTokensPreservingLayout(
        String text,
        int startIndex,
        int endIndex,
        List<String> replacements
    ) {
        var matcher = java.util.regex.Pattern
            .compile("[\\p{L}\\p{N}]+(?:['’.-][\\p{L}\\p{N}]+)*")
            .matcher(text);

        List<int[]> ranges = new ArrayList<>();
        while (matcher.find()) ranges.add(new int[]{matcher.start(), matcher.end()});

        if (startIndex < 1 || endIndex > ranges.size()
            || replacements.size() != endIndex - startIndex + 1) {
            throw new IllegalArgumentException("Unidade não corresponde ao English aprovado.");
        }

        String result = text;
        for (int offset = replacements.size() - 1; offset >= 0; offset--) {
            int tokenIndex = startIndex - 1 + offset;
            int[] range = ranges.get(tokenIndex);
            result = result.substring(0, range[0])
                + replacements.get(offset)
                + result.substring(range[1]);
        }
        return result;
    }

    private static String joinApprovedEnglish(List<EditorialWord> words, UnitRange range) {
        List<String> values = new ArrayList<>();
        for (int index = range.startIndex(); index <= range.endIndex(); index++) {
            values.add(words.get(index - 1).approvedEn());
        }
        return String.join(" ", values);
    }

    private static IllegalArgumentException structuralEdit() {
        return new IllegalArgumentException(
            "Para trocar palavras em English, volte para a revisão da cue."
        );
    }

    private static String normalized(String value) {
        return value == null ? "" : value.trim();
    }

    private record UnitRange(int startIndex, int endIndex) {}
}
