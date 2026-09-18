package io.autoptu.cobblemon.fabric.ptu;

import java.util.*;

/** Data attachment for one actual Pokémon UUID; never a replacement Pokémon or native battle state. */
public record PtuPokemonBinding(int schema, UUID pokemonId, String catalogRevision, String nativeSpecies,
                                String nativeForm, int level, PtuDataCatalog.Species species,
                                List<BoundMove> equippedMoves, PtuDataCatalog.Pools abilityPools,
                                BoundAbility nativeAbility, List<String> issues) {
    public record Input(UUID pokemonId, String namespace, String species, String form, boolean standardForm,
                        int level, List<String> moves, String ability) {
        public Input {
            Objects.requireNonNull(pokemonId); Objects.requireNonNull(namespace); Objects.requireNonNull(species);
            Objects.requireNonNull(form); Objects.requireNonNull(ability);
            moves = List.copyOf(moves);
            if (level < 1 || level > 100) throw new IllegalArgumentException("Invalid Pokémon level");
        }
    }
    public enum Learning { LEVEL_UP, REQUIRES_UNLOCK, ABOVE_LEVEL, NOT_IN_LEARNSET, MISSING_DATA }
    public enum AbilityMatch { BASIC, ADVANCED_REQUIRES_UNLOCK, HIGH_REQUIRES_UNLOCK, NOT_IN_POOL, MISSING_DATA }
    public record BoundMove(String nativeId, PtuDataCatalog.Move data, Learning learning) {}
    public record BoundAbility(String nativeId, PtuDataCatalog.Ability data, AbilityMatch match) {}

    public PtuPokemonBinding {
        equippedMoves = List.copyOf(equippedMoves); issues = List.copyOf(issues);
    }
    public static PtuPokemonBinding resolve(PtuDataCatalog catalog, Input input) {
        List<String> issues = new ArrayList<>();
        var species = catalog.resolveSpecies(input.namespace(), input.species(), input.form(), input.standardForm()).orElse(null);
        if (species == null) issues.add("UNRESOLVED_SPECIES_FORM:" + input.namespace() + ":" + input.species() + "/" + input.form());
        String id = species == null ? "" : species.id();
        var learnset = catalog.learnset(id);
        List<BoundMove> moves = new ArrayList<>();
        for (String nativeMove : input.moves()) {
            var data = catalog.move(nativeMove).orElse(null);
            Learning learning = Learning.NOT_IN_LEARNSET;
            if (data == null) learning = Learning.MISSING_DATA;
            else {
                var entries = learnset.stream().filter(entry -> PtuDataCatalog.key(entry.move()).equals(data.id())).toList();
                // Level zero means TM/tutor/egg or unspecified acquisition, never an automatic grant.
                if (entries.stream().anyMatch(entry -> entry.level() > 0 && entry.level() <= input.level())) learning = Learning.LEVEL_UP;
                else if (entries.stream().anyMatch(entry -> entry.level() == 0)) learning = Learning.REQUIRES_UNLOCK;
                else if (!entries.isEmpty()) learning = Learning.ABOVE_LEVEL;
            }
            moves.add(new BoundMove(nativeMove, data, learning));
            if (learning != Learning.LEVEL_UP) issues.add("MOVE_" + learning + ":" + nativeMove);
        }
        var pools = catalog.pools(id);
        var ability = catalog.ability(input.ability()).orElse(null);
        AbilityMatch match;
        if (ability == null) match = AbilityMatch.MISSING_DATA;
        else if (contains(pools.basic(), ability.id())) match = AbilityMatch.BASIC;
        else if (contains(pools.advanced(), ability.id())) match = AbilityMatch.ADVANCED_REQUIRES_UNLOCK;
        else if (contains(pools.high(), ability.id())) match = AbilityMatch.HIGH_REQUIRES_UNLOCK;
        else match = AbilityMatch.NOT_IN_POOL;
        if (match != AbilityMatch.BASIC) issues.add("ABILITY_" + match + ":" + input.ability());
        for (String name : java.util.stream.Stream.of(pools.basic(), pools.advanced(), pools.high()).flatMap(List::stream).distinct().toList()) {
            if (catalog.ability(name).isEmpty()) issues.add("MISSING_POOL_ABILITY:" + name);
        }
        return new PtuPokemonBinding(1, input.pokemonId(), catalog.revision(), input.namespace() + ":" + input.species(),
                input.form(), input.level(), species, moves, pools, new BoundAbility(input.ability(), ability, match), issues);
    }
    private static boolean contains(List<String> names, String id) { return names.stream().map(PtuDataCatalog::key).anyMatch(id::equals); }
}
