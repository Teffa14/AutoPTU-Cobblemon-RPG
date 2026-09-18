package io.autoptu.cobblemon.fabric.ptu;

import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.Gson;
import java.util.UUID;
import org.slf4j.LoggerFactory;

/** Observe Cobblemon's existing lifecycle without a second starter grant, roster or capture result. */
public final class PtuNativeLifecycleRuntime {
    public static final String KEY = "autoptu:ptu_lifecycle_v1";
    private static final Gson JSON = new Gson();
    private PtuNativeLifecycleRuntime() {}

    public static void register() {
        CobblemonEvents.STARTER_CHOSEN.subscribe(event -> {
            if (event.isCanceled()) return;
            attach(event.getPokemon(), PtuLifecycleRecord.Origin.STARTER, event.getPlayer().getUuid());
        });
        CobblemonEvents.POKEMON_CAPTURED.subscribe(event ->
                attach(event.getPokemon(), PtuLifecycleRecord.Origin.NATIVE_CAPTURE, event.getPlayer().getUuid()));
        CobblemonEvents.POKEMON_GAINED.subscribe(event ->
                attach(event.getPokemon(), PtuLifecycleRecord.Origin.ACQUIRED, event.getPlayerId()));
    }

    public static void attach(Pokemon pokemon, PtuLifecycleRecord.Origin origin, UUID trainer) {
        // Resolve before persisting provenance: no new Pokémon, duplicated grants or synthetic moves.
        PtuPokemonDataRuntime.bind(pokemon);
        var data = pokemon.getPersistentData();
        PtuLifecycleRecord old = null;
        String raw = data.getString(KEY);
        try {
            if (!raw.isBlank()) old = JSON.fromJson(raw, PtuLifecycleRecord.class);
            var next = PtuLifecycleRecord.observe(old, pokemon.getUuid(), origin, trainer);
            String encoded = JSON.toJson(next);
            if (encoded.equals(raw)) return;
            data.putString(KEY, encoded);
            var coordinates = pokemon.getStoreCoordinates().get();
            if (coordinates != null) coordinates.getStore().onPokemonChanged(pokemon);
        } catch (IllegalArgumentException | com.google.gson.JsonParseException error) {
            // Preserve corrupt/foreign history for recovery instead of silently resetting it.
            LoggerFactory.getLogger(PtuNativeLifecycleRuntime.class).warn("Invalid PTU lifecycle record for {}: {}", pokemon.getUuid(), error.getMessage());
        }
    }

    public static String origin(Pokemon pokemon) {
        try {
            var value = JSON.fromJson(pokemon.getPersistentData().getString(KEY), PtuLifecycleRecord.class);
            if (value == null) return "existing";
            if (!value.pokemonId().equals(pokemon.getUuid())) return "invalid";
            return value.origin().name().toLowerCase(java.util.Locale.ROOT);
        } catch (RuntimeException error) { return "invalid"; }
    }
}
