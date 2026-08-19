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
        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED, UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.CORE_CALCULATIONS_AND_COMBAT_STATS).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.ABILITIES).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.ITEMS).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).support());
    }

    @Test
    void structuredStatusRuntimeBindingDoesNotPromoteWholeLifecycleOrStatusLibrary() {
        UpstreamCompatibilityMatrix.Entry lifecycle = UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE);
        UpstreamCompatibilityMatrix.Entry statuses = UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE);
        assertTrue(lifecycle.contracts().contains("bound into BattleRuntimeState"));
        assertTrue(lifecycle.contracts().contains("Flinch round-boundary expiry"));
        assertTrue(statuses.contracts().contains("StatusEntry/StatusStateStore"));
        assertTrue(statuses.contracts().contains("StatusApplicationHookRegistry"));
        assertTrue(statuses.contracts().contains("Flinch round-boundary expiry"));
        assertTrue(statuses.adapterPolicy().contains("Status application/prevention remains core-owned"));
        assertTrue(statuses.adapterPolicy().contains("do not implement missing"));
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, lifecycle.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, statuses.support());
    }

    @Test
    void abilityPhaseRegistryUsesGenericPlaybackWithoutPromotingAbilityLibrary() {
        UpstreamCompatibilityMatrix.Entry abilities = UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.ABILITIES);
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, abilities.support());
        assertTrue(abilities.contracts().contains("AbilityPhaseEffectRegistry"));
        assertTrue(abilities.contracts().contains("Inner Focus"));
        assertTrue(abilities.contracts().contains("Lancer END"));
        assertTrue(abilities.adapterPolicy().contains("Generic rule-effect playback"));
        assertTrue(abilities.adapterPolicy().contains("do not implement the remaining ability library"));
    }

    @Test
    void moveContractRequiresTrustedCatalogMetadata() {
        UpstreamCompatibilityMatrix.Entry moves = UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR);
        assertTrue(moves.contracts().contains("MoveOption/MoveSpec/MoveCombatProfile"));
        assertTrue(moves.adapterPolicy().contains("trusted server-owned catalog"));
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, moves.support());
    }

    @Test
    void calculationContractKeepsDynamicEvasionCoreOwned() {
        UpstreamCompatibilityMatrix.Entry calculations = UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.CORE_CALCULATIONS_AND_COMBAT_STATS);
        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED, calculations.support());
        assertTrue(calculations.contracts().contains("EvasionProfile"));
        assertTrue(calculations.adapterPolicy().contains("Trainer Feature"));
        assertTrue(calculations.adapterPolicy().contains("terrain"));
    }

    @Test
    void movementContractKeepsRuntimeModifiersCoreOwned() {
        UpstreamCompatibilityMatrix.Entry movement = UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY);
        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED, movement.support());
        assertTrue(movement.contracts().contains("resolved MovementProfile"));
        assertTrue(movement.adapterPolicy().contains("Wallrunner"));
        assertTrue(movement.adapterPolicy().contains("weather"));
        assertTrue(movement.adapterPolicy().contains("Trainer Features"));
    }

    @Test
    void itemContractCarriesIdentityWithoutPromotingItemLibrary() {
        UpstreamCompatibilityMatrix.Entry items = UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.ITEMS);
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, items.support());
        assertTrue(items.contracts().contains("HeldItemState"));
        assertTrue(items.contracts().contains("heldItemsByCombatant"));
        assertTrue(items.adapterPolicy().contains("item effects"));
        assertTrue(items.adapterPolicy().contains("client/entity equipment"));
    }

    @Test
    void matrixPinsTheUpstreamsThatWereActuallyInspected() {
        assertEquals("b3dee6e1e7aa954173fc152b2dd4bf5b960e54cd", UpstreamCompatibilityMatrix.AUTOPTU_JAVA_SHA);
        assertEquals("e4bb0ca38b7018710af476ce365d515a387de4e7", UpstreamCompatibilityMatrix.AUTOPTU_PYTHON_SHA);
    }
}
