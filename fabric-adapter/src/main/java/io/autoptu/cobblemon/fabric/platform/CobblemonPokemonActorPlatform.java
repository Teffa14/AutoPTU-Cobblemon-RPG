package io.autoptu.cobblemon.fabric.platform;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.Objects;

/**
 * Narrow Cobblemon presentation-actor boundary for the RPG integration.
 *
 * <p>This is deliberately the only place used by normal Marea provisioning that knows how the
 * current Cobblemon runtime resolves an official species and constructs a {@link PokemonEntity}.
 * WORLD/RPG code supplies the server-authored species identity and Minecraft position and never
 * reads Pokemon gameplay state back through this bridge. When Cobblemon changes constructors,
 * registries, entity lookup or spawn plumbing, version migration belongs here rather than in the
 * canonical population/encounter services.</p>
 */
public final class CobblemonPokemonActorPlatform {
    private CobblemonPokemonActorPlatform() {}

    /**
     * Creates an unspawned persistent presentation actor from a server-authored official species ID.
     * No level, HP, moves, abilities, statuses, ownership, battle state or spawn outcome is imported
     * from Cobblemon as RPG truth.
     */
    public static PokemonEntity createOfficialPresentationActor(
            ServerWorld world,
            String speciesId,
            BlockPos anchor,
            float yaw
    ) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(anchor, "anchor");
        String normalizedSpeciesId = requireIdentifier(speciesId, "speciesId");

        Species species = PokemonSpecies.INSTANCE.getByName(normalizedSpeciesId);
        if (species == null) {
            throw new IllegalStateException(
                    "Cobblemon official species unavailable for presentation actor: " + normalizedSpeciesId);
        }

        Pokemon pokemon = new Pokemon();
        pokemon.setSpecies(species);
        PokemonEntity entity = new PokemonEntity(world, pokemon, CobblemonEntities.POKEMON);
        entity.refreshPositionAndAngles(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D, yaw, 0.0F);
        entity.setPersistent();
        return entity;
    }

    /** Finds an already-loaded presentation actor using only Minecraft entity/tag state. */
    public static PokemonEntity findLoadedByCommandTag(ServerWorld world, Box bounds, String commandTag) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(bounds, "bounds");
        String normalizedTag = requireIdentifier(commandTag, "commandTag");
        return world.getEntitiesByClass(
                        PokemonEntity.class,
                        bounds,
                        entity -> !entity.isRemoved() && entity.getCommandTags().contains(normalizedTag))
                .stream()
                .findFirst()
                .orElse(null);
    }

    public static String runtimeActorClassName() {
        return PokemonEntity.class.getName();
    }

    static String requireIdentifier(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}
