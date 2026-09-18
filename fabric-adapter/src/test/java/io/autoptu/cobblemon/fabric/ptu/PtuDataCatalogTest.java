package io.autoptu.cobblemon.fabric.ptu;

import org.junit.jupiter.api.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PtuDataCatalogTest {
    static PtuDataCatalog catalog;
    @BeforeAll static void load() { catalog = PtuDataCatalog.bundled(); }

    @Test void loadsRealDatasetAndCoreDamageTable() {
        assertTrue(catalog.moveCount() > 800);
        assertTrue(catalog.speciesCount() > 900);
        assertTrue(catalog.abilityCount() > 300);
        var tackle = catalog.move("tackle").orElseThrow();
        assertEquals(5, tackle.damageBase());
        assertEquals(2, tackle.ac());
        assertEquals("1d8+8", tackle.baseDice());
        assertEquals("At-Will", tackle.frequency());
        assertFalse(catalog.ability("overgrow").orElseThrow().effect().isBlank());
        System.out.printf("PTU catalog: %d species, %d moves, %d abilities, %d diagnostics%n",
                catalog.speciesCount(), catalog.moveCount(), catalog.abilityCount(), catalog.diagnostics().size());
    }
    @Test void resolvesRegionalFormWithoutBaseFallback() {
        var normal = catalog.resolveSpecies("cobblemon", "vulpix", "Normal", true).orElseThrow();
        var alolan = catalog.resolveSpecies("cobblemon", "vulpix", "Alolan", false).orElseThrow();
        assertTrue(normal.types().contains("Fire"));
        assertTrue(alolan.types().contains("Ice"));
        assertTrue(catalog.resolveSpecies("cobblemon", "vulpix", "UnknownFutureForm", false).isEmpty());
        assertTrue(catalog.resolveSpecies("othermod", "vulpix", "Normal", true).isEmpty());
        assertEquals("nidoranf", PtuDataCatalog.key("Nidoran♀"));
        assertEquals("mrmime", PtuDataCatalog.key("Mr. Mime"));
    }
    @Test void corruptBundleFailsRatherThanUsingWrongRules() {
        assertThrows(IllegalStateException.class, () -> PtuDataCatalog.load(name -> {
            if (name.equals("moves.json")) return new ByteArrayInputStream("[]".getBytes(StandardCharsets.UTF_8));
            return getClass().getResourceAsStream("/data/autoptu/ptu/" + name);
        }));
    }
    @Test void mapsActualLoadoutButDoesNotGrantIneligibleMoves() {
        var uuid = UUID.randomUUID();
        var input = new PtuPokemonBinding.Input(uuid, "cobblemon", "bulbasaur", "Normal", true, 5,
                List.of("tackle", "solarbeam", "madeupmove"), "overgrow");
        var binding = PtuPokemonBinding.resolve(catalog, input);
        assertEquals(uuid, binding.pokemonId());
        assertEquals(PtuPokemonBinding.Learning.LEVEL_UP, binding.equippedMoves().getFirst().learning());
        assertNotEquals(PtuPokemonBinding.Learning.LEVEL_UP, binding.equippedMoves().get(1).learning());
        assertEquals(PtuPokemonBinding.Learning.MISSING_DATA, binding.equippedMoves().get(2).learning());
        // The bundled PTU Bulbasaur pool is Confidence/Photosynthesis, not native Overgrow.
        assertEquals(PtuPokemonBinding.AbilityMatch.NOT_IN_POOL, binding.nativeAbility().match());
        assertEquals(3, binding.equippedMoves().size());
        assertEquals(catalog.revision(), binding.catalogRevision());
    }
    @Test void unknownSpeciesDoesNotGetDefaultStatsOrAbilities() {
        var binding = PtuPokemonBinding.resolve(catalog, new PtuPokemonBinding.Input(UUID.randomUUID(),
                "cobblemon", "unknown", "Normal", true, 5, List.of("tackle"), "overgrow"));
        assertNull(binding.species());
        assertEquals(PtuPokemonBinding.Learning.NOT_IN_LEARNSET, binding.equippedMoves().getFirst().learning());
        assertEquals(PtuPokemonBinding.AbilityMatch.NOT_IN_POOL, binding.nativeAbility().match());
        assertFalse(binding.issues().isEmpty());
    }
    @Test void doesNotActivateAllSpeciesAbilities() {
        var binding = PtuPokemonBinding.resolve(catalog, new PtuPokemonBinding.Input(UUID.randomUUID(),
                "cobblemon", "bulbasaur", "Normal", true, 100, List.of(), "chlorophyll"));
        assertNotEquals(PtuPokemonBinding.AbilityMatch.BASIC, binding.nativeAbility().match());
        assertTrue(binding.abilityPools().basic().size() > 0);
    }

    @Test void everyCanonicalCsvMoveRetainsOracleDamageAccuracyFrequencyAndEffects() throws Exception {
        try (var stream = getClass().getResourceAsStream("/data/autoptu/ptu/move_oracle.json")) {
            assertNotNull(stream);
            var rows = com.google.gson.JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray();
            // Python's CSV dictionary uses the last row for repeated exact names.
            Map<String, com.google.gson.JsonObject> oracle = new TreeMap<>();
            for (var element : rows) {
                var row = element.getAsJsonObject();
                String name = row.get("name").getAsString().strip();
                if (!name.isBlank()) oracle.put(PtuDataCatalog.key(name), row);
            }
            int checked = 0;
            for (var entry : oracle.entrySet()) {
                var row = entry.getValue();
                String category = row.get("category").getAsString().strip();
                if (!Set.of("Physical", "Special", "Status").contains(category)) continue;
                var move = catalog.move(entry.getKey()).orElseThrow(() -> new AssertionError(entry.getKey()));
                assertEquals(category, move.category(), entry.getKey());
                assertEquals(row.get("frequency").getAsString().strip(), move.frequency(), entry.getKey());
                assertEquals(row.get("effects").getAsString().strip(), move.effects(), entry.getKey());
                assertEquals(row.get("range").getAsString().strip(), move.range(), entry.getKey());
                String db = row.get("damage_base").getAsString().strip();
                if (db.matches("[0-9]+")) assertEquals(Integer.parseInt(db), move.damageBase(), entry.getKey());
                String ac = row.get("ac").getAsString().strip();
                if (ac.matches("[0-9]+")) assertEquals(Integer.parseInt(ac), move.ac(), entry.getKey());
                checked++;
            }
            assertTrue(checked > 800);
        }
    }

    @Test void derivedSheetRoundTripsWithoutLosingPtuFields() {
        var binding = PtuPokemonBinding.resolve(catalog, new PtuPokemonBinding.Input(UUID.randomUUID(),
                "cobblemon", "bulbasaur", "Normal", true, 5, List.of("tackle"), "overgrow"));
        var gson = new com.google.gson.Gson();
        assertEquals(binding, gson.fromJson(gson.toJson(binding), PtuPokemonBinding.class));
        assertFalse(binding.species().movementJson().isBlank());
        assertFalse(binding.species().skillsJson().isBlank());
    }

    @Test void abilityDefinitionsKeepCsvTriggerEffectAndFrequency() throws Exception {
        try (var stream = getClass().getResourceAsStream("/data/autoptu/ptu/abilities.json")) {
            var rows = com.google.gson.JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray();
            int checked = 0;
            for (var value : rows) {
                var row = value.getAsJsonObject();
                String name = row.get("Name").getAsString().strip();
                if (name.isBlank()) continue;
                var ability = catalog.ability(name);
                if (ability.isEmpty()) {
                    assertTrue(catalog.diagnostics().contains("Ambiguous ability:" + PtuDataCatalog.key(name)), name);
                    continue;
                }
                assertEquals(row.get("Effect").getAsString().strip(), ability.get().effect(), name);
                assertEquals(row.get("Frequency").getAsString().strip(), ability.get().frequency(), name);
                assertEquals(row.get("Trigger").getAsString().strip(), ability.get().trigger(), name);
                checked++;
            }
            assertTrue(checked > 300);
        }
    }

    @Test void refreshUsesNewLevelAndSpeciesWithoutReusingStaleStats() {
        UUID id = UUID.randomUUID();
        var initial = PtuPokemonBinding.resolve(catalog, new PtuPokemonBinding.Input(id,
                "cobblemon", "bulbasaur", "Normal", true, 5, List.of("solarbeam"), "overgrow"));
        var evolved = PtuPokemonBinding.resolve(catalog, new PtuPokemonBinding.Input(id,
                "cobblemon", "ivysaur", "Normal", true, 50, List.of("solarbeam"), "overgrow"));
        assertEquals(initial.pokemonId(), evolved.pokemonId());
        assertNotEquals(initial.species().id(), evolved.species().id());
        assertNotEquals(initial.species().baseStats(), evolved.species().baseStats());
        assertEquals(50, evolved.level());
    }
}
