package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattleRuntimePreparationEnvelopeTest {
    @Test
    void bundlesOnlyReservationBoundAuthoritativePreparationArtifacts() {
        BattleRuntimePreparationEnvelope envelope = BattleRuntimePreparationEnvelope.from(
                materialization("battle-1", List.of("tackle")),
                moves("battle-1", List.of(tackle())),
                heldItems("battle-1", "mon-1")
        );

        assertEquals("battle-1", envelope.reservationId());
        assertEquals(123L, envelope.rngSeed());
        assertEquals(Set.of("mon-1"), envelope.combatants().keySet());
        assertEquals(List.of(tackle()), envelope.movesByCombatant().get("mon-1"));
        assertEquals("item-1", envelope.heldItemsByCombatant().get("mon-1").itemInstanceId());
        assertEquals(EnumSet.of(
                        RuntimeCombatantMaterializationReadiness.Requirement.RESOLVED_MOVEMENT_PROFILE,
                        RuntimeCombatantMaterializationReadiness.Requirement.ACTION_BUDGET_INITIALIZATION,
                        RuntimeCombatantMaterializationReadiness.Requirement.DYNAMIC_ACCURACY_EVASION_FLAGS,
                        RuntimeCombatantMaterializationReadiness.Requirement.RESOLVED_DAMAGE_MODIFIERS),
                EnumSet.copyOf(envelope.unresolvedCoreRequirements()));
        assertFalse(envelope.readyForRuntimeMaterialization());

        assertThrows(UnsupportedOperationException.class,
                () -> envelope.combatants().put("other", envelope.combatants().get("mon-1")));
        assertThrows(UnsupportedOperationException.class,
                () -> envelope.movesByCombatant().get("mon-1").add(tackle()));
        assertThrows(UnsupportedOperationException.class,
                () -> envelope.unresolvedCoreRequirements().clear());
    }

    @Test
    void rejectsArtifactsFromDifferentReservations() {
        assertThrows(IllegalArgumentException.class, () -> BattleRuntimePreparationEnvelope.from(
                materialization("battle-1", List.of("tackle")),
                moves("battle-2", List.of(tackle())),
                heldItems("battle-1", "mon-1")
        ));
    }

    @Test
    void rejectsResolvedMoveMetadataThatDoesNotMatchCanonicalLoadoutOrder() {
        AuthoritativeMoveMetadata growl = new AuthoritativeMoveMetadata(
                "growl",
                new AuthoritativeMoveMetadata.Targeting("all-foes", "burst", null, null, "burst", 1, "Burst 1"),
                "standard", false, null, "At-Will"
        );

        assertThrows(IllegalArgumentException.class, () -> BattleRuntimePreparationEnvelope.from(
                materialization("battle-1", List.of("tackle", "growl")),
                moves("battle-1", List.of(growl, tackle())),
                heldItems("battle-1", "mon-1")
        ));
    }

    @Test
    void rejectsHeldItemInjectionOutsideAuthoritativeRoster() {
        BattleCoreHeldItemBootstrapProjection injected = new BattleCoreHeldItemBootstrapProjection(
                "battle-1",
                Map.of("other", new BattleCombatantHeldItemProjection("other", "item-9", "Leftovers"))
        );

        assertThrows(IllegalArgumentException.class, () -> BattleRuntimePreparationEnvelope.from(
                materialization("battle-1", List.of("tackle")),
                moves("battle-1", List.of(tackle())),
                injected
        ));
    }

    private static BattleCoreMaterializationInputProjection materialization(String reservationId, List<String> moveIds) {
        String id = "mon-1";
        RuntimeCombatantMaterializationInput input = new RuntimeCombatantMaterializationInput(
                id,
                new BattleCombatantInitialPlacement(id, new BattleGridCoordinate(2, 3)),
                new BattleCombatantHealthProjection(id, 42, 50),
                new BattleCombatantStatProjection(id, 10, 11, 12, 13, 14),
                new BattleCombatantAccuracyEvasionProjection(id, 1, 2, 3, 4),
                new BattleCombatantTraitsProjection(id, List.of("Fire"), List.of("Blaze")),
                new BattleCombatantMoveLoadoutProjection(id, moveIds),
                new BattleCombatantAffiliationProjection(id, "team-1", true),
                new BattleCombatantGeometryProjection(id, "Small"),
                new BattleCombatantBaseMovementProjection(id, 5, 2, 0, 1, 1),
                Set.of("Burned")
        );
        return new BattleCoreMaterializationInputProjection(reservationId, 123L, Map.of(id, input));
    }

    private static BattleCoreMoveCatalogProjection moves(String reservationId, List<AuthoritativeMoveMetadata> moves) {
        return new BattleCoreMoveCatalogProjection(reservationId, Map.of("mon-1", moves));
    }

    private static BattleCoreHeldItemBootstrapProjection heldItems(String reservationId, String combatantId) {
        return new BattleCoreHeldItemBootstrapProjection(
                reservationId,
                Map.of(combatantId, new BattleCombatantHeldItemProjection(combatantId, "item-1", "Leftovers"))
        );
    }

    private static AuthoritativeMoveMetadata tackle() {
        return new AuthoritativeMoveMetadata(
                "tackle",
                new AuthoritativeMoveMetadata.Targeting("single", "melee", 1, 1, null, null, "Melee, 1 Target"),
                "standard",
                true,
                new AuthoritativeMoveMetadata.Combat(2, 5, 20, "physical", "Normal"),
                "At-Will"
        );
    }
}
