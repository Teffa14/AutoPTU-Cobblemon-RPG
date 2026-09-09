package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.ecology.MigrationPhase;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Surfaces the visible population around the player's currently focused canonical WILD.
 *
 * <p>The count, explicitly requested Alpha presentation, projected habitat, group spread and optional migration
 * phase come only from server-owned ecology projections and the authored descriptor behind the focused actor.
 * Cobblemon entities provide presentation identity and observed Minecraft geometry only. This runtime never
 * derives PTU legality, stats, HP, moves, RNG, statuses, capture, encounter outcomes or battle results.</p>
 */
public final class WildFocusedPopulationPresenceRuntime implements ModInitializer {
    private static final int UPDATE_INTERVAL_TICKS = 10;
    private static final Map<MinecraftServer, Map<UUID, PopulationPresence>> REMEMBERED = new IdentityHashMap<>();

    enum GroupSpread {
        ALONE("alone"),
        CLUSTERED("clustered"),
        COHESIVE("cohesive"),
        DISPERSED("spread out");

        private final String displayText;

        GroupSpread(String displayText) {
            this.displayText = displayText;
        }

        String displayText() {
            return displayText;
        }
    }

    record PopulationPresence(
            String populationKey,
            String speciesDisplayName,
            int visibleActors,
            int visibleAlphas,
            Optional<MigrationPhase> migrationPhase,
            Optional<String> habitatDisplayName,
            Optional<GroupSpread> groupSpread,
            Optional<WildSocialRole> focusedSocialRole
    ) {
        PopulationPresence {
            if (populationKey == null || populationKey.isBlank()) {
                throw new IllegalArgumentException("populationKey is required");
            }
            if (speciesDisplayName == null || speciesDisplayName.isBlank()) {
                throw new IllegalArgumentException("speciesDisplayName is required");
            }
            if (visibleActors <= 0) throw new IllegalArgumentException("visibleActors must be positive");
            if (visibleAlphas < 0 || visibleAlphas > visibleActors) {
                throw new IllegalArgumentException("visibleAlphas must be between zero and visibleActors");
            }
            populationKey = populationKey.strip();
            speciesDisplayName = speciesDisplayName.strip();
            migrationPhase = migrationPhase == null ? Optional.empty() : migrationPhase;
            habitatDisplayName = habitatDisplayName == null ? Optional.empty() : habitatDisplayName;
            groupSpread = groupSpread == null ? Optional.empty() : groupSpread;
            focusedSocialRole = focusedSocialRole == null ? Optional.empty() : focusedSocialRole;
            habitatDisplayName = habitatDisplayName.map(String::strip);
            if (habitatDisplayName.isPresent() && habitatDisplayName.get().isBlank()) {
                throw new IllegalArgumentException("habitatDisplayName must not be blank when present");
            }
        }

        PopulationPresence(
                String populationKey,
                String speciesDisplayName,
                int visibleActors,
                int visibleAlphas,
                Optional<MigrationPhase> migrationPhase,
                Optional<String> habitatDisplayName,
                Optional<GroupSpread> groupSpread
        ) {
            this(populationKey, speciesDisplayName, visibleActors, visibleAlphas, migrationPhase, habitatDisplayName,
                    groupSpread, Optional.empty());
        }

        PopulationPresence(
                String populationKey,
                String speciesDisplayName,
                int visibleActors,
                int visibleAlphas,
                Optional<MigrationPhase> migrationPhase,
                Optional<String> habitatDisplayName
        ) {
            this(populationKey, speciesDisplayName, visibleActors, visibleAlphas, migrationPhase, habitatDisplayName,
                    Optional.empty(), Optional.empty());
        }

        PopulationPresence(
                String populationKey,
                String speciesDisplayName,
                int visibleActors,
                int visibleAlphas,
                Optional<MigrationPhase> migrationPhase
        ) {
            this(populationKey, speciesDisplayName, visibleActors, visibleAlphas, migrationPhase, Optional.empty(),
                    Optional.empty(), Optional.empty());
        }

        PopulationPresence(String populationKey, String speciesDisplayName, int visibleActors, int visibleAlphas) {
            this(populationKey, speciesDisplayName, visibleActors, visibleAlphas, Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty());
        }
    }

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % UPDATE_INTERVAL_TICKS == 0) reconcile(server.getOverworld());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            synchronized (REMEMBERED) {
                REMEMBERED.remove(server);
            }
        });
    }

    static void reconcile(ServerWorld world) {
        if (world == null || world.getServer() == null || world != world.getServer().getOverworld()) return;

        MinecraftServer server = world.getServer();
        List<WildEcologyProjectionRegistry.ProjectedActor> projections = WildEcologyProjectionRegistry.collect(world);
        Set<UUID> online = new HashSet<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            UUID playerId = player.getUuid();
            online.add(playerId);
            if (player.isSpectator()) {
                remember(server, playerId, null);
                continue;
            }

            WildHabitatCueRuntime.NearbyInteractionSnapshot focused =
                    WildHabitatCueRuntime.nearestInteractionActor(player, projections);
            PopulationPresence current = presenceFor(focused, projections);
            PopulationPresence previous = remembered(server, playerId);
            remember(server, playerId, current);
            if (shouldAnnounce(previous, current)) {
                player.sendMessage(Text.literal(presenceText(previous, current)), true);
            }
        }
        forgetOffline(server, online);
    }

    static PopulationPresence presenceFor(
            WildHabitatCueRuntime.NearbyInteractionSnapshot focused,
            List<WildEcologyProjectionRegistry.ProjectedActor> projections
    ) {
        if (focused == null || projections == null || projections.isEmpty()) return null;

        WildEcologyProjectionRegistry.ProjectedActor focusedProjection = projections.stream()
                .filter(candidate -> candidate != null && !candidate.actor().isRemoved())
                .filter(candidate -> candidate.actor().getUuid().equals(focused.actorId()))
                .findFirst()
                .orElse(null);
        if (focusedProjection == null) return null;

        String populationKey = focusedProjection.populationKey();
        int visibleActors = 0;
        int visibleAlphas = 0;
        double maxPairDistanceSquared = 0.0D;
        List<WildEcologyProjectionRegistry.ProjectedActor> population = projections.stream()
                .filter(candidate -> candidate != null && !candidate.actor().isRemoved())
                .filter(candidate -> populationKey.equals(candidate.populationKey()))
                .toList();
        for (WildEcologyProjectionRegistry.ProjectedActor projection : population) {
            visibleActors++;
            if (projection.presentationCapabilities().nativeAlphaVisual()) visibleAlphas++;
        }
        for (int left = 0; left < population.size(); left++) {
            for (int right = left + 1; right < population.size(); right++) {
                double dx = population.get(left).actor().getX() - population.get(right).actor().getX();
                double dz = population.get(left).actor().getZ() - population.get(right).actor().getZ();
                maxPairDistanceSquared = Math.max(maxPairDistanceSquared, dx * dx + dz * dz);
            }
        }
        if (visibleActors <= 0) return null;

        Optional<WildSocialRole> focusedPresentedRole = focusedProjection.presentationCapabilities().nativeAlphaVisual()
                ? Optional.of(focusedProjection.socialRole())
                : Optional.empty();
        return new PopulationPresence(
                populationKey,
                WildHabitatCueRuntime.displaySpeciesName(focused.speciesId()),
                visibleActors,
                visibleAlphas,
                focused.migrationPhase(),
                Optional.of(focused.habitatDisplayName()),
                Optional.of(classifyGroupSpread(
                        visibleActors,
                        Math.sqrt(maxPairDistanceSquared),
                        focusedProjection.behaviorProfile())),
                focusedPresentedRole);
    }

    static GroupSpread classifyGroupSpread(int visibleActors, double maxPairDistance, WildBehaviorProfile behaviorProfile) {
        if (visibleActors <= 0) throw new IllegalArgumentException("visibleActors must be positive");
        if (!Double.isFinite(maxPairDistance) || maxPairDistance < 0.0D) {
            throw new IllegalArgumentException("maxPairDistance must be finite and non-negative");
        }
        if (behaviorProfile == null) throw new IllegalArgumentException("behaviorProfile is required");
        if (visibleActors == 1) return GroupSpread.ALONE;
        if (maxPairDistance <= behaviorProfile.separationDistance()) return GroupSpread.CLUSTERED;
        if (maxPairDistance <= behaviorProfile.cohesionDistance()) return GroupSpread.COHESIVE;
        return GroupSpread.DISPERSED;
    }

    static boolean shouldAnnounce(PopulationPresence previous, PopulationPresence current) {
        if (current == null) return false;
        if (previous == null) return true;
        if (!current.populationKey().equals(previous.populationKey())) return true;
        return current.visibleActors() != previous.visibleActors()
                || current.visibleAlphas() != previous.visibleAlphas()
                || !current.migrationPhase().equals(previous.migrationPhase())
                || !current.habitatDisplayName().equals(previous.habitatDisplayName())
                || !current.groupSpread().equals(previous.groupSpread())
                || !current.focusedSocialRole().equals(previous.focusedSocialRole());
    }

    static String presenceText(PopulationPresence previous, PopulationPresence current) {
        if (current == null) throw new IllegalArgumentException("current presence is required");
        String count = Integer.toString(current.visibleActors());
        if (previous != null
                && current.populationKey().equals(previous.populationKey())
                && previous.visibleActors() != current.visibleActors()) {
            count = previous.visibleActors() + " → " + current.visibleActors();
        }
        StringBuilder text = new StringBuilder("Wild population — ")
                .append(current.speciesDisplayName())
                .append(" · ")
                .append(count)
                .append(" WILD visible");
        if (current.visibleAlphas() == 1) {
            text.append(" · Alpha visible");
        } else if (current.visibleAlphas() > 1) {
            text.append(" · ").append(current.visibleAlphas()).append(" Alphas visible");
        }
        if (current.focusedSocialRole().orElse(null) == WildSocialRole.ALPHA) {
            text.append(" · focused Alpha");
        }
        current.groupSpread().ifPresent(spread -> text.append(" · ").append(spread.displayText()));
        current.habitatDisplayName().ifPresent(habitat -> text.append(" · habitat ").append(habitat));
        current.migrationPhase().ifPresent(phase -> text.append(" · ")
                .append(WildHabitatCueRuntime.displayMigrationPhase(phase)));
        return text.toString();
    }

    private static PopulationPresence remembered(MinecraftServer server, UUID playerId) {
        synchronized (REMEMBERED) {
            Map<UUID, PopulationPresence> players = REMEMBERED.get(server);
            return players == null ? null : players.get(playerId);
        }
    }

    private static void remember(MinecraftServer server, UUID playerId, PopulationPresence presence) {
        synchronized (REMEMBERED) {
            Map<UUID, PopulationPresence> players = REMEMBERED.computeIfAbsent(server, ignored -> new HashMap<>());
            if (presence == null) players.remove(playerId);
            else players.put(playerId, presence);
            if (players.isEmpty()) REMEMBERED.remove(server);
        }
    }

    private static void forgetOffline(MinecraftServer server, Set<UUID> online) {
        synchronized (REMEMBERED) {
            Map<UUID, PopulationPresence> players = REMEMBERED.get(server);
            if (players == null) return;
            players.keySet().removeIf(playerId -> !online.contains(playerId));
            if (players.isEmpty()) REMEMBERED.remove(server);
        }
    }
}
