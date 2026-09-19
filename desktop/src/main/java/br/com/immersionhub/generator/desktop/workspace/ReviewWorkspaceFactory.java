package br.com.immersionhub.generator.desktop.workspace;

import br.com.immersionhub.generator.desktop.editorial.EditorialCue;
import br.com.immersionhub.generator.desktop.editorial.EditorialMaterial;
import br.com.immersionhub.generator.desktop.editorial.EditorialWord;
import br.com.immersionhub.generator.desktop.editorial.SemanticGroupRole;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ReviewWorkspaceFactory {
    private ReviewWorkspaceFactory() {}

    public static ReviewWorkspaceMaterial fromEditorial(
        EditorialMaterial editorial,
        long mediaDurationMs
    ) {
        Objects.requireNonNull(editorial, "editorial");
        if (!editorial.fullyApproved()) {
            throw new IllegalArgumentException(
                "O Review Workspace exige material editorial totalmente aprovado."
            );
        }
        if (mediaDurationMs <= 0) throw new IllegalArgumentException("Duração de mídia inválida.");

        Map<String, ReviewSpeaker> speakerByName = new LinkedHashMap<>();
        for (EditorialCue cue : editorial.cues()) {
            if (!cue.speaker().isBlank()) {
                speakerByName.computeIfAbsent(
                    cue.speaker(),
                    ignored -> new ReviewSpeaker(
                        "speaker-" + (speakerByName.size() + 1),
                        cue.speaker()
                    )
                );
            }
        }

        List<ReviewCue> cues = editorial.cues().stream()
            .map(cue -> cue(cue, speakerByName))
            .toList();

        return ReviewWorkspaceMaterial.create(
            editorial.id(),
            mediaDurationMs,
            List.copyOf(speakerByName.values()),
            cues,
            Instant.now()
        );
    }

    private static ReviewCue cue(
        EditorialCue source,
        Map<String, ReviewSpeaker> speakerByName
    ) {
        String speakerId = source.speaker().isBlank()
            ? ""
            : speakerByName.get(source.speaker()).id();

        return new ReviewCue(
            source.order(),
            source.originalEn(),
            source.approvedEn(),
            source.pt(),
            TemporalReview.fromAutomatic(
                new TemporalBounds(source.speechStartMs(), source.speechEndMs())
            ),
            TemporalReview.fromAutomatic(
                new TemporalBounds(source.subtitleStartMs(), source.subtitleEndMs())
            ),
            SpeakerAssignment.pending(speakerId),
            units(source)
        );
    }

    private static List<ReviewUnit> units(EditorialCue cue) {
        List<ReviewUnit> result = new ArrayList<>();
        List<EditorialWord> words = cue.words();
        int index = 0;

        while (index < words.size()) {
            EditorialWord first = words.get(index);
            if (first.semanticGroupRole() == SemanticGroupRole.NONE) {
                result.add(unit(
                    "cue-" + cue.order() + "-word-" + first.index(),
                    List.of(first),
                    first.pt()
                ));
                index++;
                continue;
            }

            String groupId = first.semanticGroupId();
            List<EditorialWord> members = new ArrayList<>();
            while (index < words.size()
                && groupId.equals(words.get(index).semanticGroupId())) {
                members.add(words.get(index));
                index++;
            }

            if (members.getFirst().semanticGroupRole() != SemanticGroupRole.LEAD) {
                throw new IllegalArgumentException("Grupo semântico deve iniciar em LEAD.");
            }

            result.add(unit(
                "cue-" + cue.order() + "-group-" + groupId,
                members,
                members.getFirst().pt()
            ));
        }

        return List.copyOf(result);
    }

    private static ReviewUnit unit(
        String id,
        List<EditorialWord> members,
        String pt
    ) {
        String en = members.stream()
            .map(EditorialWord::approvedEn)
            .reduce((left, right) -> left + " " + right)
            .orElseThrow();

        List<Integer> indexes = members.stream().map(EditorialWord::index).toList();
        TemporalReview timing = referenceTiming(members);

        return new ReviewUnit(id, indexes, en, pt, timing);
    }

    private static TemporalReview referenceTiming(List<EditorialWord> members) {
        if (members.stream().anyMatch(word -> !word.timed())) {
            return TemporalReview.untimed();
        }

        long start = members.getFirst().startMs();
        long end = members.getLast().endMs();
        return TemporalReview.fromAutomatic(new TemporalBounds(start, end));
    }
}
