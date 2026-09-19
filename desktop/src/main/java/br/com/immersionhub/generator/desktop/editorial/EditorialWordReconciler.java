package br.com.immersionhub.generator.desktop.editorial;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EditorialWordReconciler {
    private static final Pattern TOKEN = Pattern.compile(
        "[\\p{L}\\p{N}]+(?:['’.-][\\p{L}\\p{N}]+)*"
    );

    public EditorialWordReconciliationResult reconcile(
        List<EditorialWord> currentWords,
        String approvedEn
    ) {
        List<EditorialWord> oldWords = List.copyOf(currentWords);
        List<String> newTokens = tokens(approvedEn);

        int oldCount = oldWords.size();
        int newCount = newTokens.size();
        int[][] lcs = lcs(oldWords, newTokens);

        int[] newToOld = new int[newCount];
        int[] oldToNew = new int[oldCount];
        Arrays.fill(newToOld, -1);
        Arrays.fill(oldToNew, -1);

        int oldIndex = 0;
        int newIndex = 0;
        while (oldIndex < oldCount && newIndex < newCount) {
            if (sameToken(oldWords.get(oldIndex).approvedEn(), newTokens.get(newIndex))) {
                newToOld[newIndex] = oldIndex;
                oldToNew[oldIndex] = newIndex;
                oldIndex++;
                newIndex++;
            } else if (lcs[oldIndex + 1][newIndex] >= lcs[oldIndex][newIndex + 1]) {
                oldIndex++;
            } else {
                newIndex++;
            }
        }

        Set<String> invalidatedGroups = invalidatedGroups(oldWords, oldToNew);
        List<EditorialWord> reconciled = new ArrayList<>(newCount);

        int preserved = 0;
        int inserted = 0;

        for (int targetIndex = 0; targetIndex < newCount; targetIndex++) {
            String token = newTokens.get(targetIndex);
            int sourceIndex = newToOld[targetIndex];

            if (sourceIndex >= 0) {
                EditorialWord source = oldWords.get(sourceIndex);
                boolean groupInvalidated = !source.semanticGroupId().isEmpty()
                    && invalidatedGroups.contains(source.semanticGroupId());

                reconciled.add(new EditorialWord(
                    targetIndex + 1,
                    source.originalEn(),
                    token,
                    groupInvalidated ? source.individualPt() : source.pt(),
                    source.individualPt(),
                    source.startMs(),
                    source.endMs(),
                    source.confidence(),
                    groupInvalidated ? "" : source.semanticGroupId(),
                    groupInvalidated ? SemanticGroupRole.NONE : source.semanticGroupRole(),
                    groupInvalidated ? EditorialReviewStatus.PENDING : source.reviewStatus()
                ));
                preserved++;
            } else {
                reconciled.add(new EditorialWord(
                    targetIndex + 1,
                    "",
                    token,
                    "",
                    "",
                    null,
                    null,
                    null,
                    "",
                    SemanticGroupRole.NONE,
                    EditorialReviewStatus.PENDING
                ));
                inserted++;
            }
        }

        int removed = oldCount - preserved;
        return new EditorialWordReconciliationResult(
            reconciled,
            preserved,
            inserted,
            removed,
            invalidatedGroups.size()
        );
    }

    public static List<String> tokens(String text) {
        String value = text == null ? "" : text.trim();
        if (value.isEmpty()) return List.of();

        List<String> result = new ArrayList<>();
        Matcher matcher = TOKEN.matcher(value);
        while (matcher.find()) {
            String token = matcher.group().trim();
            if (!token.isEmpty()) result.add(token);
        }
        return List.copyOf(result);
    }

    public static String normalizedToken(String token) {
        if (token == null) return "";
        return token
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private static int[][] lcs(List<EditorialWord> oldWords, List<String> newTokens) {
        int[][] table = new int[oldWords.size() + 1][newTokens.size() + 1];

        for (int i = oldWords.size() - 1; i >= 0; i--) {
            for (int j = newTokens.size() - 1; j >= 0; j--) {
                if (sameToken(oldWords.get(i).approvedEn(), newTokens.get(j))) {
                    table[i][j] = 1 + table[i + 1][j + 1];
                } else {
                    table[i][j] = Math.max(table[i + 1][j], table[i][j + 1]);
                }
            }
        }
        return table;
    }

    private static boolean sameToken(String left, String right) {
        String normalizedLeft = normalizedToken(left);
        String normalizedRight = normalizedToken(right);
        return !normalizedLeft.isEmpty() && normalizedLeft.equals(normalizedRight);
    }

    private static Set<String> invalidatedGroups(
        List<EditorialWord> oldWords,
        int[] oldToNew
    ) {
        Map<String, List<Integer>> groups = new LinkedHashMap<>();
        for (int i = 0; i < oldWords.size(); i++) {
            String groupId = oldWords.get(i).semanticGroupId();
            if (!groupId.isEmpty()) {
                groups.computeIfAbsent(groupId, ignored -> new ArrayList<>()).add(i);
            }
        }

        Set<String> invalidated = new HashSet<>();
        for (Map.Entry<String, List<Integer>> entry : groups.entrySet()) {
            List<Integer> members = entry.getValue();
            boolean valid = true;
            Integer previousNewIndex = null;

            for (int oldIndex : members) {
                int mapped = oldToNew[oldIndex];
                if (mapped < 0) {
                    valid = false;
                    break;
                }
                if (previousNewIndex != null && mapped != previousNewIndex + 1) {
                    valid = false;
                    break;
                }
                previousNewIndex = mapped;
            }

            if (!valid) invalidated.add(entry.getKey());
        }
        return invalidated;
    }
}
