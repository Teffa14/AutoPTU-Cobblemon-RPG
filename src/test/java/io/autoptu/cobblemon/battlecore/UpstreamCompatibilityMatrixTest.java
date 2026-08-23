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
        assertTrue(adapter.contracts().contains("ServerOwnedWildEncounterIdentityBinder"));
        assertTrue(adapter.contracts().contains("preprovisioned server-owned canonical participant/roster"));
        assertTrue(adapter.contracts().contains("FabricCanonicalPlayerProvisioning"));
        assertTrue(adapter.contracts().contains("FileCanonicalPokemonRepository"));
        assertTrue(adapter.contracts().contains("FileCanonicalItemReservationRepository"));
        assertTrue(adapter.adapterPolicy().contains("graphical player encounter is still pending"));
        assertTrue(adapter.adapterPolicy().contains("trusted server-owned encounter service"));
        assertTrue(adapter.adapterPolicy().contains("never derives species, level, HP, stats, moves, abilities"));
        assertTrue(adapter.adapterPolicy().contains("cross-aggregate transactions"));
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
    void selectedStatusAbilityPreventionIsLiveWithoutPromotingWholeLifecycle() {
        UpstreamCompatibilityMatrix.Entry statuses = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE);
        assertTrue(statuses.contracts().contains("runtime target-owned ability prevention"));
        assertTrue(statuses.adapterPolicy().contains("complete status ticking"));
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, statuses.support());

        UpstreamCompatibilityMatrix.Entry abilities = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.ABILITIES);
        assertTrue(abilities.contracts().contains("StatusAbilityPreventionResolver"));
        assertTrue(abilities.contracts().contains("RuntimeCombatantState ability suppression"));
        assertTrue(abilities.adapterPolicy().contains("Own Tempo"));
        assertTrue(abilities.adapterPolicy().contains("ignore_defensive_abilities"));
        assertTrue(abilities.adapterPolicy().contains("must not execute ability hooks"));
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, abilities.support());
    }

    @Test
    void trainerFeaturesAdvanceOnlyWithinVerifiedFamilies() {
        UpstreamCompatibilityMatrix.Entry perks = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS);
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, perks.support());
        assertTrue(perks.contracts().contains("TrainerFeatureExecutionService.executeAuthoritative"));
        assertTrue(perks.contracts().contains("grant_temp_hp"));
        assertTrue(perks.contracts().contains("grant_ap"));
        assertTrue(perks.contracts().contains("apply_status"));
        assertTrue(perks.contracts().contains("remove_status"));
        assertTrue(perks.adapterPolicy().contains("wider Python effect library"));
        assertTrue(perks.adapterPolicy().contains("may not grant Features"));
    }

    @Test
    void movementAndDamageGuardsRemainCoreOwned() {
        UpstreamCompatibilityMatrix.Entry movement = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY);
        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED, movement.support());
        assertTrue(movement.adapterPolicy().contains("Wallrunner"));
        assertTrue(movement.adapterPolicy().contains("weather"));
        assertTrue(movement.adapterPolicy().contains("Trainer Features"));

        UpstreamCompatibilityMatrix.Entry damage = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE);
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, damage.support());
        assertTrue(damage.adapterPolicy().contains("independent HP mutation"));
    }

    @Test
    void itemContractCarriesIdentityWithoutPromotingItemLibrary() {
        UpstreamCompatibilityMatrix.Entry items = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.ITEMS);
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, items.support());
        assertTrue(items.contracts().contains("durable item/reservation store"));
        assertTrue(items.adapterPolicy().contains("unported item effects"));
        assertTrue(items.adapterPolicy().contains("client/entity equipment"));
    }

    @Test
    void matrixPinsTheUpstreamsThatWereActuallyInspected() {
        assertEquals("5d9e5069fa0c68432825a48be25fff6ba245d305", UpstreamCompatibilityMatrix.AUTOPTU_JAVA_SHA);
        assertEquals("743b0ff76c63d8ab2131fbf8de4e2e2430b9eea4", UpstreamCompatibilityMatrix.AUTOPTU_PYTHON_SHA);
    }
}
