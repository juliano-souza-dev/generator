package br.com.immersionhub.generator.desktop.editorial;

import br.com.immersionhub.generator.desktop.translation.TranslationCue;
import br.com.immersionhub.generator.desktop.translation.TranslationMaterial;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

public final class EditorialReviewService {
    private final EditorialMaterialRepository repository;
    private final EditorialReviewCursorRepository cursorRepository;
    private final EditorialWordReconciler wordReconciler;

    public EditorialReviewService(
        EditorialMaterialRepository repository,
        EditorialReviewCursorRepository cursorRepository
    ) {
        this(repository, cursorRepository, new EditorialWordReconciler());
    }

    EditorialReviewService(
        EditorialMaterialRepository repository,
        EditorialReviewCursorRepository cursorRepository,
        EditorialWordReconciler wordReconciler
    ) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.cursorRepository = Objects.requireNonNull(cursorRepository, "cursorRepository");
        this.wordReconciler = Objects.requireNonNull(wordReconciler, "wordReconciler");
    }

    public EditorialMaterial loadOrCreate(TranslationMaterial translation) throws Exception {
        Objects.requireNonNull(translation, "translation");
        var existing = repository.load(translation);
        if (existing.isPresent()) return existing.get();

        EditorialMaterial created = EditorialMaterialFactory.fromTranslation(translation);
        repository.save(created);
        cursorRepository.save(1, created.cues().size());
        return created;
    }

    public EditorialMaterial saveDraft(
        EditorialMaterial material,
        int cueOrder,
        String approvedEn,
        String pt
    ) throws Exception {
        return updateCue(
            material,
            cueOrder,
            approvedEn,
            pt,
            EditorialReviewStatus.PENDING
        );
    }

    public EditorialMaterial approve(
        EditorialMaterial material,
        int cueOrder,
        String approvedEn,
        String pt
    ) throws Exception {
        return updateCue(
            material,
            cueOrder,
            approvedEn,
            pt,
            EditorialReviewStatus.APPROVED
        );
    }

    public EditorialMaterial restoreSuggestion(
        EditorialMaterial material,
        TranslationMaterial translation,
        int cueOrder
    ) throws Exception {
        TranslationCue source = translation.cues().stream()
            .filter(value -> value.order() == cueOrder)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Cue não encontrada na tradução."));

        return updateCue(
            material,
            cueOrder,
            source.approvedEn(),
            source.pt(),
            EditorialReviewStatus.PENDING
        );
    }

    public int loadCursor(EditorialMaterial material) {
        return cursorRepository.load(material.cues().size());
    }

    public void saveCursor(EditorialMaterial material, int cueOrder) throws Exception {
        cursorRepository.save(cueOrder, material.cues().size());
    }

    public OptionalInt nextPending(EditorialMaterial material, int currentOrder) {
        int size = material.cues().size();
        for (int offset = 1; offset <= size; offset++) {
            int order = ((currentOrder - 1 + offset) % size) + 1;
            if (cue(material, order).reviewStatus() == EditorialReviewStatus.PENDING) {
                return OptionalInt.of(order);
            }
        }
        return OptionalInt.empty();
    }

    private EditorialMaterial updateCue(
        EditorialMaterial material,
        int cueOrder,
        String approvedEn,
        String pt,
        EditorialReviewStatus status
    ) throws Exception {
        Objects.requireNonNull(material, "material");
        EditorialCue current = cue(material, cueOrder);
        String normalizedEn = approvedEn == null ? "" : approvedEn.trim();

        EditorialWordReconciliationResult reconciliation = null;
        EditorialCue replacement;

        if (!normalizedEn.equals(current.approvedEn())) {
            reconciliation = wordReconciler.reconcile(current.words(), normalizedEn);
            replacement = current.withReviewAndWords(
                normalizedEn,
                pt,
                status,
                reconciliation.words()
            );
        } else {
            replacement = current.withReview(normalizedEn, pt, status);
        }

        return replaceCue(material, cueOrder, replacement, reconciliation);
    }

    private EditorialMaterial replaceCue(
        EditorialMaterial material,
        int cueOrder,
        EditorialCue replacement,
        EditorialWordReconciliationResult reconciliation
    ) throws Exception {
        List<EditorialCue> cues = new ArrayList<>(material.cues());
        cues.set(cueOrder - 1, replacement);

        EditorialMaterial updated;
        if (reconciliation == null) {
            updated = material.withCues(cues);
        } else {
            updated = material.withReconciliation(
                cues,
                new EditorialReconciliation(
                    material.id(),
                    cueOrder,
                    EditorialReconciliationReason.APPROVED_EN_EDIT,
                    reconciliation.preservedWords(),
                    reconciliation.insertedWords(),
                    reconciliation.removedWords(),
                    reconciliation.invalidatedGroups()
                )
            );
        }

        repository.save(updated);
        cursorRepository.save(cueOrder, updated.cues().size());
        return updated;
    }

    private static EditorialCue cue(EditorialMaterial material, int cueOrder) {
        if (cueOrder < 1 || cueOrder > material.cues().size()) {
            throw new IllegalArgumentException("Cue inválida.");
        }
        return material.cues().get(cueOrder - 1);
    }
}
