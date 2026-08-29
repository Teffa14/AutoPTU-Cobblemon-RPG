package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalBattleTraits;
import io.autoptu.cobblemon.authority.CanonicalCombatStats;
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
    void speciesDisplayUsesCanonicalIdentityOnly() {
        assertEquals("Bulbasaur", FabricPokemonDetailRuntime.displayName("pokemon:bulbasaur"));
        assertEquals("Unknown", FabricPokemonDetailRuntime.displayName(""));
    }
}
