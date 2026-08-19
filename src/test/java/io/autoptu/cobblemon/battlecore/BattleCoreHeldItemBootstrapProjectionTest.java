package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleAuthoritySnapshot;
import io.autoptu.cobblemon.authority.BattleItemSnapshot;
import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;
import io.autoptu.cobblemon.authority.BattleTrainerSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleCoreHeldItemBootstrapProjectionTest {
    @Test
    void projectsStableHeldItemIdentityFromAuthoritativeReservation() {
        BattleCoreHeldItemBootstrapProjection projection = BattleCoreHeldItemBootstrapProjection.from(
                snapshot(
                        List.of(pokemon("mon-1", "item-1")),
                        List.of(item("item-1", "Pink Pearl", true))
                ));

        assertEquals("battle-1", projection.reservationId());
        assertEquals(new BattleCombatantHeldItemProjection("mon-1", "item-1", "Pink Pearl"),
                projection.heldItemsByCombatant().get("mon-1"));
        assertThrows(UnsupportedOperationException.class,
                () -> projection.heldItemsByCombatant().put("other",
                        new BattleCombatantHeldItemProjection("other", "item-2", "Other")));
    }

    @Test
    void combatantsWithoutHeldItemsRemainAbsentInsteadOfReceivingInventedDefaults() {
        BattleCoreHeldItemBootstrapProjection projection = BattleCoreHeldItemBootstrapProjection.from(
                snapshot(List.of(pokemon("mon-1", null)), List.of()));
        assertTrue(projection.heldItemsByCombatant().isEmpty());
    }

    @Test
    void rejectsHeldItemReferenceOutsideFrozenReservation() {
        assertThrows(IllegalArgumentException.class, () -> BattleCoreHeldItemBootstrapProjection.from(
                snapshot(List.of(pokemon("mon-1", "missing")), List.of())));
    }

    @Test
    void rejectsConsumableMasqueradingAsHeldItem() {
        assertThrows(IllegalArgumentException.class, () -> BattleCoreHeldItemBootstrapProjection.from(
                snapshot(
                        List.of(pokemon("mon-1", "item-1")),
                        List.of(item("item-1", "Potion", false))
                )));
    }

    @Test
    void rejectsOneStableItemInstanceAssignedToMultipleCombatants() {
        assertThrows(IllegalArgumentException.class, () -> BattleCoreHeldItemBootstrapProjection.from(
                snapshot(
                        List.of(pokemon("mon-1", "item-1"), pokemon("mon-2", "item-1")),
                        List.of(item("item-1", "Pink Pearl", true))
                )));
    }

    @Test
    void rejectsForgedMapKeyThatDoesNotMatchEmbeddedCombatant() {
        assertThrows(IllegalArgumentException.class, () -> new BattleCoreHeldItemBootstrapProjection(
                "battle-1",
                Map.of("forged", new BattleCombatantHeldItemProjection("mon-1", "item-1", "Pink Pearl"))
        ));
    }

    private static BattleAuthoritySnapshot snapshot(List<BattlePokemonSnapshot> roster, List<BattleItemSnapshot> items) {
        return new BattleAuthoritySnapshot(
                "battle-1",
                "player-1",
                new BattleTrainerSnapshot("player-1", Set.of(), Map.of(), 1),
                roster,
                items,
                123L
        );
    }

    private static BattlePokemonSnapshot pokemon(String id, String heldItemInstanceId) {
        return new BattlePokemonSnapshot(id, "player-1", "species", 10, Set.of(), heldItemInstanceId, 1);
    }

    private static BattleItemSnapshot item(String id, String templateId, boolean heldItem) {
        return new BattleItemSnapshot(id, "player-1", templateId, 1, 1, heldItem);
    }
}
