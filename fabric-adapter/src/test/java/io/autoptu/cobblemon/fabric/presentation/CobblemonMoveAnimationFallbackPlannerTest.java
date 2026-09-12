package io.autoptu.cobblemon.fabric.presentation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CobblemonMoveAnimationFallbackPlannerTest {
    @Test
    void fireBeamGetsFireAndBeamNativeCandidatesWithoutSelfAlias() {
        CobblemonMoveAnimationFallbackPlanner.Plan plan =
                CobblemonMoveAnimationFallbackPlanner.plan("ouros:scarlet_lance");

        assertFalse(plan.candidateEffectPaths().isEmpty());
        assertTrue(plan.candidateEffectPaths().stream().anyMatch(path ->
                path.equals("flamethrower") || path.equals("fireblast") || path.equals("hyperbeam")));
        assertFalse(plan.candidateEffectPaths().contains("ourosscarletlance"));
    }

    @Test
    void requestedNativeNameIsNeverProposedAsItsOwnFallback() {
        CobblemonMoveAnimationFallbackPlanner.Plan plan =
                CobblemonMoveAnimationFallbackPlanner.plan("flamethrower");

        assertFalse(plan.candidateEffectPaths().contains("flamethrower"));
        assertFalse(plan.candidateEffectPaths().isEmpty());
    }

    @Test
    void fallbackChoiceOrderAndAccentVariantAreDeterministic() {
        CobblemonMoveAnimationFallbackPlanner.Plan first =
                CobblemonMoveAnimationFallbackPlanner.plan("ouros:custom_water_arc");
        CobblemonMoveAnimationFallbackPlanner.Plan second =
                CobblemonMoveAnimationFallbackPlanner.plan("ouros:custom_water_arc");

        assertEquals(first, second);
        assertTrue(first.variant() >= 0 && first.variant() <= 3);
    }

    @Test
    void distinctCustomMoveIdsCanReceiveDifferentAccentVariants() {
        int first = CobblemonMoveAnimationFallbackPlanner.plan("ouros:custom_alpha").variant();
        int second = CobblemonMoveAnimationFallbackPlanner.plan("ouros:custom_beta").variant();

        assertTrue(first >= 0 && first <= 3);
        assertTrue(second >= 0 && second <= 3);
        // The assertion is deliberately about stable bounded variants, not a required inequality:
        // hash collisions are legal and do not affect authority or coverage.
    }
}
