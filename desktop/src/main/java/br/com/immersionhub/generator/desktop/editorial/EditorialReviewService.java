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

    public EditorialReviewService(
        EditorialMaterialRepository repository,
        EditorialReviewCursorRepository cursorRepository
    ) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.cursorRepository = Objects.requireNonNull(cursorRepository, "cursorRepository");
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
        return replaceCue(
            material,
            cueOrder,
            cue(material, cueOrder).withReview(approvedEn, pt, EditorialReviewStatus.PENDING)
        );
    }

    public EditorialMaterial approve(
        EditorialMaterial material,
        int cueOrder,
        String approvedEn,
        String pt
    ) throws Exception {
        return replaceCue(
            material,
            cueOrder,
            cue(material, cueOrder).withReview(approvedEn, pt, EditorialReviewStatus.APPROVED)
        );
    }

    public EditorialMaterial restoreSuggestion(
        EditorialMaterial material,
        TranslationMaterial translation,
        int cueOrder
    ) throws Exception {
        EditorialCue current = cue(material, cueOrder);
        TranslationCue source = translation.cues().stream()
            .filter(value -> value.order() == cueOrder)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Cue não encontrada na tradução."));

        EditorialCue restored = current.withReview(
            source.approvedEn(),
            source.pt(),
            EditorialReviewStatus.PENDING
        );
        return replaceCue(material, cueOrder, restored);
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

    private EditorialMaterial replaceCue(
        EditorialMaterial material,
        int cueOrder,
        EditorialCue replacement
    ) throws Exception {
        List<EditorialCue> cues = new ArrayList<>(material.cues());
        cues.set(cueOrder - 1, replacement);
        EditorialMaterial updated = material.withCues(cues);
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
