package io.autoptu.cobblemon.fabric.presentation;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.entity.Entity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CobblemonPokemonEntityLookupTest {
    @Test
    void parsesOnlyUuidPresentationIdentity() {
        UUID id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        assertEquals(id, CobblemonPokemonEntityLookup.presentationUuid("  " + id + "  "));
        assertThrows(IllegalArgumentException.class,
                () -> CobblemonPokemonEntityLookup.presentationUuid("pokemon-one"));
        assertThrows(IllegalArgumentException.class,
                () -> CobblemonPokemonEntityLookup.presentationUuid(" "));
    }

    @Test
    void compilesAgainstRealCobblemonPokemonEntityType() {
        assertTrue(Entity.class.isAssignableFrom(PokemonEntity.class));
        assertEquals("com.cobblemon.mod.common.entity.pokemon.PokemonEntity", PokemonEntity.class.getName());
    }
}
