package br.com.immersionhub.generator.desktop.editorial;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EditorialWordReconcilerTest {
    private final EditorialWordReconciler reconciler = new EditorialWordReconciler();

    @Test
    void insertionPreservesCompatibleTimingsAndCreatesUntimedPendingWord() {
        EditorialWordReconciliationResult result = reconciler.reconcile(
            baseWords(),
            "I really do like this"
        );

        assertEquals(List.of("I", "really", "do", "like", "this"),
            result.words().stream().map(EditorialWord::approvedEn).toList());
        assertEquals(4, result.preservedWords());
        assertEquals(1, result.insertedWords());
        assertEquals(0, result.removedWords());

        EditorialWord inserted = result.words().get(2);
        assertFalse(inserted.timed());
        assertEquals("", inserted.originalEn());
        assertNull(inserted.confidence());
        assertEquals(EditorialReviewStatus.PENDING, inserted.reviewStatus());

        assertEquals(200L, result.words().get(3).startMs());
        assertEquals(300L, result.words().get(3).endMs());
    }

    @Test
    void removalDropsOrphanWithoutMovingRemainingTiming() {
        EditorialWordReconciliationResult result = reconciler.reconcile(
            baseWords(),
            "I like this"
        );

        assertEquals(List.of("I", "like", "this"),
            result.words().stream().map(EditorialWord::approvedEn).toList());
        assertEquals(3, result.preservedWords());
        assertEquals(0, result.insertedWords());
        assertEquals(1, result.removedWords());
        assertEquals(200L, result.words().get(1).startMs());
        assertEquals(300L, result.words().get(1).endMs());
    }

    @Test
    void substitutionNeverInheritsRemovedWordTiming() {
        EditorialWordReconciliationResult result = reconciler.reconcile(
            baseWords(),
            "I really love this"
        );

        EditorialWord replacement = result.words().get(2);
        assertEquals("love", replacement.approvedEn());
        assertFalse(replacement.timed());
        assertNull(replacement.confidence());
        assertEquals("", replacement.originalEn());

        assertEquals(3, result.preservedWords());
        assertEquals(1, result.insertedWords());
        assertEquals(1, result.removedWords());
    }

    @Test
    void punctuationAndCaseDoNotDestroyCompatibleTimings() {
        EditorialWordReconciliationResult result = reconciler.reconcile(
            baseWords(),
            "i REALLY like this!"
        );

        assertEquals(4, result.preservedWords());
        assertEquals(0, result.insertedWords());
        assertEquals(0, result.removedWords());
        assertTrue(result.words().stream().allMatch(EditorialWord::timed));
        assertEquals(List.of("i", "REALLY", "like", "this"),
            result.words().stream().map(EditorialWord::approvedEn).toList());
    }

    @Test
    void editInsideSemanticGroupDissolvesAffectedGroupWithoutInventingPt() {
        List<EditorialWord> grouped = List.of(
            word(1, "I", 0, 90),
            new EditorialWord(
                2, "really", "really", "intensificador",
                100L, 190L, 0.9, "g1", SemanticGroupRole.LEAD, EditorialReviewStatus.APPROVED
            ),
            new EditorialWord(
                3, "like", "like", "",
                200L, 290L, 0.9, "g1", SemanticGroupRole.MEMBER, EditorialReviewStatus.APPROVED
            ),
            word(4, "this", 300, 390)
        );

        EditorialWordReconciliationResult result = reconciler.reconcile(
            grouped,
            "I really do like this"
        );

        assertEquals(1, result.invalidatedGroups());

        EditorialWord really = result.words().get(1);
        EditorialWord like = result.words().get(3);

        assertEquals("", really.semanticGroupId());
        assertEquals(SemanticGroupRole.NONE, really.semanticGroupRole());
        assertEquals("", really.pt());
        assertEquals(EditorialReviewStatus.PENDING, really.reviewStatus());

        assertEquals("", like.semanticGroupId());
        assertEquals(SemanticGroupRole.NONE, like.semanticGroupRole());
        assertEquals(EditorialReviewStatus.PENDING, like.reviewStatus());
    }

    private static List<EditorialWord> baseWords() {
        return List.of(
            word(1, "I", 0, 90),
            word(2, "really", 100, 190),
            word(3, "like", 200, 300),
            word(4, "this", 310, 400)
        );
    }

    private static EditorialWord word(int index, String text, long start, long end) {
        return new EditorialWord(
            index,
            text,
            text,
            "",
            start,
            end,
            0.9,
            "",
            SemanticGroupRole.NONE,
            EditorialReviewStatus.PENDING
        );
    }
}
