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

class BattleRuntimeRuleStateSeedTest {
    @Test
    void bindsPreparedCombatantsToCanonicalCurrentInjuries() {
        BattleRuntimePreparationEnvelope runtime = runtimePreparation("battle-1");
        BattleRuntimeInjuryStateSeed injuries = new BattleRuntimeInjuryStateSeed(
                "battle-1", Set.of("mon-1"), Map.of("mon-1", 2));

        BattleRuntimeRuleStateSeed seed = new BattleRuntimeRuleStateSeed("battle-1", runtime, injuries);

        assertEquals("battle-1", seed.reservationId());
        assertEquals(Set.of("mon-1"), seed.runtimePreparation().combatants().keySet());
        assertEquals(2, seed.injuryState().currentInjuriesByCombatant().get("mon-1"));
    }

    @Test
    void rejectsCrossReservationOrRosterInjection() {
        BattleRuntimePreparationEnvelope runtime = runtimePreparation("battle-1");

        assertThrows(IllegalArgumentException.class, () -> new BattleRuntimeRuleStateSeed(
                "battle-1", runtime,
                new BattleRuntimeInjuryStateSeed("battle-2", Set.of("mon-1"), Map.of("mon-1", 2))
        ));
        assertThrows(IllegalArgumentException.class, () -> new BattleRuntimeRuleStateSeed(
                "battle-1", runtime,
                new BattleRuntimeInjuryStateSeed("battle-1", Set.of("other"), Map.of("other", 2))
        ));
    }

    @Test
    void boundaryCannotSeedLifecycleClockOrAbilityDecisions() {
        Set<String> componentNames = Arrays.stream(BattleRuntimeRuleStateSeed.class.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(java.util.stream.Collectors.toSet());

        assertEquals(Set.of("reservationId", "runtimePreparation", "injuryState"), componentNames);
        assertFalse(componentNames.contains("currentRound"));
        assertFalse(componentNames.contains("auraBreakBlockers"));
        assertFalse(componentNames.contains("auraStormBonus"));
        assertFalse(componentNames.contains("injuriesLastRound"));
        assertFalse(componentNames.contains("injuriesPreviousRound"));
    }

    @Test
    void compatibilityEntryKeepsRuleExecutionCoreOwned() {
        IntegrationFeatureCompatibility.Requirement requirement = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.RUNTIME_RULE_STATE_SEED);

        assertEquals(Set.of(
                        UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE,
                        UpstreamCompatibilityMatrix.Capability.ABILITIES,
                        UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE),
                requirement.capabilities());
        assertFalse(requirement.hasBlockingDependency());
        assertTrue(requirement.boundedScope().contains("current injury"));
        assertTrue(requirement.boundedScope().contains("battle round"));
        assertTrue(requirement.boundedScope().contains("Aura Break"));
        assertTrue(requirement.boundedScope().contains("AutoPTU-Java owns"));
    }

    private static BattleRuntimePreparationEnvelope runtimePreparation(String reservationId) {
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
                new AuthoritativeMoveMetadata.Targeting("single", "melee", 1, 1, null, null, "Melee, 1 Target"),
                "standard",
                true,
                new AuthoritativeMoveMetadata.Combat(2, 5, 20, "physical", "Normal"),
                "At-Will"
        );
        return new BattleRuntimePreparationEnvelope(
                reservationId,
                123L,
                Map.of(id, input),
                Map.of(id, List.of(tackle)),
                Map.of(id, new BattleCombatantHeldItemProjection(id, "item-1", "Leftovers")),
                Map.of(id, new BattleCombatantStatusStateProjection(
                        id, List.of(new CanonicalStatusEntry("burned", Map.of("source", "move:ember"))))),
                Set.of(RuntimeCombatantMaterializationReadiness.Requirement.RESOLVED_MOVEMENT_PROFILE)
        );
    }
}
