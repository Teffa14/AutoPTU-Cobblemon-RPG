package io.autoptu.cobblemon.fabric.world;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Surfaces the authored herd leader relationship for a player's focused canonical WILD member.
 *
 * <p>The relationship is resolved only from same-population ecology projections whose social role is explicitly
 * {@link WildSocialRole#ALPHA} and whose descriptor explicitly requests herd-leader presentation. Minecraft
 * positions, observed velocity and vanilla server visibility only describe proximity, separation, direction,
 * movement, group shape and occlusion. No Cobblemon herd AI or Pokemon gameplay payload is an authority input,
 * and this runtime supplies no PTU effects.</p>
 */
public final class WildFocusedHerdLeaderContextRuntime implements ModInitializer {
    private static final int UPDATE_INTERVAL_TICKS = 10;
    private static final double MOVING_HORIZONTAL_SPEED_SQUARED = 0.0001D;
    private static final Map<MinecraftServer, Map<UUID, LeaderContext>> REMEMBERED = new IdentityHashMap<>();

    record LeaderContext(String populationKey, UUID leaderActorId, String speciesDisplayName, boolean withinSeparation,
                         boolean withinCohesion, int clusteredMemberCount, int nearbyMemberCount, int stragglerMemberCount,
                         int horizontalDistanceBlocks, int verticalOffsetBlocks, String compassDirection,
                         String leaderHabitatDisplayName, boolean leaderInFocusedHabitat, boolean leaderVisibleToPlayer,
                         boolean leaderMoving) {
        LeaderContext {
            if (populationKey == null || populationKey.isBlank()) throw new IllegalArgumentException("populationKey is required");
            if (leaderActorId == null) throw new IllegalArgumentException("leaderActorId is required");
            if (speciesDisplayName == null || speciesDisplayName.isBlank()) throw new IllegalArgumentException("speciesDisplayName is required");
            if (withinSeparation && !withinCohesion) throw new IllegalArgumentException("withinSeparation requires withinCohesion");
            if (clusteredMemberCount < 0) throw new IllegalArgumentException("clusteredMemberCount must be non-negative");
            if (nearbyMemberCount < 0) throw new IllegalArgumentException("nearbyMemberCount must be non-negative");
            if (stragglerMemberCount < 0) throw new IllegalArgumentException("stragglerMemberCount must be non-negative");
            if (horizontalDistanceBlocks < 0) throw new IllegalArgumentException("horizontalDistanceBlocks must be non-negative");
            if (compassDirection == null || compassDirection.isBlank()) throw new IllegalArgumentException("compassDirection is required");
            if (leaderHabitatDisplayName == null || leaderHabitatDisplayName.isBlank()) throw new IllegalArgumentException("leaderHabitatDisplayName is required");
            populationKey = populationKey.strip();
            speciesDisplayName = speciesDisplayName.strip();
            compassDirection = compassDirection.strip();
            leaderHabitatDisplayName = leaderHabitatDisplayName.strip();
        }
    }

    record HerdCounts(int clusteredMembers, int nearbyMembers, int stragglerMembers) {
        HerdCounts {
            if (clusteredMembers < 0 || nearbyMembers < 0 || stragglerMembers < 0) {
                throw new IllegalArgumentException("herd counts must be non-negative");
            }
        }
    }

    @Override public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> { if (server.getTicks() % UPDATE_INTERVAL_TICKS == 0) reconcile(server.getOverworld()); });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { synchronized (REMEMBERED) { REMEMBERED.remove(server); } });
    }

    static void reconcile(ServerWorld world) {
        if (world == null || world.getServer() == null || world != world.getServer().getOverworld()) return;
        MinecraftServer server = world.getServer();
        List<WildEcologyProjectionRegistry.ProjectedActor> projections = WildEcologyProjectionRegistry.collect(world);
        Set<UUID> online = new HashSet<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            UUID playerId = player.getUuid();
            online.add(playerId);
            if (player.isSpectator()) { remember(server, playerId, null); continue; }
            LeaderContext current = contextFor(player, projections);
            LeaderContext previous = remembered(server, playerId);
            remember(server, playerId, current);
            if (shouldAnnounce(previous, current)) player.sendMessage(Text.literal(contextText(current)), true);
        }
        forgetOffline(server, online);
    }

    static LeaderContext contextFor(ServerPlayerEntity player, List<WildEcologyProjectionRegistry.ProjectedActor> projections) {
        if (player == null || projections == null || projections.isEmpty()) return null;
        var focused = WildHabitatCueRuntime.nearestInteractionActor(player, projections);
        if (focused == null) return null;
        var member = projections.stream().filter(candidate -> candidate != null && !candidate.actor().isRemoved())
                .filter(candidate -> candidate.actor().getUuid().equals(focused.actorId())).findFirst().orElse(null);
        if (member == null || member.presentationCapabilities().herdLeaderPresentation()) return null;
        var alpha = projections.stream()
                .filter(candidate -> candidate != null)
                .filter(candidate -> candidate.socialRole() == WildSocialRole.ALPHA)
                .filter(candidate -> candidate.presentationCapabilities().herdLeaderPresentation())
                .filter(candidate -> member.populationKey().equals(candidate.populationKey()))
                .filter(candidate -> !candidate.actor().isRemoved() && !candidate.actor().isInvisible())
                .filter(candidate -> VisibleWildPokemonEncounterRuntime.isInteractionActive(candidate.actor().getUuid()))
                .min(Comparator.comparingDouble((WildEcologyProjectionRegistry.ProjectedActor candidate) -> member.actor().squaredDistanceTo(candidate.actor()))
                        .thenComparing(candidate -> candidate.actor().getUuid())).orElse(null);
        if (alpha == null) return null;
        double dx = alpha.actor().getX() - member.actor().getX();
        double dy = alpha.actor().getY() - member.actor().getY();
        double dz = alpha.actor().getZ() - member.actor().getZ();
        double horizontalDistanceSquared = dx * dx + dz * dz;
        double separation = member.behaviorProfile().separationDistance();
        double cohesion = member.behaviorProfile().cohesionDistance();
        HerdCounts counts = herdCounts(alpha, member.populationKey(), separation, cohesion, projections);
        return new LeaderContext(member.populationKey(), alpha.actor().getUuid(), WildHabitatCueRuntime.displaySpeciesName(focused.speciesId()),
                horizontalDistanceSquared <= separation * separation, horizontalDistanceSquared <= cohesion * cohesion,
                counts.clusteredMembers(), counts.nearbyMembers(), counts.stragglerMembers(),
                roundedHorizontalDistanceBlocks(horizontalDistanceSquared), roundedVerticalOffsetBlocks(dy), compassDirection(dx, dz),
                alpha.habitatDisplayName(), member.habitatDisplayName().equals(alpha.habitatDisplayName()), player.canSee(alpha.actor()),
                isMoving(alpha.actor().getVelocity().x, alpha.actor().getVelocity().z));
    }

    static HerdCounts herdCounts(WildEcologyProjectionRegistry.ProjectedActor alpha, String populationKey, double separation,
                                 double cohesion, List<WildEcologyProjectionRegistry.ProjectedActor> projections) {
        if (alpha == null) throw new IllegalArgumentException("alpha is required");
        if (populationKey == null || populationKey.isBlank()) throw new IllegalArgumentException("populationKey is required");
        if (!Double.isFinite(separation) || separation < 0.0D) throw new IllegalArgumentException("separation must be finite and non-negative");
        if (!Double.isFinite(cohesion) || cohesion < separation) throw new IllegalArgumentException("cohesion must be finite and at least separation");
        if (projections == null) throw new IllegalArgumentException("projections are required");
        double separationSquared = separation * separation;
        double cohesionSquared = cohesion * cohesion;
        int clustered = 0;
        int nearby = 0;
        int straggler = 0;
        for (var candidate : projections) {
            if (candidate == null || candidate.actor().isRemoved() || candidate.actor().isInvisible()) continue;
            if (candidate.actor().getUuid().equals(alpha.actor().getUuid())) continue;
            if (!populationKey.equals(candidate.populationKey())) continue;
            if (!VisibleWildPokemonEncounterRuntime.isInteractionActive(candidate.actor().getUuid())) continue;
            double dx = candidate.actor().getX() - alpha.actor().getX();
            double dz = candidate.actor().getZ() - alpha.actor().getZ();
            double horizontalDistanceSquared = dx * dx + dz * dz;
            if (horizontalDistanceSquared <= separationSquared) clustered++;
            else if (horizontalDistanceSquared <= cohesionSquared) nearby++;
            else straggler++;
        }
        return new HerdCounts(clustered, nearby, straggler);
    }

    static boolean isMoving(double velocityX, double velocityZ) {
        if (!Double.isFinite(velocityX) || !Double.isFinite(velocityZ)) throw new IllegalArgumentException("horizontal velocity must be finite");
        return velocityX * velocityX + velocityZ * velocityZ >= MOVING_HORIZONTAL_SPEED_SQUARED;
    }

    static int roundedHorizontalDistanceBlocks(double horizontalDistanceSquared) {
        if (!Double.isFinite(horizontalDistanceSquared) || horizontalDistanceSquared < 0.0D) throw new IllegalArgumentException("horizontalDistanceSquared must be finite and non-negative");
        double distance = Math.sqrt(horizontalDistanceSquared);
        if (distance >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) Math.round(distance);
    }

    static int roundedVerticalOffsetBlocks(double verticalOffset) {
        if (!Double.isFinite(verticalOffset)) throw new IllegalArgumentException("verticalOffset must be finite");
        if (verticalOffset >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (verticalOffset <= Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return (int) Math.round(verticalOffset);
    }

    static String verticalRelationText(int verticalOffsetBlocks) {
        if (verticalOffsetBlocks == 0) return "same level";
        long magnitude = Math.abs((long) verticalOffsetBlocks);
        return magnitude + (magnitude == 1L ? " block " : " blocks ") + (verticalOffsetBlocks > 0 ? "above" : "below");
    }

    static String proximityText(LeaderContext context) {
        if (context == null) throw new IllegalArgumentException("context is required");
        if (context.withinSeparation()) return "clustered";
        if (context.withinCohesion()) return "nearby";
        return "regrouping distance";
    }

    static String herdShapeText(LeaderContext context) {
        if (context == null) throw new IllegalArgumentException("context is required");
        return "herd " + context.clusteredMemberCount() + " clustered · " + context.nearbyMemberCount() + " nearby · "
                + context.stragglerMemberCount() + " regrouping";
    }

    static String compassDirection(double dx, double dz) {
        if (!Double.isFinite(dx) || !Double.isFinite(dz)) throw new IllegalArgumentException("leader offset must be finite");
        if (dx == 0.0D && dz == 0.0D) return "here";
        double angle = Math.toDegrees(Math.atan2(dx, -dz));
        if (angle < 0.0D) angle += 360.0D;
        String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        return directions[(int) Math.floor((angle + 22.5D) / 45.0D) % directions.length];
    }

    static boolean shouldAnnounce(LeaderContext previous, LeaderContext current) {
        if (current == null) return false;
        return previous == null || !current.populationKey().equals(previous.populationKey()) || !current.leaderActorId().equals(previous.leaderActorId())
                || current.withinSeparation() != previous.withinSeparation() || current.withinCohesion() != previous.withinCohesion()
                || current.clusteredMemberCount() != previous.clusteredMemberCount() || current.nearbyMemberCount() != previous.nearbyMemberCount()
                || current.stragglerMemberCount() != previous.stragglerMemberCount()
                || current.horizontalDistanceBlocks() != previous.horizontalDistanceBlocks() || current.verticalOffsetBlocks() != previous.verticalOffsetBlocks()
                || !current.compassDirection().equals(previous.compassDirection()) || !current.leaderHabitatDisplayName().equals(previous.leaderHabitatDisplayName())
                || current.leaderInFocusedHabitat() != previous.leaderInFocusedHabitat() || current.leaderVisibleToPlayer() != previous.leaderVisibleToPlayer()
                || current.leaderMoving() != previous.leaderMoving();
    }

    static String contextText(LeaderContext context) {
        if (context == null) throw new IllegalArgumentException("context is required");
        return "Herd leader — Alpha " + context.speciesDisplayName() + " · " + proximityText(context)
                + " · " + herdShapeText(context)
                + " · " + context.horizontalDistanceBlocks() + " blocks · " + context.compassDirection() + " · " + verticalRelationText(context.verticalOffsetBlocks())
                + (context.leaderVisibleToPlayer() ? " · visible" : " · obscured")
                + (context.leaderMoving() ? " · moving" : " · holding")
                + (context.leaderInFocusedHabitat() ? " · same habitat" : " · leader habitat " + context.leaderHabitatDisplayName());
    }

    private static LeaderContext remembered(MinecraftServer server, UUID playerId) { synchronized (REMEMBERED) { Map<UUID, LeaderContext> players = REMEMBERED.get(server); return players == null ? null : players.get(playerId); } }
    private static void remember(MinecraftServer server, UUID playerId, LeaderContext context) { synchronized (REMEMBERED) { Map<UUID, LeaderContext> players = REMEMBERED.computeIfAbsent(server, ignored -> new HashMap<>()); if (context == null) players.remove(playerId); else players.put(playerId, context); if (players.isEmpty()) REMEMBERED.remove(server); } }
    private static void forgetOffline(MinecraftServer server, Set<UUID> online) { synchronized (REMEMBERED) { Map<UUID, LeaderContext> players = REMEMBERED.get(server); if (players == null) return; players.keySet().removeIf(playerId -> !online.contains(playerId)); if (players.isEmpty()) REMEMBERED.remove(server); } }
}
