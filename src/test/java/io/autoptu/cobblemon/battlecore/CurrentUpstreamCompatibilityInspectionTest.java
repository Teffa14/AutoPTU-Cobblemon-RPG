package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentUpstreamCompatibilityInspectionTest {
    @Test
    void pinsTheActuallyInspectedUpstreamHeads() {
        assertEquals("46b8873df5839cca1b57106a16248c457d93f5fe", CurrentUpstreamCompatibilityInspection.AUTOPTU_JAVA_SHA);
        assertEquals("91270f54b237e177fef46a875f5599e114db97e3", CurrentUpstreamCompatibilityInspection.AUTOPTU_PYTHON_SHA);
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
        assertTrue(statuses.contains("StatusAbilityPreventionResolver"));
        assertTrue(statuses.contains("RuntimeCombatantState ability suppression"));
        assertTrue(statuses.contains("Merged PR #154"));
        assertTrue(statusLimit.contains("complete status ticking"));
        assertTrue(statusLimit.contains("contract-only"));
        assertTrue(statusLimit.contains("must not interpret or execute"));

        String abilities = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.ABILITIES).contracts();
        String abilityLimit = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.ABILITIES).limitation();
        assertTrue(abilities.contains("Merged upstream PR #153"));
        assertTrue(abilities.contains("RuntimeCombatantState owns abilitiesSuppressed"));
        assertTrue(abilities.contains("Merged PR #154"));
        assertTrue(abilityLimit.contains("Own Tempo"));
        assertTrue(abilityLimit.contains("Color Change"));
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
        assertTrue(perks.contains("91270f54"));
        assertTrue(perksLimit.contains("wider Python effect library"));
        assertTrue(perksLimit.contains("must not grant Features"));

        CurrentUpstreamCompatibilityInspection.Evidence adapter = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK);
        assertTrue(adapter.contracts().contains("CanonicalWildEncounterBlueprintSource"));
        assertTrue(adapter.contracts().contains("ServerOwnedWildEncounterPreparationService"));
        assertTrue(adapter.contracts().contains("ServerOwnedWildEncounterIdentityBinder"));
        assertTrue(adapter.contracts().contains("ServerOwnedWildEncounterProvisioningService"));
        assertTrue(adapter.contracts().contains("CanonicalEncounterPokemonState"));
        assertTrue(adapter.contracts().contains("FileCanonicalPokemonRepository"));
        assertTrue(adapter.contracts().contains("FileCanonicalItemReservationRepository"));
        assertTrue(adapter.limitation().contains("authenticated graphical client encounter"));
        assertTrue(adapter.limitation().contains("world/campaign RPG generator"));
        assertTrue(adapter.limitation().contains("deterministic provisioning seed"));
        assertTrue(adapter.limitation().contains("cross-aggregate transaction recovery"));
        assertTrue(adapter.limitation().contains("RuntimeCombatantState materialization"));
        assertTrue(adapter.limitation().contains("never PTU stats"));
    }
}
