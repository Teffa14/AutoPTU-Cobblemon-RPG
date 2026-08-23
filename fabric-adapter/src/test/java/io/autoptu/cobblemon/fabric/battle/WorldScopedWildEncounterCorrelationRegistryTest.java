package io.autoptu.cobblemon.fabric.battle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldScopedWildEncounterCorrelationRegistryTest {
    @Test
    void storesOnlyCanonicalEncounterToOpaqueActorCorrelation() {
        WorldScopedWildEncounterCorrelationRegistry registry = new WorldScopedWildEncounterCorrelationRegistry();

        registry.register("ouros:forest:encounter-4", "opaque-wild-uuid");

        assertEquals("ouros:forest:encounter-4", registry.resolveCanonicalEncounterId("opaque-wild-uuid").orElseThrow());
        assertEquals(1, registry.size());
    }

    @Test
    void rejectsActorAndEncounterAliasing() {
        WorldScopedWildEncounterCorrelationRegistry registry = new WorldScopedWildEncounterCorrelationRegistry();
        registry.register("ouros:forest:encounter-4", "opaque-wild-a");

        assertThrows(IllegalStateException.class,
                () -> registry.register("ouros:forest:encounter-5", "opaque-wild-a"));
        assertThrows(IllegalStateException.class,
                () -> registry.register("ouros:forest:encounter-4", "opaque-wild-b"));
        assertThrows(IllegalStateException.class,
                () -> registry.register("ouros:forest:encounter-4", "opaque-wild-a"));
    }

    @Test
    void removalClearsBothDirections() {
        WorldScopedWildEncounterCorrelationRegistry registry = new WorldScopedWildEncounterCorrelationRegistry();
        registry.register("ouros:forest:encounter-4", "opaque-wild-a");

        assertTrue(registry.removeByExternalActor("opaque-wild-a"));
        assertTrue(registry.resolveCanonicalEncounterId("opaque-wild-a").isEmpty());
        registry.register("ouros:forest:encounter-4", "opaque-wild-b");
        assertEquals("ouros:forest:encounter-4", registry.resolveCanonicalEncounterId("opaque-wild-b").orElseThrow());
    }
}
