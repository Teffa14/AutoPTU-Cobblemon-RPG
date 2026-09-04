package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Recovers Minecraft-native CALM navigation that stops making physical progress.
 *
 * The collision steering runtime can ask Minecraft navigation to route an already-authoritative
 * visible Marea actor around world geometry. This companion observes only those canonical roaming
 * actors after ambient/steering updates. If an active navigation route remains effectively stationary
 * for several samples, it revokes the stale route so the next steering pass can recalculate or use
 * the deterministic leash-safe detour fallback.
 *
 * This is world presentation only. It never reads Cobblemon Pokemon gameplay state and never decides
 * PTU movement legality, range, targets, initiative, RNG, damage, statuses or battle outcomes.
 */
public final class MareaWildCalmNavigationRecoveryRuntime implements ModInitializer {
    private static final int UPDATE_INTERVAL_TICKS = 10;
    private static final int STALLED_SAMPLE_LIMIT = 3;
    private static final double MIN_PROGRESS_BLOCKS = 0.15D;
    private static final double MIN_PROGRESS_SQUARED = MIN_PROGRESS_BLOCKS * MIN_PROGRESS_BLOCKS;
    private static final Map<MinecraftServer, Map<UUID, NavigationProgress>> PROGRESS = new IdentityHashMap<>();

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % UPDATE_INTERVAL_TICKS != 0) return;
            recover(server);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            synchronized (PROGRESS) {
                PROGRESS.remove(server);
            }
        });
    }

    static void recover(MinecraftServer server) {
        if (server == null) return;
        ServerWorld world = server.getOverworld();
        Map<UUID, NavigationProgress> progress = progressFor(server);
        HashSet<UUID> liveActors = new HashSet<>();

        for (var population : CanonicalWildPopulationCatalogue.DEFAULT.populations()) {
            if (!population.siteId().startsWith("ouros.marea.")) continue;
            var projectedSiteId = MareaWildMigrationProjection.projectedSiteId(population, world.getTime());
            if (projectedSiteId.isEmpty()) continue;

            for (var encounter : CanonicalWildPopulationCatalogue.DEFAULT.members(population)) {
                var boundUuid = VisibleWildPokemonEncounterRuntime.boundEntityUuid(encounter.canonicalEncounterId());
                if (boundUuid.isEmpty()) continue;
                var loaded = world.getEntity(boundUuid.get());
                if (!(loaded instanceof PokemonEntity actor) || actor.isRemoved() || actor.isInvisible()) continue;
                if (!VisibleWildPokemonEncounterRuntime.isInteractionActive(actor.getUuid())) continue;

                liveActors.add(actor.getUuid());
                var navigation = actor.getNavigation();
                if (navigation.isIdle()) {
                    progress.remove(actor.getUuid());
                    continue;
                }

                NavigationProgress previous = progress.get(actor.getUuid());
                NavigationProgress next = sample(previous, actor.getX(), actor.getZ());
                if (next.stalledSamples() >= STALLED_SAMPLE_LIMIT) {
                    navigation.stop();
                    var velocity = actor.getVelocity();
                    actor.setVelocity(0.0D, velocity.y, 0.0D);
                    actor.velocityModified = true;
                    progress.remove(actor.getUuid());
                    continue;
                }
                progress.put(actor.getUuid(), next);
            }
        }

        progress.keySet().removeIf(uuid -> !liveActors.contains(uuid));
    }

    static NavigationProgress sample(NavigationProgress previous, double currentX, double currentZ) {
        if (!Double.isFinite(currentX) || !Double.isFinite(currentZ)) {
            throw new IllegalArgumentException("navigation progress requires finite coordinates");
        }
        if (previous == null) return new NavigationProgress(currentX, currentZ, 0);

        double dx = currentX - previous.x();
        double dz = currentZ - previous.z();
        double progressSquared = dx * dx + dz * dz;
        int stalled = progressSquared < MIN_PROGRESS_SQUARED ? previous.stalledSamples() + 1 : 0;
        return new NavigationProgress(currentX, currentZ, stalled);
    }

    static boolean shouldRecover(NavigationProgress progress) {
        return progress != null && progress.stalledSamples() >= STALLED_SAMPLE_LIMIT;
    }

    private static Map<UUID, NavigationProgress> progressFor(MinecraftServer server) {
        synchronized (PROGRESS) {
            return PROGRESS.computeIfAbsent(server, ignored -> new HashMap<>());
        }
    }

    record NavigationProgress(double x, double z, int stalledSamples) {
        NavigationProgress {
            if (!Double.isFinite(x) || !Double.isFinite(z) || stalledSamples < 0) {
                throw new IllegalArgumentException("navigation progress requires finite coordinates and non-negative samples");
            }
        }
    }
}
