package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpstreamCompatibilityMatrixTest {
    @Test
    void coversEveryPermanentCapabilityCategory() {
        assertEquals(EnumSet.allOf(UpstreamCompatibilityMatrix.Capability.class), EnumSet.copyOf(UpstreamCompatibilityMatrix.entries().keySet()));
    }

    @Test
    void unsupportedMechanicsStayBlockedAtAdapterBoundary() {
        assertFalse(UpstreamCompatibilityMatrix.mayProjectAuthoritativeBehavior(UpstreamCompatibilityMatrix.Capability.COMPLETE_MOVEMENT_BEHAVIOR));
        assertFalse(UpstreamCompatibilityMatrix.mayProjectAuthoritativeBehavior(UpstreamCompatibilityMatrix.Capability.AI_TACTICAL_SCORING_POLICY));
        assertFalse(UpstreamCompatibilityMatrix.mayProjectAuthoritativeBehavior(UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK));
    }

    @Test
    void verifiedAndPartialContractsCanBeConsumedWithoutExpandingTheirScope() {
        assertTrue(UpstreamCompatibilityMatrix.mayProjectAuthoritativeBehavior(UpstreamCompatibilityMatrix.Capability.CORE_TARGETING));
        assertTrue(UpstreamCompatibilityMatrix.mayProjectAuthoritativeBehavior(UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY));
        assertTrue(UpstreamCompatibilityMatrix.mayProjectAuthoritativeBehavior(UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE));
        assertTrue(UpstreamCompatibilityMatrix.mayProjectAuthoritativeBehavior(UpstreamCompatibilityMatrix.Capability.ABILITIES));
        assertTrue(UpstreamCompatibilityMatrix.mayProjectAuthoritativeBehavior(UpstreamCompatibilityMatrix.Capability.ITEMS));
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.ABILITIES).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.ITEMS).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).support());
    }

    @Test
    void delayedHitBindingAndRoundHistoryDoNotPromoteLifecycleToVerified() {
        assertTrue(UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).contracts().contains("DelayedHitBinding"));
        assertTrue(UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR).contracts().contains("DelayedHitBinding"));
        assertTrue(UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).contracts().contains("damage-history"));
        assertTrue(UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).contracts().contains("injury-history"));
        assertTrue(UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).contracts().contains("turn-end"));
        assertTrue(UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).adapterPolicy().contains("actual delayed-hit execution"));
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR).support());
    }

    @Test
    void movementContractKeepsRuntimeModifiersCoreOwned() {
        UpstreamCompatibilityMatrix.Entry movement = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY);
        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED, movement.support());
        assertTrue(movement.contracts().contains("resolved MovementProfile"));
        assertTrue(movement.adapterPolicy().contains("Wallrunner"));
        assertTrue(movement.adapterPolicy().contains("weather"));
        assertTrue(movement.adapterPolicy().contains("Trainer Features"));
    }

    @Test
    void matrixPinsTheUpstreamsThatWereActuallyInspected() {
        assertEquals("de0ab9f6224c76dc232071881f4a88435262d7e1", UpstreamCompatibilityMatrix.AUTOPTU_JAVA_SHA);
        assertEquals("54e4fa8ccbe0e555afef8b4b3713e7568608e5d3", UpstreamCompatibilityMatrix.AUTOPTU_PYTHON_SHA);
    }
}
