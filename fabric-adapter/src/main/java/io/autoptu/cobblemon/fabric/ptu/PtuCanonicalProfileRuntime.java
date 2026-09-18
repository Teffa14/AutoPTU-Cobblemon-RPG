package io.autoptu.cobblemon.fabric.ptu;

import com.cobblemon.mod.common.pokemon.Pokemon;
import java.security.SecureRandom;

/** Server-side persistence of an immutable core-generated profile on the same Pokémon UUID. */
public final class PtuCanonicalProfileRuntime {
    public static final String KEY = "autoptu:ptu_profile_v1";
    private static final SecureRandom SEEDS = new SecureRandom();
    private PtuCanonicalProfileRuntime() {}

    public static PtuCanonicalProfile.View ensure(Pokemon pokemon, PtuPokemonBinding binding) {
        var data = pokemon.getPersistentData();
        // Wrong NBT types count as corrupt, not absent. Never reroll a malformed saved profile.
        String existing = data.contains(KEY) ? data.getString(KEY) : null;
        try {
            var result = PtuCanonicalProfile.resolve(existing, binding, PtuPokemonDataRuntime.catalog(), SEEDS::nextLong);
            if (result.newPersistentJson() != null) {
                data.putString(KEY, result.newPersistentJson());
                var coordinates = pokemon.getStoreCoordinates().get();
                if (coordinates != null) coordinates.getStore().onPokemonChanged(pokemon);
            }
            return result.view();
        } catch (RuntimeException missingData) {
            org.slf4j.LoggerFactory.getLogger(PtuCanonicalProfileRuntime.class)
                    .warn("Cannot initialize PTU profile for {}: {}", pokemon.getUuid(), missingData.getMessage());
            return new PtuCanonicalProfile.View("missing_data", null);
        }
    }
}
