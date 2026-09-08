package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.authority.CanonicalWildEncounterCatalogue;
import io.autoptu.cobblemon.authority.CanonicalWorldMapCatalogue;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Region-agnostic recovery for persistent visible-wild presentation actors.
 *
 * <p>The canonical encounter binding owns identity. Minecraft chunk loading only recovers the
 * persisted presentation entity with that UUID; it never supplies PTU species, stats, HP, moves,
 * abilities, legality, RNG or outcomes.</p>
 */
public final class WildVisibleActorRecovery {
    private WildVisibleActorRecovery() {
    }

    public static PokemonEntity recoverBoundActor(
            ServerWorld world,
            CanonicalWildEncounterCatalogue.EncounterDefinition encounter
    ) {
        if (world == null) throw new IllegalArgumentException("world is required");
        if (encounter == null) throw new IllegalArgumentException("encounter is required");

        String encounterId = encounter.canonicalEncounterId();
        PokemonEntity actor = loadedActor(world, encounterId);
        if (actor != null) return actor;

        var boundUuid = VisibleWildPokemonEncounterRuntime.boundEntityUuid(encounterId);
        if (boundUuid.isEmpty()) return null;

        PokemonEntity presentation = VisibleWildPokemonEncounterRuntime.binding(boundUuid.get())
                .map(VisibleWildPokemonEncounterRuntime.Binding::presentationEntity)
                .orElse(null);
        if (presentation == null) return null;

        // During immediate hibernation/reactivation Minecraft can temporarily stop indexing the
        // presentation body while the entity itself is still live. Preserve that exact canonical
        // UUID instead of manufacturing a replacement merely because world.getEntity cannot see it.
        if (!presentation.isRemoved() && presentation.getWorld() == world) return presentation;

        var reason = presentation.getRemovalReason();
        if (reason != null && (reason.shouldDestroy() || reason == Entity.RemovalReason.CHANGED_DIMENSION)) {
            return null;
        }

        // Ordinary chunk unloads retain the canonical binding. Load only the presentation body's
        // last-known chunk so Minecraft can deserialize the same UUID. This avoids both the former
        // leash-wide chunk fan-out and replacement of a valid dormant actor.
        int chunkX = Math.floorDiv(presentation.getBlockX(), 16);
        int chunkZ = Math.floorDiv(presentation.getBlockZ(), 16);
        world.getChunk(chunkX, chunkZ);
        return loadedActor(world, encounterId);
    }

    public static BlockPos canonicalHomeAnchor(
            CanonicalWildEncounterCatalogue.EncounterDefinition encounter
    ) {
        if (encounter == null) throw new IllegalArgumentException("encounter is required");
        var site = CanonicalWorldMapCatalogue.DEFAULT.site(encounter.siteId())
                .orElseThrow(() -> new IllegalStateException(
                        "missing canonical wild encounter home site: " + encounter.siteId()));
        return new BlockPos(site.x(), site.y(), site.z()).add(
                encounter.presentationOffsetX(),
                encounter.presentationOffsetY(),
                encounter.presentationOffsetZ());
    }

    static PokemonEntity loadedActor(ServerWorld world, String encounterId) {
        var bound = VisibleWildPokemonEncounterRuntime.boundEntityUuid(encounterId);
        if (bound.isEmpty()) return null;
        var entity = world.getEntity(bound.get());
        return entity instanceof PokemonEntity pokemon && !pokemon.isRemoved() ? pokemon : null;
    }
}
