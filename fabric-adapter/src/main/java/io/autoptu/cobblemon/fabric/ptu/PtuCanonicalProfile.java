package io.autoptu.cobblemon.fabric.ptu;

import com.google.gson.Gson;
import io.autoptu.core.pokemon.PokemonCreation;
import io.autoptu.core.random.PythonRandom;
import java.util.*;
import java.util.function.LongSupplier;

/** Immutable creation snapshot, independent of native HP/IV/EV/nature/ability mechanics.
 * Progression and battle HP are intentionally not synthesized from later native changes.
 */
public record PtuCanonicalProfile(int schema, UUID pokemonId, String speciesId, String catalogRevision,
                                  String coreRevision, long creationSeed, List<BuildMove> allocationInputs,
                                  PokemonCreation.Result creation) {
    public static final String CORE_REVISION = "ca7bb78abfd93ac608af4917033722698226e677";
    private static final Gson JSON = new Gson();
    public record BuildMove(String id, PokemonCreation.MoveProfile profile) {}
    public record View(String status, PtuCanonicalProfile profile) {}
    public record Resolution(View view, String newPersistentJson) {}

    public PtuCanonicalProfile {
        if (schema != 1) throw new IllegalArgumentException("Unknown profile schema");
        Objects.requireNonNull(pokemonId); Objects.requireNonNull(speciesId); Objects.requireNonNull(catalogRevision);
        Objects.requireNonNull(coreRevision); Objects.requireNonNull(creation);
        allocationInputs = List.copyOf(allocationInputs);
        if (speciesId.isBlank() || catalogRevision.isBlank() || coreRevision.isBlank()) throw new IllegalArgumentException("Profile provenance missing");
        if (creation.level() < 1 || creation.level() > 100 || creation.nature() == null || creation.nature().isBlank()
                || !PokemonCreation.POLICY.equals(creation.allocationPolicy())) throw new IllegalArgumentException("Invalid creation contract");
        if (creation.allocation().total() != creation.level() + 10) throw new IllegalArgumentException("Invalid allocation total");
        for (int value : creation.allocation().values()) if (value < 0) throw new IllegalArgumentException("Negative allocation");
        for (int value : creation.finalStats().values()) if (value < 1 || value > 1200) throw new IllegalArgumentException("Invalid final stat");
        if (creation.baseMaximumHp() != creation.level() + 3 * creation.finalStats().hp() + 10) throw new IllegalArgumentException("Invalid base HP");
        if (allocationInputs.size() > 6 || creation.abilities().size() > 3) throw new IllegalArgumentException("Oversized creation state");
    }

    /** Only a truly absent key may initialize. Corrupt, null and mismatched records are preserved. */
    public static Resolution resolve(String existing, PtuPokemonBinding binding, PtuDataCatalog catalog, LongSupplier seeds) {
        if (existing != null) {
            try {
                if (existing.isBlank() || existing.length() > 48_000) return unchanged("invalid", null);
                var saved = JSON.fromJson(existing, PtuCanonicalProfile.class);
                if (saved == null || !saved.pokemonId().equals(binding.pokemonId())) return unchanged("invalid", null);
                if (binding.species() == null || !saved.speciesId().equals(binding.species().id())) return unchanged("species_changed", saved);
                if (saved.creation().level() != binding.level()) return unchanged("level_changed", saved);
                if (!saved.catalogRevision().equals(catalog.revision())) return unchanged("catalog_changed", saved);
                if (!saved.coreRevision().equals(CORE_REVISION)) return unchanged("core_changed", saved);
                return unchanged("ready", saved);
            } catch (RuntimeException invalid) { return unchanged("invalid", null); }
        }
        if (binding.species() == null) return unchanged("missing_species", null);
        var species = binding.species();
        List<BuildMove> inputs = binding.equippedMoves().stream()
                .filter(move -> move.learning() == PtuPokemonBinding.Learning.LEVEL_UP && move.data() != null)
                .map(move -> new BuildMove(move.data().id(), new PokemonCreation.MoveProfile(
                        move.data().category(), move.data().damageBase(), move.data().frequency()))).toList();
        var stats = species.baseStats();
        var base = new PokemonCreation.Stats(stats.get("hp"), stats.get("attack"), stats.get("defense"),
                stats.get("special_attack"), stats.get("special_defense"), stats.get("speed"));
        var pools = binding.abilityPools();
        long seed = seeds.getAsLong();
        var result = PokemonCreation.create(binding.level(), base, inputs.stream().map(BuildMove::profile).toList(), catalog.natures(),
                new PokemonCreation.AbilityPools(pools.starting(), pools.basic(), pools.advanced(), pools.high()), new PythonRandom(seed));
        var saved = new PtuCanonicalProfile(1, binding.pokemonId(), species.id(), catalog.revision(), CORE_REVISION, seed, inputs, result);
        return new Resolution(new View("ready", saved), JSON.toJson(saved));
    }
    private static Resolution unchanged(String status, PtuCanonicalProfile saved) {
        return new Resolution(new View(status, saved), null);
    }
}
