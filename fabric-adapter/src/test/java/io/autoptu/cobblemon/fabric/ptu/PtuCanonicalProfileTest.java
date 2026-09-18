package io.autoptu.cobblemon.fabric.ptu;

import com.google.gson.Gson;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class PtuCanonicalProfileTest {
    private static PtuDataCatalog catalog;
    private static final Gson JSON = new Gson();
    @BeforeAll static void catalog() { catalog = PtuDataCatalog.bundled(); }
    private PtuPokemonBinding binding(UUID id, String species, int level, List<String> moves) {
        return PtuPokemonBinding.resolve(catalog, new PtuPokemonBinding.Input(id, "cobblemon", species, "Normal", true, level, moves, "overgrow"));
    }
    @Test void createsRealCoreProfileWithNaturePointsAndSelectedAbilities() {
        var result = PtuCanonicalProfile.resolve(null, binding(UUID.randomUUID(), "bulbasaur", 5, List.of("tackle")), catalog, () -> 42);
        assertEquals("ready", result.view().status());
        var profile = result.view().profile();
        assertNotNull(result.newPersistentJson());
        assertEquals(15, profile.creation().allocation().total());
        assertEquals(1, profile.creation().abilities().size());
        assertTrue(List.of("Confidence", "Photosynthesis").contains(profile.creation().abilities().getFirst()));
        assertEquals(5 + 10 + 3 * profile.creation().finalStats().hp(), profile.creation().baseMaximumHp());
        assertEquals(List.of("tackle"), profile.allocationInputs().stream().map(PtuCanonicalProfile.BuildMove::id).toList());
    }
    @Test void repeatedViewsPersistenceReloadAndMoveChangesDoNotReroll() {
        UUID id = UUID.randomUUID();
        var initial = PtuCanonicalProfile.resolve(null, binding(id, "bulbasaur", 5, List.of("tackle")), catalog, () -> 7);
        for (int i = 0; i < 5; i++) {
            var restored = PtuCanonicalProfile.resolve(initial.newPersistentJson(), binding(id, "bulbasaur", 5, List.of()), catalog,
                    () -> { throw new AssertionError("Must not consume RNG on reload"); });
            assertEquals(initial.view().profile(), restored.view().profile());
            assertNull(restored.newPersistentJson());
        }
    }
    @Test void foreignUuidCannotReuseOrOverwriteProfile() {
        var initial = PtuCanonicalProfile.resolve(null, binding(UUID.randomUUID(), "bulbasaur", 5, List.of()), catalog, () -> 42);
        var restored = PtuCanonicalProfile.resolve(initial.newPersistentJson(), binding(UUID.randomUUID(), "bulbasaur", 5, List.of()), catalog,
                () -> { throw new AssertionError("Foreign UUID must not reroll"); });
        assertEquals("invalid", restored.view().status()); assertNull(restored.newPersistentJson());
    }
    @Test void malformedOrNullSavedDataIsNeverTreatedAsNew() {
        var binding = binding(UUID.randomUUID(), "bulbasaur", 5, List.of());
        for (String raw : List.of("", "null", "{}", "{broken", "x".repeat(48_001))) {
            var result = PtuCanonicalProfile.resolve(raw, binding, catalog, () -> { throw new AssertionError("No reroll"); });
            assertEquals("invalid", result.view().status()); assertNull(result.newPersistentJson());
        }
    }
    @Test void unknownSpeciesFailsClosedWithoutConsumingRandomness() {
        var result = PtuCanonicalProfile.resolve(null, binding(UUID.randomUUID(), "unknown", 5, List.of()), catalog,
                () -> { throw new AssertionError("No RNG for unknown species"); });
        assertEquals("missing_species", result.view().status()); assertNull(result.newPersistentJson());
    }
    @Test void evolutionAndNativeLevelsRequireExplicitReconciliation() {
        UUID id = UUID.randomUUID();
        var initial = PtuCanonicalProfile.resolve(null, binding(id, "bulbasaur", 5, List.of()), catalog, () -> 42);
        var leveled = PtuCanonicalProfile.resolve(initial.newPersistentJson(), binding(id, "bulbasaur", 6, List.of()), catalog, () -> 1);
        var evolved = PtuCanonicalProfile.resolve(initial.newPersistentJson(), binding(id, "ivysaur", 5, List.of()), catalog, () -> 1);
        assertEquals("level_changed", leveled.view().status()); assertEquals("species_changed", evolved.view().status());
        assertEquals(initial.view().profile(), leveled.view().profile()); assertEquals(initial.view().profile(), evolved.view().profile());
        assertNull(leveled.newPersistentJson()); assertNull(evolved.newPersistentJson());
    }
    @Test void absentKeyConsumesOneSeedAndDoesNotUseUnlearnableNativeMoves() {
        var calls = new AtomicInteger();
        var result = PtuCanonicalProfile.resolve(null, binding(UUID.randomUUID(), "bulbasaur", 5, List.of("tackle", "solarbeam", "unknown")), catalog, calls::incrementAndGet);
        assertEquals(1, calls.get());
        assertEquals(List.of("tackle"), result.view().profile().allocationInputs().stream().map(PtuCanonicalProfile.BuildMove::id).toList());
    }
    @Test void loadedNatureCatalogContainsAllOracleRowsIncludingHpNatures() {
        assertEquals(36, catalog.natures().size());
        var cuddly = catalog.natures().stream().filter(value -> value.name().equals("Cuddly")).findFirst().orElseThrow();
        assertEquals(1, cuddly.modifiers().hp()); assertEquals(-2, cuddly.modifiers().attack());
        assertEquals(0, catalog.natures().stream().filter(value -> value.name().equals("Composed")).findFirst().orElseThrow().modifiers().total());
    }
    @Test void startingAbilityPoolsRetainSourceOrderRatherThanBeingDropped() throws Exception {
        try (var stream = getClass().getResourceAsStream("/data/autoptu/ptu/pools.json")) {
            var rows = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            var source = rows.getAsJsonObject("bulbasaur").getAsJsonArray("starting");
            assertEquals(java.util.stream.StreamSupport.stream(source.spliterator(), false).map(com.google.gson.JsonElement::getAsString).toList(), catalog.pools("bulbasaur").starting());
        }
    }
    @Test void changedCatalogOrCoreNeverSilentlyRebuildsPersistedStats() {
        var binding = binding(UUID.randomUUID(), "bulbasaur", 5, List.of());
        var initial = PtuCanonicalProfile.resolve(null, binding, catalog, () -> 42);
        for (String field : List.of("catalogRevision", "coreRevision")) {
            var json = com.google.gson.JsonParser.parseString(initial.newPersistentJson()).getAsJsonObject();
            json.addProperty(field, "previous-revision");
            var restored = PtuCanonicalProfile.resolve(JSON.toJson(json), binding, catalog, () -> { throw new AssertionError("No migration roll"); });
            assertEquals(field.equals("catalogRevision") ? "catalog_changed" : "core_changed", restored.view().status());
            assertEquals(initial.view().profile().creation(), restored.view().profile().creation());
            assertNull(restored.newPersistentJson());
        }
    }
    @Test void invalidSavedPointsAreRejectedRatherThanRepaired() {
        var binding = binding(UUID.randomUUID(), "bulbasaur", 5, List.of());
        var initial = PtuCanonicalProfile.resolve(null, binding, catalog, () -> 42);
        var json = com.google.gson.JsonParser.parseString(initial.newPersistentJson()).getAsJsonObject();
        json.getAsJsonObject("creation").getAsJsonObject("allocation").addProperty("hp", -1);
        var restored = PtuCanonicalProfile.resolve(JSON.toJson(json), binding, catalog, () -> { throw new AssertionError("No repair roll"); });
        assertEquals("invalid", restored.view().status()); assertNull(restored.newPersistentJson());
    }
    @Test void sheetSerializationRetainsSavedProfileAndDoesNotGrantMoves() {
        var binding = binding(UUID.randomUUID(), "bulbasaur", 5, List.of("tackle", "solarbeam"));
        var profile = PtuCanonicalProfile.resolve(null, binding, catalog, () -> 42);
        var sheet = binding.withCanonical(profile.view());
        assertEquals(sheet, JSON.fromJson(JSON.toJson(sheet), PtuPokemonBinding.class));
        assertEquals(binding.equippedMoves(), sheet.equippedMoves());
        assertEquals(binding.nativeAbility(), sheet.nativeAbility());
    }
}
