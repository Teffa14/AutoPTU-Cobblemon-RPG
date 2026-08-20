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
        assertTrue(lifecycle.contracts().contains("StatusStateStore"));
        assertTrue(lifecycle.contracts().contains("Flinch"));
        assertTrue(statuses.contracts().contains("StatusEntry/StatusStateStore"));
        assertTrue(statuses.contracts().contains("StatusApplicationHookRegistry"));
        assertTrue(statuses.adapterPolicy().contains("Status application/prevention remains core-owned"));
        assertTrue(statuses.adapterPolicy().contains("do not implement missing"));
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, lifecycle.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, statuses.support());
    }

    @Test
    void trainerRuntimeAndCombatStagesStayAuthoritativeWithoutPromotingWholePerkLibrary() {
        UpstreamCompatibilityMatrix.Entry calculations = UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.CORE_CALCULATIONS_AND_COMBAT_STATS);
        UpstreamCompatibilityMatrix.Entry lifecycle = UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE);
        UpstreamCompatibilityMatrix.Entry perks = UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS);
        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED, calculations.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, lifecycle.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, perks.support());
        assertTrue(calculations.contracts().contains("CombatStageState"));
        assertTrue(calculations.contracts().contains("CombatStageHookRegistry"));
        assertTrue(calculations.contracts().contains("CombatStageMutationService/CombatStageMutationResult"));
        assertTrue(calculations.contracts().contains("CombatStageMutationOptions"));
        assertTrue(calculations.contracts().contains("SpatialAbilityQuery"));
        assertTrue(calculations.contracts().contains("Simple"));
        assertTrue(calculations.contracts().contains("Defiant"));
        assertTrue(calculations.contracts().contains("Competitive"));
        assertTrue(calculations.contracts().contains("Plus [SwSh]"));
        assertTrue(calculations.contracts().contains("Minus [SwSh]"));
        assertTrue(calculations.adapterPolicy().contains("combat-stage reaction effects remain core-owned"));
        assertTrue(calculations.adapterPolicy().contains("returned starting/base/final stages"));
        assertTrue(calculations.adapterPolicy().contains("must not claim current stages"));
        assertTrue(calculations.adapterPolicy().contains("recursive suppression state"));
        assertTrue(calculations.adapterPolicy().contains("apply stage deltas in Minecraft"));
        assertTrue(lifecycle.contracts().contains("TrainerRuntimeState/controller binding"));
        assertTrue(lifecycle.contracts().contains("fixed Link Feature"));
        assertTrue(perks.contracts().contains("TrainerRuntimeState"));
        assertTrue(perks.contracts().contains("CombatStageState"));
        assertTrue(perks.contracts().contains("CombatStageHookRegistry"));
        assertTrue(perks.contracts().contains("Attack/Defense/Special Attack/Special Defense/Speed Link"));
        assertTrue(perks.contracts().contains("direct mutation path"));
        assertTrue(perks.adapterPolicy().contains("battle-start AP"));
        assertTrue(perks.adapterPolicy().contains("may not grant Features"));
        assertTrue(perks.adapterPolicy().contains("spend/restore AP"));
        assertTrue(perks.adapterPolicy().contains("mutate combat stages"));
        assertTrue(perks.adapterPolicy().contains("run combat-stage reactions"));
    }

    @Test
    void postDamageAuraRuntimeBindingKeepsDamageAndAbilitiesPartial() {
        UpstreamCompatibilityMatrix.Entry abilities = UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.ABILITIES);
        UpstreamCompatibilityMatrix.Entry damage = UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE);
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, abilities.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, damage.support());
        assertTrue(abilities.contracts().contains("AbilityPhaseEffectRegistry"));
        assertTrue(abilities.contracts().contains("CombatStageHookRegistry"));
        assertTrue(abilities.contracts().contains("PostDamageHookRegistry"));
        assertTrue(abilities.contracts().contains("Inner Focus"));
        assertTrue(abilities.contracts().contains("Lancer"));
        assertTrue(abilities.contracts().contains("Simple"));
        assertTrue(abilities.contracts().contains("Defiant"));
        assertTrue(abilities.contracts().contains("Competitive"));
        assertTrue(abilities.contracts().contains("Plus [SwSh]"));
        assertTrue(abilities.contracts().contains("Minus [SwSh]"));
        assertTrue(abilities.contracts().contains("Aqua Boost"));
        assertTrue(abilities.contracts().contains("Ignition Boost"));
        assertTrue(abilities.contracts().contains("Thunder Boost"));
        assertTrue(abilities.contracts().contains("Power Spot"));
        assertTrue(abilities.contracts().contains("Type Aura"));
        assertTrue(abilities.contracts().contains("authoritative positions"));
        assertTrue(abilities.adapterPolicy().contains("radius/adjacency holder selection"));
        assertTrue(abilities.adapterPolicy().contains("damage-bonus source selection"));
        assertTrue(abilities.adapterPolicy().contains("remaining ability library"));
        assertTrue(damage.contracts().contains("PostDamageHookRegistry/PostDamageHookResult"));
        assertTrue(damage.contracts().contains("before authoritative HP mutation"));
        assertTrue(damage.contracts().contains("MoveResolvedEvent"));
        assertTrue(damage.contracts().contains("Power Spot"));
        assertTrue(damage.contracts().contains("Type Aura"));
        assertTrue(damage.adapterPolicy().contains("MoveResolvedEvent damage/targetHp"));
        assertTrue(damage.adapterPolicy().contains("must never select aura sources"));
        assertTrue(damage.adapterPolicy().contains("independent HP mutation"));
        assertTrue(abilities.adapterPolicy().contains("Aura-adjusted damage/HP may be mirrored"));
        assertTrue(abilities.adapterPolicy().contains("do not apply aura bonuses"));
    }

    @Test
    void moveContractRequiresTrustedCatalogMetadata() {
        UpstreamCompatibilityMatrix.Entry moves = UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR);
        assertTrue(moves.contracts().contains("MoveOption/MoveSpec/MoveCombatProfile"));
        assertTrue(moves.contracts().contains("MoveSpec keyword"));
        assertTrue(moves.contracts().contains("move_has_keyword"));
        assertTrue(moves.adapterPolicy().contains("trusted server-owned catalog"));
        assertTrue(moves.adapterPolicy().contains("keyword identities"));
        assertTrue(moves.adapterPolicy().contains("infer from text"));
        assertTrue(moves.adapterPolicy().contains("push, pull, crash or contact"));
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
        assertEquals("11748b3c77f86ea96f78a357aaa92370e3478a58", UpstreamCompatibilityMatrix.AUTOPTU_JAVA_SHA);
        assertEquals("e4bb0ca38b7018710af476ce365d515a387de4e7", UpstreamCompatibilityMatrix.AUTOPTU_PYTHON_SHA);
    }
}
