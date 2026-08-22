package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpstreamCompatibilityMatrixTest {
    @Test
    void coversEveryPermanentCapabilityCategory() {
        assertEquals(EnumSet.allOf(UpstreamCompatibilityMatrix.Capability.class),
                EnumSet.copyOf(UpstreamCompatibilityMatrix.entries().keySet()));
    }

    @Test
    void unsupportedMechanicsStayBlockedWhileRuntimeTestedAdapterIsOnlyPartial() {
        assertFalse(UpstreamCompatibilityMatrix.mayProjectAuthoritativeBehavior(
                UpstreamCompatibilityMatrix.Capability.COMPLETE_MOVEMENT_BEHAVIOR));
        assertFalse(UpstreamCompatibilityMatrix.mayProjectAuthoritativeBehavior(
                UpstreamCompatibilityMatrix.Capability.AI_TACTICAL_SCORING_POLICY));

        UpstreamCompatibilityMatrix.Entry adapter = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK);
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, adapter.support());
        assertTrue(UpstreamCompatibilityMatrix.mayProjectAuthoritativeBehavior(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK));
        assertTrue(adapter.contracts().contains("dedicated-server"));
        assertTrue(adapter.contracts().contains("PokemonEntity"));
        assertTrue(adapter.contracts().contains("authoritative relocation"));
        assertTrue(adapter.contracts().contains("canonical identity mapping"));
        assertTrue(adapter.contracts().contains("FabricAuthenticatedPlayerContextResolver"));
        assertTrue(adapter.contracts().contains("MinecraftServer PlayerManager"));
        assertTrue(adapter.contracts().contains("CanonicalPlayerMutationService"));
        assertTrue(adapter.contracts().contains("VersionedCanonicalStateRepository"));
        assertTrue(adapter.contracts().contains("FileVersionedCanonicalStateRepository"));
        assertTrue(adapter.contracts().contains("required atomic replacement"));
        assertTrue(adapter.contracts().contains("FabricCanonicalPlayerStoreRuntime"));
        assertTrue(adapter.contracts().contains("world save root"));
        assertTrue(adapter.contracts().contains("CanonicalPlayerEncounterProfile"));
        assertTrue(adapter.contracts().contains("FileCanonicalPlayerEncounterProfileRepository"));
        assertTrue(adapter.contracts().contains("PersistentCanonicalPlayerEncounterContextSource"));
        assertTrue(adapter.contracts().contains("two-process dedicated-server restart smoke"));
        assertTrue(adapter.contracts().contains("FileCanonicalItemReservationRepository"));
        assertTrue(adapter.contracts().contains("active reservation"));
        assertTrue(adapter.adapterPolicy().contains("successful logged-in graphical player encounter is still pending"));
        assertTrue(adapter.adapterPolicy().contains("Durable Pokemon aggregate persistence"));
        assertTrue(adapter.adapterPolicy().contains("wiring the item ledger into Fabric world lifecycle"));
        assertTrue(adapter.adapterPolicy().contains("cross-aggregate transactions"));
        assertTrue(adapter.adapterPolicy().contains("BattleAuthorityService must continue to re-resolve ownership and quantities"));
        assertTrue(adapter.adapterPolicy().contains("client replacement aggregates"));
    }

    @Test
    void verifiedAndPartialContractsCanBeConsumedWithoutExpandingTheirScope() {
        assertTrue(UpstreamCompatibilityMatrix.mayProjectAuthoritativeBehavior(UpstreamCompatibilityMatrix.Capability.CORE_TARGETING));
        assertTrue(UpstreamCompatibilityMatrix.mayProjectAuthoritativeBehavior(UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY));
        assertTrue(UpstreamCompatibilityMatrix.mayProjectAuthoritativeBehavior(UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE));
        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED,
                UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.CORE_CALCULATIONS_AND_COMBAT_STATS).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.ABILITIES).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.ITEMS).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).support());
    }

    @Test
    void structuredStatusRuntimeBindingDoesNotPromoteWholeLifecycleOrStatusLibrary() {
        UpstreamCompatibilityMatrix.Entry lifecycle = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE);
        UpstreamCompatibilityMatrix.Entry statuses = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE);
        assertTrue(lifecycle.contracts().contains("StatusStateStore"));
        assertTrue(lifecycle.contracts().contains("Flinch"));
        assertTrue(statuses.contracts().contains("StatusEntry/StatusStateStore"));
        assertTrue(statuses.contracts().contains("StatusApplicationHookRegistry"));
        assertTrue(statuses.adapterPolicy().contains("Status application/prevention"));
        assertTrue(statuses.adapterPolicy().contains("do not implement missing"));
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, lifecycle.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, statuses.support());
    }

    @Test
    void trainerRuntimeAndCombatStagesStayAuthoritativeWithoutPromotingWholePerkLibrary() {
        UpstreamCompatibilityMatrix.Entry calculations = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.CORE_CALCULATIONS_AND_COMBAT_STATS);
        UpstreamCompatibilityMatrix.Entry lifecycle = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE);
        UpstreamCompatibilityMatrix.Entry perks = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS);

        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED, calculations.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, lifecycle.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, perks.support());
        assertTrue(calculations.contracts().contains("CombatStageState"));
        assertTrue(calculations.contracts().contains("CombatStageHookRegistry"));
        assertTrue(calculations.contracts().contains("CombatStageMutationService/CombatStageMutationResult"));
        assertTrue(calculations.contracts().contains("CombatStageMutationOptions"));
        assertTrue(calculations.contracts().contains("SpatialAbilityQuery"));
        assertTrue(calculations.adapterPolicy().contains("combat-stage reaction effects remain core-owned"));
        assertTrue(lifecycle.contracts().contains("TrainerRuntimeState/controller binding"));
        assertTrue(perks.contracts().contains("TrainerRuntimeState"));
        assertTrue(perks.contracts().contains("initiativeModifier"));
        assertTrue(perks.contracts().contains("TrainerFeaturePrerequisiteResolution"));
        assertTrue(perks.contracts().contains("TrainerFeatureFrequencyResolution"));
        assertTrue(perks.contracts().contains("TrainerFeatureResourceResolution"));
        assertTrue(perks.contracts().contains("TrainerFeatureUsageResolution"));
        assertTrue(perks.contracts().contains("TrainerFeatureExecutionService.executeAuthoritative"));
        assertTrue(perks.contracts().contains("TrainerFeatureTargetResolution"));
        assertTrue(perks.contracts().contains("TrainerFeatureTrainerTargetResolution"));
        assertTrue(perks.contracts().contains("TrainerFeatureEffectRegistry"));
        assertTrue(perks.contracts().contains("heal/heal_active"));
        assertTrue(perks.contracts().contains("raise_cs"));
        assertTrue(perks.contracts().contains("grant_temp_hp"));
        assertTrue(perks.contracts().contains("grant_ap"));
        assertTrue(perks.contracts().contains("only after an applied effect"));
        assertTrue(perks.adapterPolicy().contains("battle-start AP"));
        assertTrue(perks.adapterPolicy().contains("may not grant Features"));
        assertTrue(perks.adapterPolicy().contains("select or rewrite targets"));
        assertTrue(perks.adapterPolicy().contains("wider Python effect library"));
        assertTrue(perks.adapterPolicy().contains("Temporary HP damage absorption"));
    }

    @Test
    void postDamageAuraRuntimeBindingKeepsDamageAndAbilitiesPartial() {
        UpstreamCompatibilityMatrix.Entry abilities = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.ABILITIES);
        UpstreamCompatibilityMatrix.Entry damage = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE);
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, abilities.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, damage.support());
        assertTrue(abilities.contracts().contains("AbilityPhaseEffectRegistry"));
        assertTrue(abilities.contracts().contains("CombatStageHookRegistry"));
        assertTrue(abilities.contracts().contains("PostDamageHookRegistry"));
        assertTrue(damage.contracts().contains("PostDamageHookRegistry/PostDamageHookResult"));
        assertTrue(damage.contracts().contains("RuntimeMoveResolution"));
        assertTrue(damage.adapterPolicy().contains("independent HP mutation"));
        assertTrue(abilities.adapterPolicy().contains("independently alter damage/HP"));
    }

    @Test
    void initiativeRolloverRemainsCoreOwnedWithoutPromotingFullLifecycle() {
        UpstreamCompatibilityMatrix.Entry initiative = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE);
        UpstreamCompatibilityMatrix.Entry lifecycle = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE);
        UpstreamCompatibilityMatrix.Entry legalActions = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.AI_LEGAL_ACTION_INFRASTRUCTURE);

        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED, initiative.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, lifecycle.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED, legalActions.support());
        assertTrue(initiative.contracts().contains("authoritative initiative assembly/installation"));
        assertTrue(initiative.adapterPolicy().contains("choose the next actor"));
        assertTrue(lifecycle.contracts().contains("InitiativeOrderAssembly/InitiativeAssemblyInstaller"));
        assertTrue(lifecycle.adapterPolicy().contains("complete Python start_round parity is still absent"));
        assertTrue(legalActions.contracts().contains("initiative exhaustion"));
        assertTrue(legalActions.adapterPolicy().contains("must not supply initiative order"));
    }

    @Test
    void lifecycleOwnsCanonicalRoundWithoutAdapterInput() {
        UpstreamCompatibilityMatrix.Entry lifecycle = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE);
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, lifecycle.support());
        assertTrue(lifecycle.contracts().contains("BattleRuntimeState.currentRound"));
        assertTrue(lifecycle.adapterPolicy().contains("must never advance currentRound"));
    }

    @Test
    void moveContractRequiresTrustedCatalogMetadata() {
        UpstreamCompatibilityMatrix.Entry moves = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR);
        assertTrue(moves.contracts().contains("MoveOption/MoveSpec/MoveCombatProfile"));
        assertTrue(moves.contracts().contains("MoveSpec keyword"));
        assertTrue(moves.contracts().contains("move_has_keyword"));
        assertTrue(moves.adapterPolicy().contains("trusted server-owned catalog"));
        assertTrue(moves.adapterPolicy().contains("infer from text"));
        assertTrue(moves.adapterPolicy().contains("push, pull, crash or contact"));
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, moves.support());
    }

    @Test
    void calculationContractKeepsDynamicEvasionCoreOwned() {
        UpstreamCompatibilityMatrix.Entry calculations = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.CORE_CALCULATIONS_AND_COMBAT_STATS);
        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED, calculations.support());
        assertTrue(calculations.contracts().contains("EvasionProfile"));
        assertTrue(calculations.adapterPolicy().contains("Trainer Feature"));
        assertTrue(calculations.adapterPolicy().contains("terrain"));
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
    void itemContractCarriesIdentityWithoutPromotingItemLibrary() {
        UpstreamCompatibilityMatrix.Entry items = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.ITEMS);
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, items.support());
        assertTrue(items.contracts().contains("HeldItemState"));
        assertTrue(items.contracts().contains("heldItemsByCombatant"));
        assertTrue(items.contracts().contains("durable item/reservation store"));
        assertTrue(items.adapterPolicy().contains("unported item effects"));
        assertTrue(items.adapterPolicy().contains("client/entity equipment"));
    }

    @Test
    void matrixPinsTheUpstreamsThatWereActuallyInspected() {
        assertEquals("bc22b78e0a46bd65b6d5ddc38fcabe0b8368440b", UpstreamCompatibilityMatrix.AUTOPTU_JAVA_SHA);
        assertEquals("85dfad45e345d4f77dc5f05b86ea546ef9389d26", UpstreamCompatibilityMatrix.AUTOPTU_PYTHON_SHA);
    }
}
