package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleCoreMaterializationInputProjectionTest {
    @Test
    void packagesEveryIntegrationFrozenRuntimeInputWithoutCoreResolvedDefaults() {
        BattleCoreMaterializationInputProjection projection = projection();

        assertEquals("battle-1", projection.reservationId());
        assertEquals(123L, projection.rngSeed());
        RuntimeCombatantMaterializationInput input = projection.combatants().get("mon-1");
        assertEquals(new BattleGridCoordinate(2, 3), input.initialPlacement().anchor());
        assertEquals(42, input.health().currentHp());
        assertEquals(50, input.health().maxHp());
        assertEquals(10, input.combatStats().atk());
        assertEquals(1, input.baseAccuracyEvasion().accuracyStage());
        assertEquals(List.of("Fire"), input.traits().types());
        assertEquals(List.of("Blaze"), input.traits().abilities());
        assertEquals(List.of("Tackle", "Growl"), input.moveLoadout().moveIds());
        assertEquals("team-1", input.affiliation().teamId());
        assertEquals("Small", input.geometry().sizeLabel());
        assertEquals(5, input.baseMovement().overland());
        assertEquals(Set.of("Burned"), input.statuses());

        assertThrows(UnsupportedOperationException.class,
                () -> projection.combatants().put("other", input));
        assertThrows(UnsupportedOperationException.class,
                () -> input.statuses().add("Poisoned"));
    }

    @Test
    void rejectsCrossCombatantProjectionInjection() {
        RuntimeCombatantMaterializationInput valid = projection().combatants().get("mon-1");
        BattleCombatantHealthProjection forgedHealth = new BattleCombatantHealthProjection("other", 1, 1);

        assertThrows(IllegalArgumentException.class, () -> new RuntimeCombatantMaterializationInput(
                valid.combatantId(), valid.initialPlacement(), forgedHealth, valid.combatStats(),
                valid.baseAccuracyEvasion(), valid.traits(), valid.moveLoadout(), valid.affiliation(),
                valid.geometry(), valid.baseMovement(), valid.statuses()));
    }

    @Test
    void coreResolvedRuntimeFieldsAreStructurallyAbsent() {
        Set<String> componentNames = Arrays.stream(RuntimeCombatantMaterializationInput.class.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());

        for (String forbidden : Set.of(
                "movementProfile", "actionBudget", "sniper", "noGuard", "blur",
                "probabilityControl", "damageModifiers")) {
            assertFalse(componentNames.contains(forbidden), "adapter boundary must not carry " + forbidden);
        }
        assertTrue(componentNames.contains("baseMovement"));
        assertTrue(componentNames.contains("baseAccuracyEvasion"));
    }

    private static BattleCoreMaterializationInputProjection projection() {
        String reservationId = "battle-1";
        String combatantId = "mon-1";

        BattleCombatantStatProjection stats = new BattleCombatantStatProjection(combatantId, 10, 11, 12, 13, 14);
        BattleCombatantHealthProjection health = new BattleCombatantHealthProjection(combatantId, 42, 50);
        BattleCombatantAffiliationProjection affiliation =
                new BattleCombatantAffiliationProjection(combatantId, "team-1", true);
        BattleCombatantMoveLoadoutProjection moves =
                new BattleCombatantMoveLoadoutProjection(combatantId, List.of("Tackle", "Growl"));
        BattleCoreBootstrapProjection combatState = new BattleCoreBootstrapProjection(
                reservationId,
                123L,
                Set.of(combatantId),
                Map.of(combatantId, Set.of("Burned")),
                Map.of(combatantId, stats),
                Map.of(combatantId, health),
                Map.of(combatantId, affiliation),
                Map.of(combatantId, moves)
        );

        BattleInitialPlacementSnapshot placement = new BattleInitialPlacementSnapshot(
                reservationId,
                Map.of(combatantId,
                        new BattleCombatantInitialPlacement(combatantId, new BattleGridCoordinate(2, 3)))
        );
        BattleCorePlacedBootstrapProjection placed =
                new BattleCorePlacedBootstrapProjection(reservationId, combatState, placement);

        BattleCombatantBaseMovementProjection baseMovement =
                new BattleCombatantBaseMovementProjection(combatantId, 5, 2, 0, 1, 1);
        BattleCoreMovementBootstrapProjection movement = new BattleCoreMovementBootstrapProjection(
                reservationId, placed, Map.of(combatantId, baseMovement));

        BattleCombatantGeometryProjection geometry =
                new BattleCombatantGeometryProjection(combatantId, "Small");
        BattleCoreGeometryBootstrapProjection geometryBootstrap = new BattleCoreGeometryBootstrapProjection(
                reservationId, movement, Map.of(combatantId, geometry));

        BattleCombatantTraitsProjection traits =
                new BattleCombatantTraitsProjection(combatantId, List.of("Fire"), List.of("Blaze"));
        BattleCoreTraitsBootstrapProjection traitsBootstrap = new BattleCoreTraitsBootstrapProjection(
                reservationId, geometryBootstrap, Map.of(combatantId, traits));

        BattleCombatantAccuracyEvasionProjection accuracy =
                new BattleCombatantAccuracyEvasionProjection(combatantId, 1, 2, 3, 4);
        BattleCoreAccuracyBootstrapProjection accuracyBootstrap = new BattleCoreAccuracyBootstrapProjection(
                reservationId, traitsBootstrap, Map.of(combatantId, accuracy));

        return BattleCoreMaterializationInputProjection.from(accuracyBootstrap);
    }
}
