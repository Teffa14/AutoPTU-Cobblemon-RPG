package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleRuntimePreparationCompatibilityTest {
    @Test
    void preparationEnvelopeConsumesOnlyNonBlockingUpstreamCategories() {
        IntegrationFeatureCompatibility.Requirement requirement = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.RUNTIME_BATTLE_PREPARATION_ENVELOPE);

        assertEquals(EnumSet.of(
                        UpstreamCompatibilityMatrix.Capability.CORE_TARGETING,
                        UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY,
                        UpstreamCompatibilityMatrix.Capability.CORE_CALCULATIONS_AND_COMBAT_STATS,
                        UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR,
                        UpstreamCompatibilityMatrix.Capability.ABILITIES,
                        UpstreamCompatibilityMatrix.Capability.ITEMS,
                        UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE,
                        UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE),
                EnumSet.copyOf(requirement.capabilities()));
        assertFalse(requirement.hasBlockingDependency());
        assertTrue(requirement.boundedScope().contains("StatusStateStore"));
        assertTrue(requirement.boundedScope().contains("status expiry"));
        assertTrue(requirement.boundedScope().contains("MovementProfile"));
        assertTrue(requirement.boundedScope().contains("ActionBudget"));
        assertTrue(requirement.boundedScope().contains("move effects"));
        assertTrue(requirement.boundedScope().contains("item effects"));
    }
}
