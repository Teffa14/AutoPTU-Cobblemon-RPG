package io.autoptu.cobblemon.authority;

import java.util.Objects;

/** Durable server-owned result for establishing one physical Ouros field camp. */
public record FieldCampSetupAttempt(
        String attemptId,
        String campId,
        String establishedByPlayerId,
        String taskId,
        Phase phase,
        int canonicalSkillRank,
        int improvisedPercent,
        int standardPercent,
        int excellentPercent,
        int rollPercent,
        Quality quality
) {
    public FieldCampSetupAttempt {
        attemptId = requireText(attemptId, "attemptId");
        campId = requireText(campId, "campId");
        establishedByPlayerId = requireText(establishedByPlayerId, "establishedByPlayerId");
        taskId = requireText(taskId, "taskId");
        phase = Objects.requireNonNull(phase, "phase");
        if (canonicalSkillRank < 0) throw new IllegalArgumentException("canonicalSkillRank must be >= 0");
        if (improvisedPercent < 0 || standardPercent < 0 || excellentPercent < 0
                || improvisedPercent + standardPercent + excellentPercent != 100) {
            throw new IllegalArgumentException("quality percentages must be non-negative and sum to 100");
        }
        if (phase == Phase.PLANNED) {
            if (rollPercent != 0 || quality != null) {
                throw new IllegalArgumentException("planned camp setup must not contain an outcome");
            }
        } else {
            if (rollPercent < 1 || rollPercent > 100 || quality == null) {
                throw new IllegalArgumentException("committed camp setup requires a 1..100 roll and quality");
            }
        }
    }

    public static FieldCampSetupAttempt planned(
            String attemptId,
            String campId,
            String playerId,
            String taskId,
            int skillRank,
            WorldTaskDefinition.QualityDistribution distribution
    ) {
        Objects.requireNonNull(distribution, "distribution");
        return new FieldCampSetupAttempt(
                attemptId,
                campId,
                playerId,
                taskId,
                Phase.PLANNED,
                skillRank,
                distribution.improvisedPercent(),
                distribution.standardPercent(),
                distribution.excellentPercent(),
                0,
                null
        );
    }

    public FieldCampSetupAttempt committed(int rollPercent, Quality quality) {
        if (phase != Phase.PLANNED) throw new IllegalStateException("only planned camp setup can commit");
        return new FieldCampSetupAttempt(
                attemptId,
                campId,
                establishedByPlayerId,
                taskId,
                Phase.COMMITTED,
                canonicalSkillRank,
                improvisedPercent,
                standardPercent,
                excellentPercent,
                rollPercent,
                quality
        );
    }

    public WorldTaskDefinition.QualityDistribution frozenDistribution() {
        return new WorldTaskDefinition.QualityDistribution(
                improvisedPercent,
                standardPercent,
                excellentPercent
        );
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }

    public enum Phase {
        PLANNED,
        COMMITTED
    }

    public enum Quality {
        IMPROVISED,
        STANDARD,
        EXCELLENT
    }
}
