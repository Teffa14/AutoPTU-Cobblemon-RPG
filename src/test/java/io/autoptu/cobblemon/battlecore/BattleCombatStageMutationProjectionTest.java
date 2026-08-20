package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleCombatStageMutationProjectionTest {
    @Test
    void carriesAuthoritativeBaseMutationWithoutApplyingRules() {
        BattleCombatStageMutationProjection projection = new BattleCombatStageMutationProjection(
                "target",
                BattleCombatStageMutationProjection.Stat.ATK,
                0,
                1,
                1,
                1,
                1
        );

        assertEquals("target", projection.targetId());
        assertEquals(1, projection.baseAppliedDelta());
        assertEquals(1, projection.finalStage());
        assertFalse(projection.reactionChangedStage());
    }

    @Test
    void preservesPostHookFinalStageWithoutRecomputingReaction() {
        BattleCombatStageMutationProjection projection = new BattleCombatStageMutationProjection(
                "target",
                BattleCombatStageMutationProjection.Stat.DEF,
                0,
                -1,
                -1,
                -1,
                -2
        );

        assertEquals(-1, projection.baseStage());
        assertEquals(-2, projection.finalStage());
        assertTrue(projection.reactionChangedStage());
    }

    @Test
    void acceptsAuthoritativeClampResult() {
        BattleCombatStageMutationProjection projection = new BattleCombatStageMutationProjection(
                "target",
                BattleCombatStageMutationProjection.Stat.SPD,
                5,
                3,
                1,
                6,
                6
        );

        assertEquals(1, projection.baseAppliedDelta());
        assertFalse(projection.reactionChangedStage());
    }

    @Test
    void rejectsForgedBaseAppliedDelta() {
        assertThrows(IllegalArgumentException.class, () -> new BattleCombatStageMutationProjection(
                "target",
                BattleCombatStageMutationProjection.Stat.SPATK,
                0,
                2,
                2,
                1,
                1
        ));
    }

    @Test
    void rejectsStagesOutsidePtuBounds() {
        assertThrows(IllegalArgumentException.class, () -> new BattleCombatStageMutationProjection(
                "target",
                BattleCombatStageMutationProjection.Stat.SPDEF,
                7,
                0,
                0,
                7,
                7
        ));
    }

    @Test
    void rejectsAppliedDeltaThatOpposesRequestedDirection() {
        assertThrows(IllegalArgumentException.class, () -> new BattleCombatStageMutationProjection(
                "target",
                BattleCombatStageMutationProjection.Stat.ATK,
                0,
                2,
                -1,
                -1,
                -1
        ));
    }
}
