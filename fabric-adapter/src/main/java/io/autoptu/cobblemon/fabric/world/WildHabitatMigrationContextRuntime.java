package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.authority.CanonicalWildEncounterCatalogue;
import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import io.autoptu.cobblemon.ecology.MigrationPhase;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Surfaces authored migration context to players inside the projected WILD habitat.
 *
 * <p>This is Minecraft world context only. Phase, population identity and habitat geometry come from
 * server-owned canonical ecology bindings. Cobblemon remains presentation-only and no PTU battle
 * legality, movement, RNG, damage, status, ability, item or outcome is derived here.</p>
 */
public final class WildHabitatMigrationContextRuntime implements ModInitializer {
    private static final int UPDATE_INTERVAL_TICKS = 20;
    private static final Map<MinecraftServer, Map<UUID, Map<String, HabitatMigrationContext>>> OBSERVED_CONTEXTS =
            new IdentityHashMap<>();
    private static final Map<MinecraftServer, Map<UUID, Map<String, HabitatMigrationContext>>> TRANSIT_CONTEXTS =
            new IdentityHashMap<>();

    record HabitatCircle(double centerX, double centerZ, int radiusBlocks) {
        HabitatCircle {
            if (!Double.isFinite(centerX) || !Double.isFinite(centerZ)) {
                throw new IllegalArgumentException("habitat center must be finite");
            }
            if (radiusBlocks <= 0) throw new IllegalArgumentException("radiusBlocks must be positive");
        }
    }

    record HabitatMigrationContext(
            String populationId,
            String habitatDisplayName,
            MigrationPhase phase,
            List<HabitatCircle> circles
    ) {
        HabitatMigrationContext {
            if (populationId == null || populationId.isBlank()) throw new IllegalArgumentException("populationId is required");
            if (habitatDisplayName == null || habitatDisplayName.isBlank()) {
                throw new IllegalArgumentException("habitatDisplayName is required");
            }
            if (phase == null) throw new IllegalArgumentException("phase is required");
            circles = List.copyOf(circles);
            if (circles.isEmpty()) throw new IllegalArgumentException("at least one habitat circle is required");
            populationId = populationId.strip();
            habitatDisplayName = habitatDisplayName.strip();
        }

        HabitatMigrationContext withPhase(MigrationPhase replacementPhase) {
            return new HabitatMigrationContext(populationId, habitatDisplayName, replacementPhase, circles);
        }
    }

    private record MutableContext(
            String populationId,
            String habitatDisplayName,
            MigrationPhase phase,
            List<HabitatCircle> circles
    ) {}

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % UPDATE_INTERVAL_TICKS != 0) return;
            reconcile(server.getOverworld());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            synchronized (OBSERVED_CONTEXTS) {
                OBSERVED_CONTEXTS.remove(server);
            }
            synchronized (TRANSIT_CONTEXTS) {
                TRANSIT_CONTEXTS.remove(server);
            }
        });
    }

    static void reconcile(ServerWorld world) {
        if (world == null || world.getServer() == null || world != world.getServer().getOverworld()) return;

        Map<String, HabitatMigrationContext> contexts = migrationContexts(world);
        Set<UUID> online = new HashSet<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            UUID playerId = player.getUuid();
            online.add(playerId);
            if (player.isSpectator()) {
                remember(world.getServer(), playerId, Map.of());
                rememberTransit(world.getServer(), playerId, Map.of());
                continue;
            }

            Map<String, HabitatMigrationContext> previous = remembered(world.getServer(), playerId);
            Map<String, HabitatMigrationContext> transit = retainedTransit(world.getServer(), playerId);
            Map<String, HabitatMigrationContext> current = new HashMap<>();
            Map<String, HabitatMigrationContext> nextTransit = new HashMap<>();

            for (Map.Entry<String, HabitatMigrationContext> retained : transit.entrySet()) {
                MigrationPhase authoredPhase = authoredPhase(world, retained.getKey());
                if (shouldRetainTransitContext(authoredPhase)) nextTransit.put(retained.getKey(), retained.getValue());
            }

            for (HabitatMigrationContext context : contexts.values()) {
                if (!containsHorizontal(player.getX(), player.getZ(), context)) continue;
                current.put(context.populationId(), context);
                HabitatMigrationContext previousContext = previous.get(context.populationId());
                HabitatMigrationContext transitContext = transit.get(context.populationId());
                MigrationPhase previousPhase = previousContext != null
                        ? previousContext.phase()
                        : transitContext == null ? null : transitContext.phase();
                if (shouldAnnounceEntry(previousPhase, context.phase())) {
                    player.sendMessage(Text.literal(entryContextText(context)), true);
                } else if (shouldAnnounce(previousPhase, context.phase())) {
                    player.sendMessage(Text.literal(announcementText(context)), true);
                }
                nextTransit.remove(context.populationId());
            }
            for (Map.Entry<String, HabitatMigrationContext> observed : previous.entrySet()) {
                if (current.containsKey(observed.getKey())) continue;
                if (contexts.containsKey(observed.getKey())) {
                    player.sendMessage(Text.literal(departureContextText(observed.getValue())), true);
                    continue;
                }
                MigrationPhase authoredPhase = authoredPhase(world, observed.getKey());
                if (shouldAnnounceUnprojected(observed.getValue().phase(), authoredPhase)) {
                    player.sendMessage(Text.literal(unprojectedMigrationText(observed.getValue(), authoredPhase)), true);
                    nextTransit.put(observed.getKey(), observed.getValue().withPhase(authoredPhase));
                }
            }
            remember(world.getServer(), playerId, current);
            rememberTransit(world.getServer(), playerId, nextTransit);
        }
        forgetOffline(world.getServer(), online);
    }

    static Map<String, HabitatMigrationContext> migrationContexts(ServerWorld world) {
        if (world == null) return Map.of();
        Map<String, MutableContext> mutable = new LinkedHashMap<>();
        long worldTick = world.getTime();

        for (var projection : WildEcologyProjectionRegistry.collect(world)) {
            if (!contributesToMigrationContext(
                    VisibleWildPokemonEncounterRuntime.isInteractionActive(projection.actor().getUuid()))) {
                continue;
            }
            var binding = VisibleWildPokemonEncounterRuntime.binding(projection.actor().getUuid()).orElse(null);
            if (binding == null) continue;
            CanonicalWildEncounterCatalogue.EncounterDefinition encounter =
                    CanonicalWildEncounterCatalogue.DEFAULT.encounter(binding.canonicalEncounterId()).orElse(null);
            if (encounter == null) continue;
            CanonicalWildPopulationCatalogue.PopulationDefinition population =
                    CanonicalWildPopulationCatalogue.DEFAULT.population(encounter.populationId()).orElse(null);
            if (population == null) continue;
            var descriptor = WildEcologyDescriptorRegistry.descriptorFor(population).orElse(null);
            if (descriptor == null || !descriptor.worldEligibility().accepts(world)) continue;
            MigrationPhase phase = descriptor.projectionPhase(population, worldTick).orElse(null);
            if (phase == null) continue;

            HabitatCircle circle = new HabitatCircle(
                    projection.habitatCenterX(),
                    projection.habitatCenterZ(),
                    projection.habitatLeashRadiusBlocks());
            MutableContext existing = mutable.get(population.populationId());
            if (existing == null) {
                List<HabitatCircle> circles = new ArrayList<>();
                circles.add(circle);
                mutable.put(population.populationId(), new MutableContext(
                        population.populationId(), projection.habitatDisplayName(), phase, circles));
                continue;
            }
            if (!existing.habitatDisplayName().equals(projection.habitatDisplayName())) {
                throw new IllegalStateException("inconsistent habitat display name for population: " + population.populationId());
            }
            if (existing.phase() != phase) {
                throw new IllegalStateException("inconsistent migration phase for population: " + population.populationId());
            }
            existing.circles().add(circle);
        }

        Map<String, HabitatMigrationContext> result = new LinkedHashMap<>();
        for (MutableContext context : mutable.values()) {
            result.put(context.populationId(), new HabitatMigrationContext(
                    context.populationId(), context.habitatDisplayName(), context.phase(), context.circles()));
        }
        return Map.copyOf(result);
    }

    static boolean contributesToMigrationContext(boolean interactionActive) {
        return interactionActive;
    }

    static MigrationPhase authoredPhase(ServerWorld world, String populationId) {
        if (world == null || populationId == null || populationId.isBlank()) return null;
        CanonicalWildPopulationCatalogue.PopulationDefinition population =
                CanonicalWildPopulationCatalogue.DEFAULT.population(populationId).orElse(null);
        if (population == null) return null;
        var descriptor = WildEcologyDescriptorRegistry.descriptorFor(population).orElse(null);
        if (descriptor == null || !descriptor.worldEligibility().accepts(world)) return null;
        return descriptor.projectionPhase(population, world.getTime()).orElse(null);
    }

    static boolean containsHorizontal(double playerX, double playerZ, HabitatMigrationContext context) {
        for (HabitatCircle circle : context.circles()) {
            double dx = playerX - circle.centerX();
            double dz = playerZ - circle.centerZ();
            double radius = circle.radiusBlocks();
            if (dx * dx + dz * dz <= radius * radius) return true;
        }
        return false;
    }

    static boolean shouldAnnounceEntry(MigrationPhase previous, MigrationPhase current) {
        return previous == null && current != null;
    }

    static boolean shouldAnnounce(MigrationPhase previous, MigrationPhase current) {
        return previous != null && current != null && previous != current;
    }

    static boolean shouldAnnounceUnprojected(MigrationPhase previous, MigrationPhase current) {
        return previous != null && previous != MigrationPhase.IN_TRANSIT && current == MigrationPhase.IN_TRANSIT;
    }

    static boolean shouldRetainTransitContext(MigrationPhase phase) {
        return phase == MigrationPhase.IN_TRANSIT;
    }

    static String entryContextText(HabitatMigrationContext context) {
        if (context == null) throw new IllegalArgumentException("context is required");
        return "Wild habitat — "
                + context.habitatDisplayName()
                + " · "
                + WildHabitatCueRuntime.displayMigrationPhase(context.phase());
    }

    static String departureContextText(HabitatMigrationContext context) {
        if (context == null) throw new IllegalArgumentException("context is required");
        return "Wild habitat no longer nearby — " + context.habitatDisplayName();
    }

    static String unprojectedMigrationText(HabitatMigrationContext context, MigrationPhase phase) {
        if (context == null) throw new IllegalArgumentException("context is required");
        if (phase == null) throw new IllegalArgumentException("phase is required");
        return "Wild habitat migration — "
                + context.habitatDisplayName()
                + " · "
                + WildHabitatCueRuntime.displayMigrationPhase(phase);
    }

    static String announcementText(HabitatMigrationContext context) {
        if (context == null) throw new IllegalArgumentException("context is required");
        return "Wild habitat migration — "
                + context.habitatDisplayName()
                + " · "
                + WildHabitatCueRuntime.displayMigrationPhase(context.phase());
    }

    private static Map<String, HabitatMigrationContext> remembered(MinecraftServer server, UUID playerId) {
        synchronized (OBSERVED_CONTEXTS) {
            Map<UUID, Map<String, HabitatMigrationContext>> players = OBSERVED_CONTEXTS.get(server);
            if (players == null) return Map.of();
            Map<String, HabitatMigrationContext> contexts = players.get(playerId);
            return contexts == null ? Map.of() : Map.copyOf(contexts);
        }
    }

    private static Map<String, HabitatMigrationContext> retainedTransit(MinecraftServer server, UUID playerId) {
        synchronized (TRANSIT_CONTEXTS) {
            Map<UUID, Map<String, HabitatMigrationContext>> players = TRANSIT_CONTEXTS.get(server);
            if (players == null) return Map.of();
            Map<String, HabitatMigrationContext> contexts = players.get(playerId);
            return contexts == null ? Map.of() : Map.copyOf(contexts);
        }
    }

    private static void remember(MinecraftServer server, UUID playerId, Map<String, HabitatMigrationContext> contexts) {
        rememberIn(OBSERVED_CONTEXTS, server, playerId, contexts);
    }

    private static void rememberTransit(MinecraftServer server, UUID playerId, Map<String, HabitatMigrationContext> contexts) {
        rememberIn(TRANSIT_CONTEXTS, server, playerId, contexts);
    }

    private static void rememberIn(
            Map<MinecraftServer, Map<UUID, Map<String, HabitatMigrationContext>>> store,
            MinecraftServer server,
            UUID playerId,
            Map<String, HabitatMigrationContext> contexts
    ) {
        synchronized (store) {
            Map<UUID, Map<String, HabitatMigrationContext>> players =
                    store.computeIfAbsent(server, ignored -> new HashMap<>());
            if (contexts.isEmpty()) players.remove(playerId);
            else players.put(playerId, Map.copyOf(contexts));
            if (players.isEmpty()) store.remove(server);
        }
    }

    private static void forgetOffline(MinecraftServer server, Set<UUID> online) {
        forgetOfflineIn(OBSERVED_CONTEXTS, server, online);
        forgetOfflineIn(TRANSIT_CONTEXTS, server, online);
    }

    private static void forgetOfflineIn(
            Map<MinecraftServer, Map<UUID, Map<String, HabitatMigrationContext>>> store,
            MinecraftServer server,
            Set<UUID> online
    ) {
        synchronized (store) {
            Map<UUID, Map<String, HabitatMigrationContext>> players = store.get(server);
            if (players == null) return;
            players.keySet().removeIf(playerId -> !online.contains(playerId));
            if (players.isEmpty()) store.remove(server);
        }
    }
}
