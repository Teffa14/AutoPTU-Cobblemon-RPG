package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentUpstreamCompatibilityInspectionTest {
    @Test
    void pinsTheActuallyInspectedUpstreamHeads() {
        assertEquals("e5439ac27a77cc41300435ed352cf4baf41f1269", CurrentUpstreamCompatibilityInspection.AUTOPTU_JAVA_SHA);
        assertEquals("ce82de564a09b4b66abebda356eca46a9723af4d", CurrentUpstreamCompatibilityInspection.AUTOPTU_PYTHON_SHA);
    }

    @Test
    void recordsCurrentBoundariesWithoutPromotingBroaderSystems() {
        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED,
                CurrentUpstreamCompatibilityInspection.evidence(UpstreamCompatibilityMatrix.Capability.CORE_TARGETING).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED,
                CurrentUpstreamCompatibilityInspection.evidence(UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED,
                CurrentUpstreamCompatibilityInspection.evidence(UpstreamCompatibilityMatrix.Capability.AI_LEGAL_ACTION_INFRASTRUCTURE).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK).support());

        String statuses = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE).contracts();
        String statusLimit = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE).limitation();
        assertTrue(statuses.contains("ordered stacked StatusEntry"));
        assertTrue(statuses.contains("apply_status/remove_status"));
        assertTrue(statuses.contains("StatusAbilityPreventionResolver"));
        assertTrue(statusLimit.contains("Complete status ticking"));
        assertTrue(statusLimit.contains("must not interpret or execute"));

        String abilities = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.ABILITIES).contracts();
        String abilityLimit = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.ABILITIES).limitation();
        assertTrue(abilities.contains("Merged upstream PR #152"));
        assertTrue(abilities.contains("StatusAbilityPreventionResolver"));
        assertTrue(abilityLimit.contains("not yet wired into live status application"));
        assertTrue(abilityLimit.contains("ignore_defensive_abilities"));

        String perks = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS).contracts();
        String perksLimit = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS).limitation();
        assertTrue(perks.contains("only after the effect reports applied"));
        assertTrue(perks.contains("grant_temp_hp"));
        assertTrue(perks.contains("grant_ap"));
        assertTrue(perks.contains("apply_status"));
        assertTrue(perks.contains("remove_status"));
        assertTrue(perksLimit.contains("wider Python effect library"));
        assertTrue(perksLimit.contains("must not grant Features"));

        CurrentUpstreamCompatibilityInspection.Evidence adapter = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK);
        assertTrue(adapter.contracts().contains("FabricCanonicalPlayerProvisioning"));
        assertTrue(adapter.contracts().contains("minecraft-player:<uuid>"));
        assertTrue(adapter.contracts().contains("PersistentCanonicalPlayerEncounterContextSource.fromWorldRuntime"));
        assertTrue(adapter.contracts().contains("FabricAuthenticatedPlayerContextResolver.persistentWorld"));
        assertTrue(adapter.contracts().contains("CobblemonPlayerVsWildClaimCoordinator.persistentWorld"));
        assertTrue(adapter.contracts().contains("PersistentCanonicalPlayerPokemonIdentityBinder"));
        assertTrue(adapter.contracts().contains("FileCanonicalPokemonRepository"));
        assertTrue(adapter.contracts().contains("FileCanonicalItemReservationRepository"));
        assertTrue(adapter.contracts().contains("active item reservation"));
        assertTrue(adapter.contracts().contains("two-process dedicated-server restart smoke"));
        assertTrue(adapter.limitation().contains("authenticated graphical client encounter"));
        assertTrue(adapter.limitation().contains("Encounter-profile creation and wild Pokemon canonical identity provisioning"));
        assertTrue(adapter.limitation().contains("Cross-aggregate transaction recovery"));
        assertTrue(adapter.limitation().contains("RuntimeCombatantState materialization"));
        assertTrue(adapter.limitation().contains("never supply PTU stats"));
    }
}
