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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Global Minecraft-visible habitat feedback for every registered wild ecology population.
 *
 * Region/species content publishes projected actors through {@link WildEcologyProjectionRegistry}.
 * This runtime derives only presentation habitat presence and physical interaction-range feedback
 * from those server-authored projections. It never selects encounters, rolls RNG, reads Cobblemon
 * Pokemon gameplay payloads, or supplies PTU species/stats/moves/legality/results.
 */
public final class WildHabitatCueRuntime implements ModInitializer {
    private static final int UPDATE_INTERVAL_TICKS = 100;
    private static final int ENGAGEMENT_UPDATE_INTERVAL_TICKS = 10;
    private static final Map<MinecraftServer, Map<UUID, Map<String, HabitatSnapshot>>> INSIDE_POPULATIONS = new IdentityHashMap<>();
    private static final Map<MinecraftServer, Map<UUID, NearbyInteractionSnapshot>> NEARBY_INTERACTION_ACTORS = new IdentityHashMap<>();

    record HabitatCircle(double centerX, double centerZ, int radiusBlocks) {
        HabitatCircle {
            if (!Double.isFinite(centerX) || !Double.isFinite(centerZ)) throw new IllegalArgumentException("habitat center must be finite");
            if (radiusBlocks <= 0) throw new IllegalArgumentException("radiusBlocks must be positive");
        }
    }

    record HabitatCue(String populationKey, String displayName, List<HabitatCircle> circles, int visibleActors, int visibleAlphas) {
        HabitatCue {
            if (populationKey == null || populationKey.isBlank()) throw new IllegalArgumentException("populationKey is required");
            if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("displayName is required");
            circles = List.copyOf(circles);
            if (circles.isEmpty()) throw new IllegalArgumentException("at least one habitat circle is required");
            if (visibleActors <= 0) throw new IllegalArgumentException("visibleActors must be positive");
            if (visibleAlphas < 0 || visibleAlphas > visibleActors) throw new IllegalArgumentException("visibleAlphas must be within visible actor count");
        }

        HabitatCue(String populationKey, String displayName, List<HabitatCircle> circles, int visibleActors) {
            this(populationKey, displayName, circles, visibleActors, 0);
        }
    }

    record HabitatSnapshot(int visibleActors, int visibleAlphas) {
        HabitatSnapshot {
            if (visibleActors <= 0) throw new IllegalArgumentException("visibleActors must be positive");
            if (visibleAlphas < 0 || visibleAlphas > visibleActors) throw new IllegalArgumentException("visibleAlphas must be within visible actor count");
        }

        static HabitatSnapshot from(HabitatCue habitat) {
            return new HabitatSnapshot(habitat.visibleActors(), habitat.visibleAlphas());
        }
    }

    record NearbyInteractionSnapshot(
            UUID actorId,
            WildSocialRole socialRole,
            String speciesId,
            String habitatDisplayName,
            Optional<MigrationPhase> migrationPhase,
            boolean alphaPresentation
    ) {
        NearbyInteractionSnapshot {
            if (actorId == null) throw new IllegalArgumentException("actorId is required");
            if (socialRole == null) throw new IllegalArgumentException("socialRole is required");
            if (speciesId == null || speciesId.isBlank()) throw new IllegalArgumentException("speciesId is required");
            if (habitatDisplayName == null || habitatDisplayName.isBlank()) throw new IllegalArgumentException("habitatDisplayName is required");
            speciesId = speciesId.strip();
            habitatDisplayName = habitatDisplayName.strip();
            migrationPhase = migrationPhase == null ? Optional.empty() : migrationPhase;
        }

        NearbyInteractionSnapshot(
                UUID actorId,
                WildSocialRole socialRole,
                String speciesId,
                String habitatDisplayName,
                Optional<MigrationPhase> migrationPhase
        ) {
            this(actorId, socialRole, speciesId, habitatDisplayName, migrationPhase, socialRole == WildSocialRole.ALPHA);
        }

        NearbyInteractionSnapshot(UUID actorId, WildSocialRole socialRole, String speciesId, String habitatDisplayName) {
            this(actorId, socialRole, speciesId, habitatDisplayName, Optional.empty(), socialRole == WildSocialRole.ALPHA);
        }

        NearbyInteractionSnapshot(UUID actorId, WildSocialRole socialRole) {
            this(actorId, socialRole, "pokemon", "Wild habitat", Optional.empty(), socialRole == WildSocialRole.ALPHA);
        }
    }

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % ENGAGEMENT_UPDATE_INTERVAL_TICKS == 0) reconcileNearbyInteractions(server.getOverworld());
            if (server.getTicks() % UPDATE_INTERVAL_TICKS == 0) reconcile(server.getOverworld());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            synchronized (INSIDE_POPULATIONS) {
                INSIDE_POPULATIONS.remove(server);
            }
            synchronized (NEARBY_INTERACTION_ACTORS) {
                NEARBY_INTERACTION_ACTORS.remove(server);
            }
        });
    }

    static void reconcile(ServerWorld world) {
        if (world == null || world.getServer() == null || world != world.getServer().getOverworld()) return;

        Map<String, HabitatCue> habitats = habitatCues(world);
        Set<UUID> online = new HashSet<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            online.add(player.getUuid());
            if (player.isSpectator()) {
                remember(world.getServer(), player.getUuid(), Map.of());
                continue;
            }

            Map<String, HabitatSnapshot> previous = remembered(world.getServer(), player.getUuid());
            Map<String, HabitatSnapshot> current = new HashMap<>();
            for (HabitatCue habitat : habitats.values()) {
                if (!containsHorizontal(player.getX(), player.getZ(), habitat)) continue;
                HabitatSnapshot snapshot = HabitatSnapshot.from(habitat);
                current.put(habitat.populationKey(), snapshot);
                if (shouldAnnounce(previous.get(habitat.populationKey()), habitat)) announce(player, habitat);
            }
            remember(world.getServer(), player.getUuid(), current);
        }
        forgetOffline(world.getServer(), online);
    }

    static void reconcileNearbyInteractions(ServerWorld world) {
        if (world == null || world.getServer() == null || world != world.getServer().getOverworld()) return;

        List<WildEcologyProjectionRegistry.ProjectedActor> projections = WildEcologyProjectionRegistry.collect(world);
        Set<UUID> online = new HashSet<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            UUID playerId = player.getUuid();
            online.add(playerId);
            if (player.isSpectator()) {
                rememberNearbyInteraction(world.getServer(), playerId, null);
                continue;
            }

            NearbyInteractionSnapshot previousActor = rememberedNearbyInteraction(world.getServer(), playerId);
            NearbyInteractionSnapshot currentActor = nearestInteractionActor(player, projections);
            rememberNearbyInteraction(world.getServer(), playerId, currentActor);
            if (shouldAnnounceNearbyInteraction(previousActor, currentActor)) {
                player.sendMessage(Text.literal(nearbyInteractionText(currentActor)), true);
            }
        }
        forgetOfflineNearbyInteractions(world.getServer(), online);
    }

    static NearbyInteractionSnapshot nearestInteractionActor(
            ServerPlayerEntity player,
            List<WildEcologyProjectionRegistry.ProjectedActor> projections) {
        if (player == null || projections == null || projections.isEmpty()) return null;

        MinecraftServer server = player.getServer();
        long worldTick = player.getServerWorld().getTime();
        NearbyInteractionSnapshot remembered = server == null
                ? null
                : rememberedNearbyInteraction(server, player.getUuid());
        if (remembered != null) {
            WildEcologyProjectionRegistry.ProjectedActor retained = projections.stream()
                    .filter(candidate -> candidate != null && !candidate.actor().isRemoved())
                    .filter(candidate -> candidate.actor().getUuid().equals(remembered.actorId()))
                    .filter(candidate -> VisibleWildPokemonEncounterRuntime.isInteractionActive(candidate.actor().getUuid()))
                    .filter(candidate -> VisibleWildPokemonEncounterRuntime.isEligibleInteractionTarget(player, candidate.actor()))
                    .findFirst()
                    .orElse(null);
            NearbyInteractionSnapshot retainedSnapshot = interactionSnapshot(retained, worldTick);
            if (retainedSnapshot != null) return retainedSnapshot;
        }

        WildEcologyProjectionRegistry.ProjectedActor projection = projections.stream()
                .filter(candidate -> candidate != null && !candidate.actor().isRemoved())
                .filter(candidate -> VisibleWildPokemonEncounterRuntime.isInteractionActive(candidate.actor().getUuid()))
                .filter(candidate -> VisibleWildPokemonEncounterRuntime.isEligibleInteractionTarget(player, candidate.actor()))
                .min(Comparator
                        .comparingDouble((WildEcologyProjectionRegistry.ProjectedActor candidate) ->
                                player.squaredDistanceTo(candidate.actor()))
                        .thenComparing(candidate -> candidate.actor().getUuid().toString()))
                .orElse(null);
        return interactionSnapshot(projection, worldTick);
    }

    static NearbyInteractionSnapshot interactionSnapshot(
            WildEcologyProjectionRegistry.ProjectedActor projection,
            long worldTick
    ) {
        if (projection == null) return null;
        if (worldTick < 0L) throw new IllegalArgumentException("worldTick must be >= 0");
        var binding = VisibleWildPokemonEncounterRuntime.binding(projection.actor().getUuid()).orElse(null);
        if (binding == null) return null;
        var encounter = CanonicalWildEncounterCatalogue.DEFAULT.encounter(binding.canonicalEncounterId()).orElse(null);
        if (encounter == null) return null;
        var population = CanonicalWildPopulationCatalogue.DEFAULT.population(encounter.populationId()).orElse(null);
        Optional<MigrationPhase> migrationPhase = population == null
                ? Optional.empty()
                : WildEcologyDescriptorRegistry.descriptorFor(population)
                        .flatMap(descriptor -> descriptor.projectionPhase(population, worldTick));
        return new NearbyInteractionSnapshot(
                projection.actor().getUuid(),
                projection.socialRole(),
                encounter.speciesId(),
                projection.habitatDisplayName(),
                migrationPhase,
                projection.presentationCapabilities().nativeAlphaVisual());
    }

    static boolean shouldAnnounceNearbyInteraction(NearbyInteractionSnapshot previousActor, NearbyInteractionSnapshot currentActor) {
        return currentActor != null && !currentActor.equals(previousActor);
    }

    static String nearbyInteractionText(NearbyInteractionSnapshot interaction) {
        if (interaction == null) throw new IllegalArgumentException("interaction is required");
        String species = displaySpeciesName(interaction.speciesId());
        String identity = interaction.alphaPresentation() ? "Alpha " + species : species;
        String phase = interaction.migrationPhase()
                .map(value -> " · " + displayMigrationPhase(value))
                .orElse("");
        return identity + " · " + interaction.habitatDisplayName() + phase + " · interact to inspect encounter";
    }

    static String nearbyInteractionText(WildSocialRole socialRole) {
        return socialRole == WildSocialRole.ALPHA
                ? "Alpha wild Pokemon within reach · interact to inspect encounter"
                : "Wild Pokemon within reach · interact to inspect encounter";
    }

    static String displaySpeciesName(String speciesId) {
        if (speciesId == null || speciesId.isBlank()) throw new IllegalArgumentException("speciesId is required");
        String normalized = speciesId.strip();
        int namespace = normalized.lastIndexOf(':');
        if (namespace >= 0 && namespace + 1 < normalized.length()) normalized = normalized.substring(namespace + 1);
        StringBuilder display = new StringBuilder(normalized.length());
        boolean capitalize = true;
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (c == '_' || c == '-') {
                if (display.length() > 0 && display.charAt(display.length() - 1) != ' ') display.append(' ');
                capitalize = true;
                continue;
            }
            display.append(capitalize ? Character.toUpperCase(c) : c);
            capitalize = false;
        }
        if (display.length() == 0) throw new IllegalArgumentException("speciesId must contain a species name");
        return display.toString();
    }

    static String displayMigrationPhase(MigrationPhase phase) {
        if (phase == null) throw new IllegalArgumentException("phase is required");
        String normalized = phase.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    static Map<String, HabitatCue> habitatCues(ServerWorld world) {
        Map<String, List<HabitatCircle>> circlesByPopulation = new LinkedHashMap<>();
        Map<String, Integer> countsByPopulation = new LinkedHashMap<>();
        Map<String, Integer> alphasByPopulation = new LinkedHashMap<>();
        Map<String, String> labelsByPopulation = new LinkedHashMap<>();
        for (var projection : WildEcologyProjectionRegistry.collect(world)) {
            if (!contributesToHabitatCue(VisibleWildPokemonEncounterRuntime.isInteractionActive(projection.actor().getUuid()))) {
                continue;
            }
            circlesByPopulation.computeIfAbsent(projection.populationKey(), ignored -> new ArrayList<>())
                    .add(new HabitatCircle(
                            projection.habitatCenterX(),
                            projection.habitatCenterZ(),
                            projection.habitatLeashRadiusBlocks()));
            countsByPopulation.merge(projection.populationKey(), 1, Integer::sum);
            if (projection.presentationCapabilities().nativeAlphaVisual()) {
                alphasByPopulation.merge(projection.populationKey(), 1, Integer::sum);
            }
            String previousLabel = labelsByPopulation.putIfAbsent(projection.populationKey(), projection.habitatDisplayName());
            if (previousLabel != null && !previousLabel.equals(projection.habitatDisplayName())) {
                throw new IllegalStateException("inconsistent habitat display name for population: " + projection.populationKey());
            }
        }

        Map<String, HabitatCue> habitats = new LinkedHashMap<>();
        for (var entry : circlesByPopulation.entrySet()) {
            habitats.put(entry.getKey(), new HabitatCue(
                    entry.getKey(),
                    labelsByPopulation.getOrDefault(entry.getKey(), entry.getKey()),
                    entry.getValue(),
                    countsByPopulation.getOrDefault(entry.getKey(), 0),
                    alphasByPopulation.getOrDefault(entry.getKey(), 0)));
        }
        return Map.copyOf(habitats);
    }

    static boolean contributesToHabitatCue(boolean interactionActive) {
        return interactionActive;
    }

    static boolean containsHorizontal(double playerX, double playerZ, HabitatCue habitat) {
        for (HabitatCircle circle : habitat.circles()) {
            double dx = playerX - circle.centerX();
            double dz = playerZ - circle.centerZ();
            double radius = circle.radiusBlocks();
            if (dx * dx + dz * dz <= radius * radius) return true;
        }
        return false;
    }

    static boolean shouldAnnounce(HabitatSnapshot previous, HabitatCue current) {
        return previous == null || !previous.equals(HabitatSnapshot.from(current));
    }

    static String announcementText(HabitatCue habitat) {
        String herd = habitat.visibleActors() == 1
                ? "1 roaming Pokemon"
                : habitat.visibleActors() + " roaming Pokemon";
        String alpha = habitat.visibleAlphas() == 1
                ? " · Alpha present"
                : habitat.visibleAlphas() > 1 ? " · " + habitat.visibleAlphas() + " authored Alphas present" : "";
        return "Wild habitat — " + habitat.displayName() + " · " + herd + alpha;
    }

    private static void announce(ServerPlayerEntity player, HabitatCue habitat) {
        player.sendMessage(Text.literal(announcementText(habitat)), true);
    }

    private static Map<String, HabitatSnapshot> remembered(MinecraftServer server, UUID playerId) {
        synchronized (INSIDE_POPULATIONS) {
            Map<UUID, Map<String, HabitatSnapshot>> players = INSIDE_POPULATIONS.get(server);
            if (players == null) return Map.of();
            Map<String, HabitatSnapshot> populations = players.get(playerId);
            return populations == null ? Map.of() : Map.copyOf(populations);
        }
    }

    private static void remember(MinecraftServer server, UUID playerId, Map<String, HabitatSnapshot> populations) {
        synchronized (INSIDE_POPULATIONS) {
            Map<UUID, Map<String, HabitatSnapshot>> players = INSIDE_POPULATIONS.computeIfAbsent(server, ignored -> new HashMap<>());
            if (populations.isEmpty()) players.remove(playerId);
            else players.put(playerId, Map.copyOf(populations));
            if (players.isEmpty()) INSIDE_POPULATIONS.remove(server);
        }
    }

    private static void forgetOffline(MinecraftServer server, Set<UUID> online) {
        synchronized (INSIDE_POPULATIONS) {
            Map<UUID, Map<String, HabitatSnapshot>> players = INSIDE_POPULATIONS.get(server);
            if (players == null) return;
            players.keySet().removeIf(playerId -> !online.contains(playerId));
            if (players.isEmpty()) INSIDE_POPULATIONS.remove(server);
        }
    }

    private static NearbyInteractionSnapshot rememberedNearbyInteraction(MinecraftServer server, UUID playerId) {
        synchronized (NEARBY_INTERACTION_ACTORS) {
            Map<UUID, NearbyInteractionSnapshot> players = NEARBY_INTERACTION_ACTORS.get(server);
            return players == null ? null : players.get(playerId);
        }
    }

    private static void rememberNearbyInteraction(MinecraftServer server, UUID playerId, NearbyInteractionSnapshot actor) {
        synchronized (NEARBY_INTERACTION_ACTORS) {
            Map<UUID, NearbyInteractionSnapshot> players = NEARBY_INTERACTION_ACTORS.computeIfAbsent(server, ignored -> new HashMap<>());
            if (actor == null) players.remove(playerId);
            else players.put(playerId, actor);
            if (players.isEmpty()) NEARBY_INTERACTION_ACTORS.remove(server);
        }
    }

    private static void forgetOfflineNearbyInteractions(MinecraftServer server, Set<UUID> online) {
        synchronized (NEARBY_INTERACTION_ACTORS) {
            Map<UUID, NearbyInteractionSnapshot> players = NEARBY_INTERACTION_ACTORS.get(server);
            if (players == null) return;
            players.keySet().removeIf(playerId -> !online.contains(playerId));
            if (players.isEmpty()) NEARBY_INTERACTION_ACTORS.remove(server);
        }
    }
}
