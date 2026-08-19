package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeldItemFeatureCompatibilityTest {
    @Test
    void canonicalHeldItemBootstrapDependsOnlyOnPartialItemContract() {
        IntegrationFeatureCompatibility.Requirement requirement = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.CANONICAL_HELD_ITEM_BOOTSTRAP);

        assertEquals(EnumSet.of(UpstreamCompatibilityMatrix.Capability.ITEMS),
                EnumSet.copyOf(requirement.capabilities()));
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.ITEMS).support());
        assertTrue(requirement.boundedScope().contains("heldItemsByCombatant"));
        assertTrue(requirement.boundedScope().contains("fail closed"));
        assertTrue(requirement.boundedScope().contains("item effects remain core-owned"));
        assertFalse(requirement.hasBlockingDependency());
    }
}
