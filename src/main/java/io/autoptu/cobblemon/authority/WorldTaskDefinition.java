package io.autoptu.cobblemon.authority;

import java.util.Collections;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Server-authored Ouros world task definition.
 *
 * <p>The referenced skill id is canonical Trainer state input. The quality curve is world-content
 * policy, not a PTU skill-check formula.</p>
 */
public record WorldTaskDefinition(
        String taskId,
        String displayName,
        String canonicalSkillId,
        int minimumKnowledgeRank,
        NavigableMap<Integer, QualityDistribution> qualityByMinimumRank
) {
    public WorldTaskDefinition {
        taskId = requireText(taskId, "taskId");
        displayName = requireText(displayName, "displayName");
        canonicalSkillId = requireText(canonicalSkillId, "canonicalSkillId");
        if (minimumKnowledgeRank < 0) {
            throw new IllegalArgumentException("minimumKnowledgeRank must be >= 0");
        }
        Objects.requireNonNull(qualityByMinimumRank, "qualityByMinimumRank");
        if (qualityByMinimumRank.isEmpty()) {
            throw new IllegalArgumentException("qualityByMinimumRank must not be empty");
        }
        TreeMap<Integer, QualityDistribution> copy = new TreeMap<>();
        qualityByMinimumRank.forEach((rank, distribution) -> {
            if (rank == null || rank < 0) {
                throw new IllegalArgumentException("quality rank thresholds must be >= 0");
            }
            copy.put(rank, Objects.requireNonNull(distribution, "quality distribution"));
        });
        if (copy.firstKey() != 0) {
            throw new IllegalArgumentException("qualityByMinimumRank must define a rank 0 baseline");
        }
        qualityByMinimumRank = Collections.unmodifiableNavigableMap(copy);
    }

    public QualityDistribution distributionForRank(int rank) {
        if (rank < 0) {
            throw new IllegalArgumentException("rank must be >= 0");
        }
        return qualityByMinimumRank.floorEntry(rank).getValue();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    public record QualityDistribution(int improvisedPercent, int standardPercent, int excellentPercent) {
        public QualityDistribution {
            if (improvisedPercent < 0 || standardPercent < 0 || excellentPercent < 0) {
                throw new IllegalArgumentException("quality percentages must be >= 0");
            }
            if (improvisedPercent + standardPercent + excellentPercent != 100) {
                throw new IllegalArgumentException("quality percentages must sum to 100");
            }
        }
    }
}
