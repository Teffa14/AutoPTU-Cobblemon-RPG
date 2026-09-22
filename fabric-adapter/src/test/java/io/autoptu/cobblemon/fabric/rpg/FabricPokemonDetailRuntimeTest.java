package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalAccuracyEvasion;
import io.autoptu.cobblemon.authority.CanonicalBaseMovement;
import io.autoptu.cobblemon.authority.CanonicalBattleTraits;
import io.autoptu.cobblemon.authority.CanonicalCombatStats;
import io.autoptu.cobblemon.authority.CanonicalHealth;
import io.autoptu.cobblemon.authority.CanonicalInjuryState;
import io.autoptu.cobblemon.authority.CanonicalPokemonDetail;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class FabricPokemonDetailRuntimeTest {
    @Test
    void summaryLabelsProjectCanonicalCombatInputs() {
        assertEquals(
                "Types grass, poison | Abilities overgrow, chlorophyll",
                FabricPokemonDetailRuntime.traits(new CanonicalBattleTraits(
                        List.of("grass", "poison"),
                        List.of("overgrow", "chlorophyll")
                ))
        );
        assertEquals(
                "ATK 7 | DEF 8 | SPATK 9 | SPDEF 10 | SPD 11",
                FabricPokemonDetailRuntime.stats(new CanonicalCombatStats(7, 8, 9, 10, 11))
        );
    }

    @Test
    void missingOptionalCanonicalInputsStayUnavailable() {
        assertEquals("Types unavailable | Abilities unavailable", FabricPokemonDetailRuntime.traits(null));
        assertEquals("Combat stats unavailable", FabricPokemonDetailRuntime.stats(null));
        assertEquals("Base movement unavailable", FabricPokemonDetailRuntime.movement(null));
        assertEquals("Accuracy/evasion unavailable", FabricPokemonDetailRuntime.accuracy(null));
    }

    @Test
    void mobilityLabelUsesOnlyCanonicalSnapshotValues() {
        CanonicalPokemonDetail detail = new CanonicalPokemonDetail(
                1, "pokemon-1", "pokemon:bulbasaur", 12,
                new CanonicalHealth(20, 30), List.of(),
                new CanonicalCombatStats(7, 8, 9, 10, 11),
                new CanonicalBaseMovement(5, 4, 0, 2, 1),
                new CanonicalAccuracyEvasion(1, 2, 3, 4), null, null,
                new CanonicalInjuryState(0), false, List.of(), 4L
        );
        assertEquals(
                "Movement OVR 5 | SWIM 4 | SKY 0 | LJ 2 | HJ 1 | Accuracy 1 | PEV 2 | SEV 3 | STEV 4",
                FabricPokemonDetailRuntime.mobility(detail)
        );
    }

    @Test
    void conditionLabelUsesOnlyCanonicalSnapshotValues() {
        CanonicalPokemonDetail detail = new CanonicalPokemonDetail(
                1, "pokemon-1", "pokemon:bulbasaur", 12,
                new CanonicalHealth(20, 30), List.of("poisoned", "slowed"),
                new CanonicalCombatStats(7, 8, 9, 10, 11), null, null, null, null,
                new CanonicalInjuryState(2), false, List.of(), 4L
        );
        assertEquals(
                "PTU | Bulbasaur Lv 12 | HP 20/30 | ATK 7 | DEF 8 | SPATK 9 | SPDEF 10 | SPD 11 | Status poisoned, slowed | Injuries 2",
                FabricPokemonDetailRuntime.conditionLabel(detail)
        );
    }

    @Test
    void speciesDisplayUsesCanonicalIdentityOnly() {
        assertEquals("Bulbasaur", FabricPokemonDetailRuntime.displayName("pokemon:bulbasaur"));
        assertEquals("Unknown", FabricPokemonDetailRuntime.displayName(""));
    }
}
