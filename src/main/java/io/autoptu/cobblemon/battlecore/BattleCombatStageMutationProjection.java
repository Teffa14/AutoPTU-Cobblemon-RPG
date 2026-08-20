package io.autoptu.cobblemon.battlecore;

import java.util.Objects;

/**
 * Adapter-neutral projection of one already-resolved AutoPTU-Java combat-stage mutation.
 *
 * This type carries the result of the authoritative core mutation boundary. It never
 * computes or applies a PTU combat-stage change. Minecraft/Cobblemon may use it to
 * update presentation state after the core has returned the canonical result.
 */
public record BattleCombatStageMutationProjection(
        String targetId,
        Stat stat,
        int startingStage,
        int requestedDelta,
        int baseAppliedDelta,
        int baseStage,
        int finalStage
) {
    public enum Stat { ATK, DEF, SPATK, SPDEF, SPD }

    public BattleCombatStageMutationProjection {
        if (targetId == null || targetId.isBlank()) {
            throw new IllegalArgumentException("targetId is required");
        }
        targetId = targetId.strip();
        stat = Objects.requireNonNull(stat, "stat");
        requireStage(startingStage, "startingStage");
        requireStage(baseStage, "baseStage");
        requireStage(finalStage, "finalStage");
        if (baseAppliedDelta != baseStage - startingStage) {
            throw new IllegalArgumentException("baseAppliedDelta must match the authoritative base-stage transition");
        }
        if (requestedDelta > 0 && (baseAppliedDelta < 0 || baseAppliedDelta > requestedDelta)) {
            throw new IllegalArgumentException("positive requestedDelta cannot yield an incompatible baseAppliedDelta");
        }
        if (requestedDelta < 0 && (baseAppliedDelta > 0 || baseAppliedDelta < requestedDelta)) {
            throw new IllegalArgumentException("negative requestedDelta cannot yield an incompatible baseAppliedDelta");
        }
        if (requestedDelta == 0 && baseAppliedDelta != 0) {
            throw new IllegalArgumentException("zero requestedDelta cannot change the base stage");
        }
    }

    public boolean reactionChangedStage() {
        return finalStage != baseStage;
    }

    private static void requireStage(int value, String field) {
        if (value < -6 || value > 6) {
            throw new IllegalArgumentException(field + " must be within PTU combat-stage bounds -6..6");
        }
    }
}
