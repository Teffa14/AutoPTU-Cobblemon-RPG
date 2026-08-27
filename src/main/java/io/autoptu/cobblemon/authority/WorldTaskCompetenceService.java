package io.autoptu.cobblemon.authority;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Resolves a server-authored Minecraft/Ouros world task against persistent canonical Trainer state.
 * No client-provided rank, modifier, probability or outcome is accepted.
 */
public final class WorldTaskCompetenceService {

    public Assessment assess(CanonicalPlayerState player, WorldTaskDefinition task) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(task, "task");

        int rank = canonicalSkillRank(player.skillRanks(), task.canonicalSkillId());
        boolean understood = rank >= task.minimumKnowledgeRank();
        WorldTaskDefinition.QualityDistribution distribution = task.distributionForRank(rank);
        String detail = understood
                ? "Task knowledge requirement satisfied by canonical Trainer state."
                : "Requires " + task.canonicalSkillId() + " rank " + task.minimumKnowledgeRank()
                        + "; canonical rank is " + rank + ".";
        return new Assessment(
                task.taskId(),
                task.displayName(),
                task.canonicalSkillId(),
                rank,
                task.minimumKnowledgeRank(),
                understood,
                distribution,
                detail
        );
    }

    static int canonicalSkillRank(Map<String, Integer> skillRanks, String requiredSkillId) {
        Objects.requireNonNull(skillRanks, "skillRanks");
        Objects.requireNonNull(requiredSkillId, "requiredSkillId");

        String normalizedRequired = normalizeSkillId(requiredSkillId);
        int resolved = 0;
        boolean found = false;
        for (Map.Entry<String, Integer> entry : skillRanks.entrySet()) {
            if (normalizeSkillId(entry.getKey()).equals(normalizedRequired)) {
                if (found && resolved != entry.getValue()) {
                    throw new IllegalStateException(
                            "Ambiguous canonical skill aliases for " + requiredSkillId);
                }
                found = true;
                resolved = entry.getValue();
            }
        }
        return found ? resolved : 0;
    }

    private static String normalizeSkillId(String value) {
        String lower = value.trim().toLowerCase(Locale.ROOT);
        StringBuilder normalized = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                normalized.append(c);
            }
        }
        return normalized.toString();
    }

    public record Assessment(
            String taskId,
            String displayName,
            String canonicalSkillId,
            int canonicalSkillRank,
            int minimumKnowledgeRank,
            boolean understood,
            WorldTaskDefinition.QualityDistribution distribution,
            String detail
    ) {
        public Assessment {
            Objects.requireNonNull(taskId, "taskId");
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(canonicalSkillId, "canonicalSkillId");
            Objects.requireNonNull(distribution, "distribution");
            Objects.requireNonNull(detail, "detail");
        }
    }
}
