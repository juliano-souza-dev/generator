package br.com.immersionhub.generator.desktop.editorial;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
        return cursorRepository.load(material);
    }

    public void saveCursor(EditorialMaterial material, WordReviewPosition position) throws Exception {
        ensureCueReviewComplete(material);
        cursorRepository.save(material, position);
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

    public Optional<WordReviewPosition> nextPending(
        EditorialMaterial material,
        WordReviewPosition current
    ) {
        ensureCueReviewComplete(material);
        List<WordReviewPosition> positions = allPositions(material);
        int currentIndex = positions.indexOf(current);
        if (currentIndex < 0) currentIndex = 0;

        for (int offset = 1; offset <= positions.size(); offset++) {
            WordReviewPosition candidate = positions.get((currentIndex + offset) % positions.size());
            if (word(material, candidate).reviewStatus() == EditorialReviewStatus.PENDING) {
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
        int index = positions.indexOf(current);
        return index > 0 ? Optional.of(positions.get(index - 1)) : Optional.empty();
    }

    public Optional<WordReviewPosition> next(
        EditorialMaterial material,
        WordReviewPosition current
    ) {
        List<WordReviewPosition> positions = allPositions(material);
        int index = positions.indexOf(current);
        return index >= 0 && index + 1 < positions.size()
            ? Optional.of(positions.get(index + 1))
            : Optional.empty();
    }

    public long approvedCount(EditorialMaterial material) {
        return material.cues().stream()
            .flatMap(cue -> cue.words().stream())
            .filter(word -> word.reviewStatus() == EditorialReviewStatus.APPROVED)
            .count();
    }

    public int totalCount(EditorialMaterial material) {
        return material.cues().stream().mapToInt(cue -> cue.words().size()).sum();
    }

    private EditorialMaterial update(
        EditorialMaterial material,
        WordReviewPosition position,
        String approvedEn,
        String pt,
        EditorialReviewStatus status
    ) throws Exception {
        ensureCueReviewComplete(material);
        if (!WordReviewCursorRepository.valid(material, position)) {
            throw new IllegalArgumentException("Word inválida.");
        }

        EditorialCue cue = material.cues().get(position.cueOrder() - 1);
        EditorialWord current = cue.words().get(position.wordIndex() - 1);

        if (current.semanticGroupRole() != SemanticGroupRole.NONE) {
            throw new IllegalArgumentException("A unidade agrupada deve ser tratada pelo agrupamento semântico.");
        }

        String normalizedEn = approvedEn == null ? "" : approvedEn.trim();
        if (normalizedEn.isEmpty()) throw new IllegalArgumentException("English não pode ser vazio.");

        String oldToken = EditorialWordReconciler.normalizedToken(current.approvedEn());
        String newToken = EditorialWordReconciler.normalizedToken(normalizedEn);
        if (!oldToken.equals(newToken)) {
            throw new IllegalArgumentException(
                "Para trocar a palavra em English, volte para a revisão da cue."
            );
        }

        List<EditorialWord> words = new ArrayList<>(cue.words());
        words.set(
            position.wordIndex() - 1,
            current.withReview(normalizedEn, pt, status)
        );

        String cueEnglish = replaceTokenPreservingLayout(
            cue.approvedEn(),
            position.wordIndex(),
            normalizedEn
        );
        EditorialCue updatedCue = cue.withReviewAndWords(
            cueEnglish,
            cue.pt(),
            cue.reviewStatus(),
            words
        );

        List<EditorialCue> cues = new ArrayList<>(material.cues());
        cues.set(position.cueOrder() - 1, updatedCue);
        EditorialMaterial updated = material.withCues(cues);

        repository.save(updated);
        cursorRepository.save(updated, position);
        return updated;
    }

    private static EditorialWord word(EditorialMaterial material, WordReviewPosition position) {
        return material.cues().get(position.cueOrder() - 1).words().get(position.wordIndex() - 1);
    }

    private static List<WordReviewPosition> allPositions(EditorialMaterial material) {
        ensureCueReviewComplete(material);
        List<WordReviewPosition> positions = new ArrayList<>();
        for (EditorialCue cue : material.cues()) {
            for (EditorialWord word : cue.words()) {
                positions.add(new WordReviewPosition(cue.order(), word.index()));
            }
        }
        if (positions.isEmpty()) throw new IllegalArgumentException("Material editorial sem words.");
        return List.copyOf(positions);
    }

    private static void ensureCueReviewComplete(EditorialMaterial material) {
        if (!material.cuesApproved()) {
            throw new IllegalStateException("Conclua a revisão das cues antes do Word by Word.");
        }
    }

    private static String replaceTokenPreservingLayout(
        String text,
        int wordIndex,
        String replacement
    ) {
        var matcher = java.util.regex.Pattern
            .compile("[\\p{L}\\p{N}]+(?:['’.-][\\p{L}\\p{N}]+)*")
            .matcher(text);

        int current = 0;
        while (matcher.find()) {
            current++;
            if (current == wordIndex) {
                return text.substring(0, matcher.start())
                    + replacement
                    + text.substring(matcher.end());
            }
        }
        throw new IllegalArgumentException("Word não encontrada no English aprovado.");
    }
}
