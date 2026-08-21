package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.CanonicalStatusEntry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleRuntimeAssemblySeedTest {
    @Test
    void joinsTrainerAndCanonicalRuntimeStateAroundOnePreparedBattle() {
        BattleRuntimePreparationEnvelope battle = battle("battle-1", 123L);
        BattleTrainerRuntimePreparationEnvelope trainer = trainer(battle);
        BattleRuntimeCanonicalStateSeed canonical = canonical(battle);

        BattleRuntimeAssemblySeed seed = BattleRuntimeAssemblySeed.from(trainer, canonical);

        assertEquals("battle-1", seed.reservationId());
        assertEquals(123L, seed.rngSeed());
        assertEquals(battle, seed.battle());
        assertEquals("trainer-1", seed.trainer().trainerId());
        assertEquals(2, seed.ruleState().injuryState().currentInjuriesByCombatant().get("mon-1"));
        assertEquals("Rain", seed.environmentState().weather());
        assertEquals(EnumSet.of(
                        RuntimeCombatantMaterializationReadiness.Requirement.RESOLVED_MOVEMENT_PROFILE,
                        RuntimeCombatantMaterializationReadiness.Requirement.DYNAMIC_ACCURACY_EVASION_FLAGS,
                        RuntimeCombatantMaterializationReadiness.Requirement.RESOLVED_DAMAGE_MODIFIERS),
                EnumSet.copyOf(seed.unresolvedCoreRequirements()));
        assertFalse(seed.readyForRuntimeMaterialization());
    }

    @Test
    void rejectsCrossReservationAndSameIdDifferentPreparedBattleInjection() {
        BattleRuntimePreparationEnvelope battle = battle("battle-1", 123L);
        BattleTrainerRuntimePreparationEnvelope trainer = trainer(battle);

        BattleRuntimePreparationEnvelope otherReservation = battle("battle-2", 123L);
        assertThrows(IllegalArgumentException.class, () -> BattleRuntimeAssemblySeed.from(
                trainer,
                canonical(otherReservation)));

        BattleRuntimePreparationEnvelope altered = battle("battle-1", 999L);
        assertThrows(IllegalArgumentException.class, () -> BattleRuntimeAssemblySeed.from(
                trainer,
                canonical(altered)));
    }

    @Test
    void rejectsTrainerCombatantIdentityCollisionBeforeRuntimeAssembly() {
        BattleRuntimePreparationEnvelope battle = battle("battle-1", 123L);
        BattleTrainerRuntimePreparationEnvelope collidingTrainer = new BattleTrainerRuntimePreparationEnvelope(
                battle,
                new BattleTrainerRuntimeProjection(
                        "mon-1", Set.of("Attack Link"), 3, Set.of("mon-1")));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> BattleRuntimeAssemblySeed.from(collidingTrainer, canonical(battle)));

        assertTrue(error.getMessage().contains("must not collide"));
    }

    @Test
    void boundaryCannotSeedCoreOwnedRuntimeResolutionOrLifecycleState() {
        Set<String> components = Arrays.stream(BattleRuntimeAssemblySeed.class.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());

        assertEquals(Set.of("reservationId", "trainerPreparation", "canonicalState"), components);
        for (String forbidden : Set.of(
                "movementProfile",
                "actionBudget",
                "accuracyFlags",
                "damageModifiers",
                "currentRound",
                "initiativeOrder",
                "initiativeCursor",
                "temporaryEffects",
                "battleOutcome")) {
            assertFalse(components.contains(forbidden));
        }
    }

    @Test
    void assemblyConsumesOnlyExistingMappedNonBlockingIntegrationFeatures() {
        for (IntegrationFeatureCompatibility.Feature feature : new IntegrationFeatureCompatibility.Feature[]{
                IntegrationFeatureCompatibility.Feature.RUNTIME_BATTLE_PREPARATION_ENVELOPE,
                IntegrationFeatureCompatibility.Feature.CANONICAL_TRAINER_RUNTIME_BOOTSTRAP,
                IntegrationFeatureCompatibility.Feature.RUNTIME_RULE_STATE_SEED}) {
            assertFalse(IntegrationFeatureCompatibility.requirement(feature).hasBlockingDependency());
        }

        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED,
                UpstreamCompatibilityMatrix.entry(
                        UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                UpstreamCompatibilityMatrix.entry(
                        UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                UpstreamCompatibilityMatrix.entry(
                        UpstreamCompatibilityMatrix.Capability.TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS).support());
    }

    private static BattleTrainerRuntimePreparationEnvelope trainer(BattleRuntimePreparationEnvelope battle) {
        return new BattleTrainerRuntimePreparationEnvelope(
                battle,
                new BattleTrainerRuntimeProjection(
                        "trainer-1", Set.of("Attack Link"), 3, Set.of("mon-1")));
    }

    private static BattleRuntimeCanonicalStateSeed canonical(BattleRuntimePreparationEnvelope battle) {
        BattleRuntimeInjuryStateSeed injuries = new BattleRuntimeInjuryStateSeed(
                battle.reservationId(), Set.of("mon-1"), Map.of("mon-1", 2));
        BattleRuntimeRuleStateSeed rules = new BattleRuntimeRuleStateSeed(
                battle.reservationId(), battle, injuries);
        BattleRuntimeEnvironmentSeed environment = new BattleRuntimeEnvironmentSeed(
                battle.reservationId(), battle, "Rain", "Forest", Set.of("team-1"),
                Map.of("mon-1", true), Map.of());
        return new BattleRuntimeCanonicalStateSeed(
                battle.reservationId(), rules, environment);
    }

    private static BattleRuntimePreparationEnvelope battle(String reservationId, long seed) {
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
                Map.of(),
                Map.of(id, new BattleCombatantStatusStateProjection(
                        id, List.of(new CanonicalStatusEntry(
                                "burned", Map.of("source", "move:ember"))))),
                EnumSet.of(
                        RuntimeCombatantMaterializationReadiness.Requirement.RESOLVED_MOVEMENT_PROFILE,
                        RuntimeCombatantMaterializationReadiness.Requirement.DYNAMIC_ACCURACY_EVASION_FLAGS,
                        RuntimeCombatantMaterializationReadiness.Requirement.RESOLVED_DAMAGE_MODIFIERS)
        );
    }
}
