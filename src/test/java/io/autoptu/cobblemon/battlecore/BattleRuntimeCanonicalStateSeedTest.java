package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.CanonicalStatusEntry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleRuntimeCanonicalStateSeedTest {
    @Test
    void combinesCanonicalInjuriesAndEnvironmentForOnePreparedBattle() {
        BattleRuntimePreparationEnvelope runtime = runtimePreparation("battle-1", 123L);
        BattleRuntimeRuleStateSeed ruleState = ruleState("battle-1", runtime, 2);
        BattleRuntimeEnvironmentSeed environment = environment("battle-1", runtime);

        BattleRuntimeCanonicalStateSeed seed =
                new BattleRuntimeCanonicalStateSeed("battle-1", ruleState, environment);

        assertEquals("battle-1", seed.reservationId());
        assertEquals(runtime, seed.runtimePreparation());
        assertEquals(2, seed.ruleState().injuryState().currentInjuriesByCombatant().get("mon-1"));
        assertEquals("Rain", seed.environmentState().weather());
        assertEquals("Forest", seed.environmentState().terrainName());
        assertEquals(Set.of("team-1"), seed.environmentState().tailwindTeams());
        assertEquals(Boolean.TRUE, seed.environmentState().groundedByCombatant().get("mon-1"));
    }

    @Test
    void rejectsCrossReservationOrDifferentPreparedBattleInjection() {
        BattleRuntimePreparationEnvelope runtime = runtimePreparation("battle-1", 123L);
        BattleRuntimeRuleStateSeed ruleState = ruleState("battle-1", runtime, 2);

        assertThrows(IllegalArgumentException.class, () -> new BattleRuntimeCanonicalStateSeed(
                "battle-1", ruleState, environment("battle-2", runtimePreparation("battle-2", 123L))));

        BattleRuntimePreparationEnvelope altered = runtimePreparation("battle-1", 999L);
        assertThrows(IllegalArgumentException.class, () -> new BattleRuntimeCanonicalStateSeed(
                "battle-1", ruleState, environment("battle-1", altered)));
    }

    @Test
    void boundaryCannotSeedLifecycleInitiativeOrDerivedEffects() {
        Set<String> componentNames = Arrays.stream(BattleRuntimeCanonicalStateSeed.class.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(java.util.stream.Collectors.toSet());

        assertEquals(Set.of("reservationId", "ruleState", "environmentState"), componentNames);
        assertFalse(componentNames.contains("currentRound"));
        assertFalse(componentNames.contains("initiativeOrder"));
        assertFalse(componentNames.contains("initiativeCursor"));
        assertFalse(componentNames.contains("trainerActions"));
        assertFalse(componentNames.contains("injuriesLastRound"));
        assertFalse(componentNames.contains("injuriesPreviousRound"));
        assertFalse(componentNames.contains("auraBreakBlockers"));
        assertFalse(componentNames.contains("auraStormBonus"));
    }

    @Test
    void compatibilityScopeKeepsEnvironmentInterpretationAndMutationCoreOwned() {
        IntegrationFeatureCompatibility.Requirement requirement = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.RUNTIME_RULE_STATE_SEED);

        assertEquals(Set.of(
                        UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE,
                        UpstreamCompatibilityMatrix.Capability.ABILITIES,
                        UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE,
                        UpstreamCompatibilityMatrix.Capability.TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS,
                        UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE),
                requirement.capabilities());
        assertFalse(requirement.hasBlockingDependency());
        assertTrue(requirement.boundedScope().contains("current injury"));
        assertTrue(requirement.boundedScope().contains("weather"));
        assertTrue(requirement.boundedScope().contains("Tailwind"));
        assertTrue(requirement.boundedScope().contains("grounded"));
        assertTrue(requirement.boundedScope().contains("mounted"));
        assertTrue(requirement.boundedScope().contains("AutoPTU-Java owns"));
    }

    private static BattleRuntimeRuleStateSeed ruleState(
            String reservationId, BattleRuntimePreparationEnvelope runtime, int injuries) {
        return new BattleRuntimeRuleStateSeed(
                reservationId,
                runtime,
                new BattleRuntimeInjuryStateSeed(
                        reservationId, Set.of("mon-1"), Map.of("mon-1", injuries)));
    }

    private static BattleRuntimeEnvironmentSeed environment(
            String reservationId, BattleRuntimePreparationEnvelope runtime) {
        return new BattleRuntimeEnvironmentSeed(
                reservationId,
                runtime,
                "Rain",
                "Forest",
                Set.of("team-1"),
                Map.of("mon-1", true),
                Map.of());
    }

    private static BattleRuntimePreparationEnvelope runtimePreparation(String reservationId, long seed) {
        String id = "mon-1";
        RuntimeCombatantMaterializationInput input = new RuntimeCombatantMaterializationInput(
                id,
                new BattleCombatantInitialPlacement(id, new BattleGridCoordinate(2, 3)),
                new BattleCombatantHealthProjection(id, 42, 50),
                new BattleCombatantStatProjection(id, 10, 11, 12, 13, 14),
                new BattleCombatantAccuracyEvasionProjection(id, 1, 2, 3, 4),
                new BattleCombatantTraitsProjection(id, List.of("Fire"), List.of("Blaze")),
                new BattleCombatantMoveLoadoutProjection(id, List.of("tackle")),
                new BattleCombatantAffiliationProjection(id, "team-1", true),
                new BattleCombatantGeometryProjection(id, "Small"),
                new BattleCombatantBaseMovementProjection(id, 5, 2, 0, 1, 1),
                Set.of("burned")
        );
        AuthoritativeMoveMetadata tackle = new AuthoritativeMoveMetadata(
                "tackle",
                new AuthoritativeMoveMetadata.Targeting(
                        "single", "melee", 1, 1, null, null, "Melee, 1 Target"),
                "standard",
                true,
                new AuthoritativeMoveMetadata.Combat(2, 5, 20, "physical", "Normal"),
                "At-Will"
        );
        return new BattleRuntimePreparationEnvelope(
                reservationId,
                seed,
                Map.of(id, input),
                Map.of(id, List.of(tackle)),
                Map.of(id, new BattleCombatantHeldItemProjection(id, "item-1", "Leftovers")),
                Map.of(id, new BattleCombatantStatusStateProjection(
                        id, List.of(new CanonicalStatusEntry(
                                "burned", Map.of("source", "move:ember"))))),
                Set.of(RuntimeCombatantMaterializationReadiness.Requirement.RESOLVED_MOVEMENT_PROFILE)
        );
    }
}
