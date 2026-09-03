package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import io.autoptu.cobblemon.authority.CanonicalWorldMapCatalogue;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/** Applies the canonical Marea migration timeline to the existing visible-wild actors. */
public final class MareaWildMigrationRuntime implements ModInitializer {
    private static final String POPULATION_ID = CanonicalWildPopulationCatalogue.MAREA_LOWER_SHELF_POPULATION_ID;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> reconcile(server.getOverworld()));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % MareaVisibleWildPokemonRuntime.presenceReconcileIntervalTicks() != 0) return;
            reconcile(server.getOverworld());
        });
    }

    static int reconcile(ServerWorld world) {
        if (world == null || world.getServer() == null || world != world.getServer().getOverworld()) return 0;
        if (!FabricCanonicalPlayerStoreRuntime.storesAvailable(world.getServer())) return 0;

        var population = CanonicalWildPopulationCatalogue.DEFAULT.population(POPULATION_ID)
                .orElseThrow(() -> new IllegalStateException("missing migrating Marea population: " + POPULATION_ID));
        var projectedSiteId = MareaWildMigrationProjection.projectedSiteId(population, world.getTime());
        if (projectedSiteId.isEmpty()) {
            return setMembersActive(world, population, false, null);
        }

        var site = CanonicalWorldMapCatalogue.DEFAULT.site(projectedSiteId.get())
                .orElseThrow(() -> new IllegalStateException("missing migration projection site: " + projectedSiteId.get()));
        boolean active = hasPlayerInside(world, site.x(), site.y(), site.z(), population.presenceFootprint());
        if (!active) return setMembersActive(world, population, false, null);

        int visible = 0;
        for (var encounter : CanonicalWildPopulationCatalogue.DEFAULT.members(population)) {
            PokemonEntity actor = loadedActor(world, encounter.canonicalEncounterId());
            if (actor == null) actor = MareaVisibleWildPokemonRuntime.ensureProjected(world, encounter);
            if (actor == null) continue;

            BlockPos anchor = new BlockPos(site.x(), site.y(), site.z()).add(
                    encounter.presentationOffsetX(), encounter.presentationOffsetY(), encounter.presentationOffsetZ());
            double centerX = anchor.getX() + 0.5D;
            double centerZ = anchor.getZ() + 0.5D;
            double dx = actor.getX() - centerX;
            double dz = actor.getZ() - centerZ;
            int leash = population.habitatLeashRadiusBlocks();
            if (dx * dx + dz * dz > (double) leash * leash) {
                actor.requestTeleport(centerX, anchor.getY(), centerZ);
            }
            actor.setInvisible(false);
            VisibleWildPokemonEncounterRuntime.setInteractionActive(actor.getUuid(), true);
            visible++;
        }
        return visible;
    }

    private static int setMembersActive(
            ServerWorld world,
            CanonicalWildPopulationCatalogue.PopulationDefinition population,
            boolean active,
            BlockPos ignored
    ) {
        int visible = 0;
        for (var encounter : CanonicalWildPopulationCatalogue.DEFAULT.members(population)) {
            PokemonEntity actor = loadedActor(world, encounter.canonicalEncounterId());
            if (actor == null) continue;
            actor.setInvisible(!active);
            VisibleWildPokemonEncounterRuntime.setInteractionActive(actor.getUuid(), active);
            if (active) visible++;
        }
        return visible;
    }

    private static PokemonEntity loadedActor(ServerWorld world, String encounterId) {
        var bound = VisibleWildPokemonEncounterRuntime.boundEntityUuid(encounterId);
        if (bound.isEmpty()) return null;
        var entity = world.getEntity(bound.get());
        return entity instanceof PokemonEntity pokemon && !pokemon.isRemoved() ? pokemon : null;
    }

    private static boolean hasPlayerInside(
            ServerWorld world,
            int x,
            int y,
            int z,
            CanonicalWildPopulationCatalogue.PresenceFootprint footprint
    ) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isSpectator()) continue;
            double dx = player.getX() - (x + 0.5D);
            double dy = player.getY() - y;
            double dz = player.getZ() - (z + 0.5D);
            if (footprint.containsOffset(dx, dy, dz)) return true;
        }
        return false;
    }
}
