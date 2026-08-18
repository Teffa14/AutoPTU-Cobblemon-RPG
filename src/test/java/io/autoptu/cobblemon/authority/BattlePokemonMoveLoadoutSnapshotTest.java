package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattlePokemonMoveLoadoutSnapshotTest {
    @Test
    void freezesCanonicalMoveIdsIntoBattleSnapshot() {
        ArrayList<String> mutableMoveIds = new ArrayList<>(List.of("Tackle", "Quick Attack"));
        CanonicalMoveLoadout loadout = new CanonicalMoveLoadout(mutableMoveIds);
        CanonicalPokemonState state = new CanonicalPokemonState(
                "pokemon-1",
                "player-1",
                "pikachu",
                12,
                Set.of("Overland"),
                Set.of("Burned"),
                new CanonicalCombatStats(8, 7, 9, 8, 11),
                new CanonicalHealth(37, 52),
                loadout,
                null,
                4L
        );

        BattlePokemonSnapshot snapshot = BattlePokemonSnapshot.from(state);
        mutableMoveIds.add("Client Injected Hyper Beam");

        assertEquals(List.of("Tackle", "Quick Attack"), snapshot.moveLoadout().moveIds());
        assertTrue(snapshot.moveLoadout().contains(" Tackle "));
        assertFalse(snapshot.moveLoadout().contains("Client Injected Hyper Beam"));
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.moveLoadout().moveIds().add("Forged Move"));
    }

    @Test
    void preservesExplicitEmptyCanonicalLoadout() {
        CanonicalPokemonState state = new CanonicalPokemonState(
                "pokemon-empty",
                "player-1",
                "magikarp",
                5,
                Set.of(),
                Set.of(),
                new CanonicalCombatStats(5, 5, 5, 5, 5),
                new CanonicalHealth(10, 10),
                new CanonicalMoveLoadout(List.of()),
                null,
                1L
        );

        BattlePokemonSnapshot snapshot = BattlePokemonSnapshot.from(state);

        assertTrue(snapshot.moveLoadout().moveIds().isEmpty());
    }

    @Test
    void legacyConstructorsLeaveMoveAuthorityUnspecified() {
        CanonicalPokemonState legacy = new CanonicalPokemonState(
                "pokemon-legacy",
                "player-1",
                "eevee",
                10,
                Set.of(),
                Set.of(),
                new CanonicalCombatStats(10, 10, 10, 10, 10),
                new CanonicalHealth(20, 20),
                null,
                2L
        );

        assertNull(legacy.moveLoadout());
        assertNull(BattlePokemonSnapshot.from(legacy).moveLoadout());
    }

    @Test
    void rejectsBlankOrDuplicateCanonicalMoveIds() {
        assertThrows(IllegalArgumentException.class,
                () -> new CanonicalMoveLoadout(List.of("Tackle", " ")));
        assertThrows(IllegalArgumentException.class,
                () -> new CanonicalMoveLoadout(List.of("Tackle", " Tackle ")));
    }
}
