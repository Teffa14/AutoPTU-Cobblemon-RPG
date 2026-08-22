package io.autoptu.cobblemon.fabric.battle;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CobblemonCanonicalEncounterIdentityRegistryTest {
    @Test
    void rejectsDuplicateCanonicalCombatantIdsWithinOneParticipantRegistration() {
        CobblemonCanonicalEncounterIdentityRegistry registry = new CobblemonCanonicalEncounterIdentityRegistry();
        LinkedHashMap<String, String> mappings = new LinkedHashMap<>();
        mappings.put("external-a", "canonical-shared");
        mappings.put("external-b", "canonical-shared");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> registry.register(
                CobblemonBattleStartInterceptor.ParticipantKind.WILD,
                "actor",
                "wild-pack",
                mappings
        ));

        assertEquals("duplicate canonical combatant identity", error.getMessage());
        assertEquals(0, registry.registeredParticipantCount());
    }
}
