package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.CanonicalStatusEntry;
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
                heldItems("battle-1", "mon-1"),
                statusState("battle-1", "mon-1", List.of(new CanonicalStatusEntry("burned", Map.of("source", "move:ember"))))
        );
        assertEquals("battle-1", envelope.reservationId());
        assertEquals(123L, envelope.rngSeed());
        assertEquals(Set.of("mon-1"), envelope.combatants().keySet());
        assertEquals(List.of(tackle()), envelope.movesByCombatant().get("mon-1"));
        assertEquals("item-1", envelope.heldItemsByCombatant().get("mon-1").itemInstanceId());
        assertEquals("move:ember", envelope.statusStateByCombatant().get("mon-1").entries().get(0).payload().get("source"));
        assertEquals(EnumSet.of(
                        RuntimeCombatantMaterializationReadiness.Requirement.RESOLVED_MOVEMENT_PROFILE,
                        RuntimeCombatantMaterializationReadiness.Requirement.DYNAMIC_ACCURACY_EVASION_FLAGS,
                        RuntimeCombatantMaterializationReadiness.Requirement.RESOLVED_DAMAGE_MODIFIERS),
                EnumSet.copyOf(envelope.unresolvedCoreRequirements()));
        assertFalse(envelope.readyForRuntimeMaterialization());
        assertThrows(UnsupportedOperationException.class, () -> envelope.statusStateByCombatant().clear());
    }

    @Test
    void rejectsArtifactsFromDifferentReservations() {
        assertThrows(IllegalArgumentException.class, () -> BattleRuntimePreparationEnvelope.from(
                materialization("battle-1", List.of("tackle")), moves("battle-2", List.of(tackle())), heldItems("battle-1", "mon-1"),
                statusState("battle-1", "mon-1", List.of(new CanonicalStatusEntry("burned")))));
        assertThrows(IllegalArgumentException.class, () -> BattleRuntimePreparationEnvelope.from(
                materialization("battle-1", List.of("tackle")), moves("battle-1", List.of(tackle())), heldItems("battle-1", "mon-1"),
                statusState("battle-2", "mon-1", List.of(new CanonicalStatusEntry("burned")))));
    }

    @Test
    void rejectsResolvedMoveMetadataThatDoesNotMatchCanonicalLoadoutOrder() {
        AuthoritativeMoveMetadata growl = new AuthoritativeMoveMetadata(
                "growl", new AuthoritativeMoveMetadata.Targeting("all-foes", "burst", null, null, "burst", 1, "Burst 1"),
                "standard", false, null, "At-Will");
        assertThrows(IllegalArgumentException.class, () -> BattleRuntimePreparationEnvelope.from(
                materialization("battle-1", List.of("tackle", "growl")), moves("battle-1", List.of(growl, tackle())),
                heldItems("battle-1", "mon-1"), statusState("battle-1", "mon-1", List.of(new CanonicalStatusEntry("burned")))));
    }

    @Test
    void rejectsHeldItemInjectionOutsideAuthoritativeRoster() {
        BattleCoreHeldItemBootstrapProjection injected = new BattleCoreHeldItemBootstrapProjection(
                "battle-1", Map.of("other", new BattleCombatantHeldItemProjection("other", "item-9", "Leftovers")));
        assertThrows(IllegalArgumentException.class, () -> BattleRuntimePreparationEnvelope.from(
                materialization("battle-1", List.of("tackle")), moves("battle-1", List.of(tackle())), injected,
                statusState("battle-1", "mon-1", List.of(new CanonicalStatusEntry("burned")))));
    }

    @Test
    void rejectsStructuredStatusStateThatDoesNotMatchCanonicalNames() {
        assertThrows(IllegalArgumentException.class, () -> BattleRuntimePreparationEnvelope.from(
                materialization("battle-1", List.of("tackle")), moves("battle-1", List.of(tackle())), heldItems("battle-1", "mon-1"),
                statusState("battle-1", "mon-1", List.of(new CanonicalStatusEntry("flinched", Map.of("applied_round", 2))))));
    }

    private static BattleCoreMaterializationInputProjection materialization(String reservationId, List<String> moveIds) {
        String id = "mon-1";
        RuntimeCombatantMaterializationInput input = new RuntimeCombatantMaterializationInput(
                id, new BattleCombatantInitialPlacement(id, new BattleGridCoordinate(2, 3)),
                new BattleCombatantHealthProjection(id, 42, 50), new BattleCombatantStatProjection(id, 10, 11, 12, 13, 14),
                new BattleCombatantAccuracyEvasionProjection(id, 1, 2, 3, 4), new BattleCombatantTraitsProjection(id, List.of("Fire"), List.of("Blaze")),
                new BattleCombatantMoveLoadoutProjection(id, moveIds), new BattleCombatantAffiliationProjection(id, "team-1", true),
                new BattleCombatantGeometryProjection(id, "Small"), new BattleCombatantBaseMovementProjection(id, 5, 2, 0, 1, 1), Set.of("burned"));
        return new BattleCoreMaterializationInputProjection(reservationId, 123L, Map.of(id, input));
    }

    private static BattleCoreMoveCatalogProjection moves(String reservationId, List<AuthoritativeMoveMetadata> moves) {
        return new BattleCoreMoveCatalogProjection(reservationId, Map.of("mon-1", moves));
    }

    private static BattleCoreHeldItemBootstrapProjection heldItems(String reservationId, String combatantId) {
        return new BattleCoreHeldItemBootstrapProjection(reservationId,
                Map.of(combatantId, new BattleCombatantHeldItemProjection(combatantId, "item-1", "Leftovers")));
    }

    private static BattleCoreStatusStateBootstrapProjection statusState(String reservationId, String combatantId, List<CanonicalStatusEntry> entries) {
        Set<String> names = entries.stream().map(CanonicalStatusEntry::name)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        BattleCoreBootstrapProjection bootstrap = new BattleCoreBootstrapProjection(reservationId, 123L, Map.of(combatantId, names));
        return new BattleCoreStatusStateBootstrapProjection(reservationId, bootstrap,
                Map.of(combatantId, new BattleCombatantStatusStateProjection(combatantId, entries)));
    }

    private static AuthoritativeMoveMetadata tackle() {
        return new AuthoritativeMoveMetadata("tackle",
                new AuthoritativeMoveMetadata.Targeting("single", "melee", 1, 1, null, null, "Melee, 1 Target"),
                "standard", true, new AuthoritativeMoveMetadata.Combat(2, 5, 20, "physical", "Normal"), "At-Will");
    }
}
