package br.com.immersionhub.generator.desktop.workspace;

import br.com.immersionhub.generator.desktop.editorial.EditorialCue;
import br.com.immersionhub.generator.desktop.editorial.EditorialMaterial;
import br.com.immersionhub.generator.desktop.editorial.EditorialReviewStatus;
import br.com.immersionhub.generator.desktop.editorial.EditorialWord;
import br.com.immersionhub.generator.desktop.editorial.SemanticGroupRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReviewWorkspaceContractTest {
    @Test
    void workspaceRequiresFullyApprovedEditorialMaterial() {
        EditorialMaterial pending = editorial(false);

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> ReviewWorkspaceFactory.fromEditorial(pending, 5_000)
        );

        assertTrue(error.getMessage().contains("totalmente aprovado"));
    }

    @Test
    void factoryBuildsSingleWorkspaceWithGroupsAndPendingTemporalApproval() {
        ReviewWorkspaceMaterial workspace = ReviewWorkspaceFactory.fromEditorial(
            editorial(true),
            5_000
        );

        assertEquals(ReviewWorkspaceMaterial.SCHEMA_VERSION, workspace.schemaVersion());
        assertEquals(2, workspace.cues().size());
        assertEquals(1, workspace.speakers().size());

        ReviewCue first = workspace.cues().getFirst();
        assertEquals("Speaker A", workspace.speakers().getFirst().name());
        assertEquals(workspace.speakers().getFirst().id(), first.speaker().speakerId());
        assertEquals(ReviewApprovalStatus.PENDING, first.speaker().approvalStatus());

        assertEquals(3, first.units().size());
        ReviewUnit group = first.units().get(1);
        assertTrue(group.grouped());
        assertEquals(List.of(2, 3), group.wordIndexes());
        assertEquals("really do", group.approvedEn());
        assertEquals("realmente", group.pt());

        assertEquals(
            ReviewApprovalStatus.PENDING,
            first.speechTiming().approvalStatus()
        );
        assertEquals(
            TimingOrigin.AUTOMATIC_REFERENCE,
            first.speechTiming().draftOrigin()
        );
        assertFalse(workspace.fullyApproved());
    }

    @Test
    void untimedEditorialWordRemainsUntimedInsteadOfReceivingInventedBounds() {
        ReviewWorkspaceMaterial workspace = ReviewWorkspaceFactory.fromEditorial(
            editorial(true),
            5_000
        );

        ReviewUnit untimed = workspace.cues().get(1).units().get(1);

        assertTrue(untimed.timing().reference().isEmpty());
        assertTrue(untimed.timing().current().isEmpty());
        assertEquals(ReviewApprovalStatus.PENDING, untimed.timing().approvalStatus());
    }

    @Test
    void humanDraftAndAutosaveDoNotApproveTiming() {
        TemporalReview automatic = TemporalReview.fromAutomatic(
            new TemporalBounds(100, 700)
        );

        TemporalReview edited = automatic.withHumanDraft(
            new TemporalBounds(120, 680)
        );

        assertEquals(TimingOrigin.HUMAN, edited.draftOrigin());
        assertEquals(ReviewApprovalStatus.PENDING, edited.approvalStatus());

        ReviewAutosaveState autosave = ReviewAutosaveState.clean()
            .dirty()
            .saved(Instant.parse("2026-09-19T15:00:00Z"));

        assertEquals(AutosaveStatus.SAVED, autosave.status());
        assertEquals(ReviewApprovalStatus.PENDING, edited.approvalStatus());

        TemporalReview approved = edited.approve();
        assertEquals(ReviewApprovalStatus.APPROVED, approved.approvalStatus());
        assertEquals(new TemporalBounds(120, 680), approved.current().orElseThrow());
    }

    @Test
    void checkpointResumesOnlyAgainstSameMaterialAndValidCursor() {
        ReviewWorkspaceMaterial first = ReviewWorkspaceFactory.fromEditorial(
            editorial(true),
            5_000
        );

        ReviewWorkspaceCursor cursor = ReviewWorkspaceCursor.unit(
            1,
            first.cues().getFirst().units().get(1).id()
        );
        ReviewWorkspaceCheckpoint checkpoint = new ReviewWorkspaceCheckpoint(
            first.id(),
            cursor,
            ReviewAutosaveState.clean()
        ).dirty().saved(Instant.parse("2026-09-19T15:01:00Z"));

        assertTrue(checkpoint.canResume(first));

        ReviewWorkspaceCheckpoint dirty = checkpoint.dirty();
        assertFalse(dirty.canResume(first));

        ReviewWorkspaceMaterial other = ReviewWorkspaceFactory.fromEditorial(
            editorialWithChangedPt(),
            5_000
        );
        assertFalse(checkpoint.canResume(other));

        ReviewWorkspaceCheckpoint failed = checkpoint.failed("Falha ao salvar.");
        assertFalse(failed.canResume(first));
    }

    @Test
    void materialIdentityIsDeterministicAndDoesNotDependOnCreationTime() {
        EditorialMaterial editorial = editorial(true);

        ReviewWorkspaceMaterial first = ReviewWorkspaceFactory.fromEditorial(editorial, 5_000);
        ReviewWorkspaceMaterial second = ReviewWorkspaceMaterial.create(
            first.editorialMaterialId(),
            first.mediaDurationMs(),
            first.speakers(),
            first.cues(),
            first.createdAt().plusSeconds(60)
        );

        assertEquals(first.id(), second.id());
    }

    @Test
    void timingBeyondMediaDurationIsRejected() {
        EditorialMaterial editorial = editorial(true);

        assertThrows(
            IllegalArgumentException.class,
            () -> ReviewWorkspaceFactory.fromEditorial(editorial, 1_500)
        );
    }

    @Test
    void invalidationPolicyIsMinimalAndExplicit() {
        assertEquals(
            ReviewInvalidationScope.CUE_TIMING_ONLY,
            ReviewWorkspacePolicy.invalidationFor(ReviewMutationKind.CUE_TIMING)
        );
        assertEquals(
            ReviewInvalidationScope.UNIT_TIMING_ONLY,
            ReviewWorkspacePolicy.invalidationFor(ReviewMutationKind.UNIT_TIMING)
        );
        assertEquals(
            ReviewInvalidationScope.SPEAKER_ONLY,
            ReviewWorkspacePolicy.invalidationFor(ReviewMutationKind.SPEAKER)
        );
        assertEquals(
            ReviewInvalidationScope.WORKSPACE_DEPENDENCY,
            ReviewWorkspacePolicy.invalidationFor(ReviewMutationKind.EDITORIAL_DEPENDENCY)
        );
    }

    private static EditorialMaterial editorial(boolean approved) {
        EditorialReviewStatus status = approved
            ? EditorialReviewStatus.APPROVED
            : EditorialReviewStatus.PENDING;

        List<EditorialWord> firstWords = List.of(
            word(1, "I", "eu", 100L, 200L, status),
            groupedWord(2, "really", "realmente", 210L, 350L, status, SemanticGroupRole.LEAD),
            groupedWord(3, "do", "realmente", 360L, 470L, status, SemanticGroupRole.MEMBER),
            word(4, "like", "gosto", 480L, 700L, status)
        );

        List<EditorialWord> secondWords = List.of(
            word(1, "It", "isso", 1_000L, 1_150L, status),
            word(2, "works", "funciona", null, null, status)
        );

        return EditorialMaterial.create(
            "translation-workspace",
            List.of(
                new EditorialCue(
                    1,
                    100,
                    800,
                    90,
                    850,
                    "Speaker A",
                    "I really do like",
                    "I really do like",
                    "Eu realmente gosto",
                    status,
                    firstWords
                ),
                new EditorialCue(
                    2,
                    1_000,
                    1_800,
                    980,
                    1_850,
                    "",
                    "It works",
                    "It works",
                    "Isso funciona",
                    status,
                    secondWords
                )
            ),
            Instant.parse("2026-09-19T14:00:00Z")
        );
    }

    private static EditorialMaterial editorialWithChangedPt() {
        EditorialMaterial source = editorial(true);
        EditorialCue first = source.cues().getFirst();

        EditorialCue changed = first.withReview(
            first.approvedEn(),
            "Eu gosto mesmo",
            EditorialReviewStatus.APPROVED
        );

        return EditorialMaterial.create(
            source.translationMaterialId(),
            List.of(changed, source.cues().get(1)),
            source.createdAt()
        );
    }

    private static EditorialWord word(
        int index,
        String en,
        String pt,
        Long start,
        Long end,
        EditorialReviewStatus status
    ) {
        return new EditorialWord(
            index,
            en,
            en,
            pt,
            start,
            end,
            start == null ? null : 0.9,
            "",
            SemanticGroupRole.NONE,
            status
        );
    }

    private static EditorialWord groupedWord(
        int index,
        String en,
        String groupPt,
        Long start,
        Long end,
        EditorialReviewStatus status,
        SemanticGroupRole role
    ) {
        return new EditorialWord(
            index,
            en,
            en,
            groupPt,
            en.equals("really") ? "realmente" : "ênfase",
            start,
            end,
            0.9,
            "g-1",
            role,
            status
        );
    }
}
