package io.autoptu.cobblemon.fabric.battle;

import io.autoptu.cobblemon.authority.CanonicalWildEncounterCatalogue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MareaCanonicalWildEncounterBlueprintSourceTest {
    @Test
    void firstMareaFletchlingResolvesAsCompleteAlreadyAuthoredBlueprint() {
        var source = new MareaCanonicalWildEncounterBlueprintSource();
        var blueprint = source.resolve(CanonicalWildEncounterCatalogue.MAREA_FIRST_FLETCHLING_ID)
                .orElseThrow();

        assertEquals(CanonicalWildEncounterCatalogue.MAREA_FIRST_FLETCHLING_ID, blueprint.canonicalEncounterId());
        assertEquals(1, blueprint.side());
        assertEquals(1, blueprint.pokemon().size());

        var pokemon = blueprint.pokemon().getFirst();
        assertEquals("fletchling", pokemon.speciesId());
        assertEquals(5, pokemon.level());
        assertEquals(39, pokemon.health().currentHp());
        assertEquals(39, pokemon.health().maxHp());
        assertEquals(List.of("tackle", "growl"), pokemon.moveLoadout().moveIds());
        assertEquals(List.of("normal", "flying"), pokemon.battleTraits().types());
        assertEquals(List.of("big-pecks"), pokemon.battleTraits().abilities());
        assertEquals(3, pokemon.baseMovement().overland());
        assertEquals(5, pokemon.baseMovement().sky());
        assertTrue(pokemon.statusState().entries().isEmpty());
        assertEquals(0, pokemon.injuryState().injuries());
    }

    @Test
    void createOnlyWorldRegistryReceivesExactBlueprintBeforeAnyActorIdentityExists() {
        var source = new MareaCanonicalWildEncounterBlueprintSource();
        var registry = new WorldScopedCanonicalWildEncounterBlueprintRegistry();
        var publisher = new ServerOwnedWildEncounterBlueprintPublisher(source, registry);

        assertTrue(publisher.publish(CanonicalWildEncounterCatalogue.MAREA_FIRST_FLETCHLING_ID));
        var published = registry.resolve(CanonicalWildEncounterCatalogue.MAREA_FIRST_FLETCHLING_ID)
                .orElseThrow();
        assertEquals(
                source.resolve(CanonicalWildEncounterCatalogue.MAREA_FIRST_FLETCHLING_ID).orElseThrow(),
                published
        );

        assertThrows(IllegalStateException.class,
                () -> publisher.publish(CanonicalWildEncounterCatalogue.MAREA_FIRST_FLETCHLING_ID));
    }
}
