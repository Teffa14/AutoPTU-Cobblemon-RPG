package io.autoptu.cobblemon.fabric.ptu;

import java.util.Objects;
import java.util.UUID;

/** Provenance of the real native object; origin never claims a native capture was PTU. */
public record PtuLifecycleRecord(int schema, UUID pokemonId, Origin origin, UUID firstTrainer) {
    public enum Origin { EXISTING, ACQUIRED, STARTER, NATIVE_CAPTURE }
    public PtuLifecycleRecord {
        if (schema != 1) throw new IllegalArgumentException("Unknown lifecycle schema");
        Objects.requireNonNull(pokemonId); Objects.requireNonNull(origin);
    }
    public static PtuLifecycleRecord observe(PtuLifecycleRecord previous, UUID pokemonId, Origin origin, UUID trainer) {
        if (previous == null) return new PtuLifecycleRecord(1, pokemonId, origin, trainer);
        if (!previous.pokemonId().equals(pokemonId)) throw new IllegalArgumentException("Lifecycle UUID mismatch");
        // Loading, gaining, moving or trading an existing Pokémon must never rewrite its creation history.
        boolean provisional = previous.origin() == Origin.EXISTING || previous.origin() == Origin.ACQUIRED;
        boolean specific = origin == Origin.STARTER || origin == Origin.NATIVE_CAPTURE;
        if (provisional && specific) return new PtuLifecycleRecord(1, pokemonId, origin, trainer);
        return previous;
    }
}
