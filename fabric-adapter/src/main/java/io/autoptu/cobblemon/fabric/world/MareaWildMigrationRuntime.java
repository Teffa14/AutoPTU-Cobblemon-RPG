package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.authority.CanonicalWildEncounterCatalogue;
import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import io.autoptu.cobblemon.authority.CanonicalWorldMapCatalogue;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.IdentityHashMap;
import java.util.Map;

/** Compatibility runtime for existing migration fixtures; normal population lifecycle is global. */
public final class MareaWildMigrationRuntime implements ModInitializer {
    private static final String POPULATION_ID = CanonicalWildPopulationCatalogue.MAREA_LOWER_SHELF_POPULATION_ID;
    private static final Map<MinecraftServer, Boolean> ACTIVE_MIGRATION_PROJECTION = new IdentityHashMap<>();

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> reconcile(server.getOverworld()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ACTIVE_MIGRATION_PROJECTION.remove(server));
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
        var source = WildPopulationContentRegistry.sourceFor(population)
                .orElseThrow(() -> new IllegalStateException("missing registered Marea population source: " + POPULATION_ID));
        var projectedSiteId = source.projectedSiteResolver().projectedSiteId(population, world.getTime());
        if (projectedSiteId.isEmpty()) {
            setProjectionMarkedActive(world.getServer(), false);
            return setMembersActive(world, population, false);
        }

        var site = CanonicalWorldMapCatalogue.DEFAULT.site(projectedSiteId.get())
                .orElseThrow(() -> new IllegalStateException("missing migration projection site: " + projectedSiteId.get()));
        boolean wasActive = isProjectionMarkedActive(world.getServer());
        boolean active = hasPlayerInside(
                world,
                site.x(),
                site.y(),
                site.z(),
                activityFootprint(population, wasActive)
        );
        setProjectionMarkedActive(world.getServer(), active);
        if (!active) return setMembersActive(world, population, false);

        int visible = 0;
        for (var encounter : CanonicalWildPopulationCatalogue.DEFAULT.members(population)) {
            PokemonEntity actor = recoverBoundActor(world, encounter);
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

    static PokemonEntity recoverBoundActor(
            ServerWorld world,
            CanonicalWildEncounterCatalogue.EncounterDefinition encounter
    ) {
        return WildVisibleActorRecovery.recoverBoundActor(world, encounter);
    }

    static BlockPos canonicalHomeAnchor(CanonicalWildEncounterCatalogue.EncounterDefinition encounter) {
        return WildVisibleActorRecovery.canonicalHomeAnchor(encounter);
    }

    static CanonicalWildPopulationCatalogue.PresenceFootprint activityFootprint(
            CanonicalWildPopulationCatalogue.PopulationDefinition population,
            boolean wasActive
    ) {
        if (population == null) throw new IllegalArgumentException("population is required");
        return wasActive ? population.retentionFootprint() : population.presenceFootprint();
    }

    private static boolean isProjectionMarkedActive(MinecraftServer server) {
        synchronized (ACTIVE_MIGRATION_PROJECTION) {
            return Boolean.TRUE.equals(ACTIVE_MIGRATION_PROJECTION.get(server));
        }
    }

    private static void setProjectionMarkedActive(MinecraftServer server, boolean active) {
        synchronized (ACTIVE_MIGRATION_PROJECTION) {
            if (active) ACTIVE_MIGRATION_PROJECTION.put(server, true);
            else ACTIVE_MIGRATION_PROJECTION.remove(server);
        }
    }

    private static int setMembersActive(
            ServerWorld world,
            CanonicalWildPopulationCatalogue.PopulationDefinition population,
            boolean active
    ) {
        int visible = 0;
        for (var encounter : CanonicalWildPopulationCatalogue.DEFAULT.members(population)) {
            PokemonEntity actor = WildVisibleActorRecovery.loadedActor(world, encounter.canonicalEncounterId());
            if (actor == null) continue;
            actor.setInvisible(!active);
            VisibleWildPokemonEncounterRuntime.setInteractionActive(actor.getUuid(), active);
            if (active) visible++;
        }
        return visible;
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
