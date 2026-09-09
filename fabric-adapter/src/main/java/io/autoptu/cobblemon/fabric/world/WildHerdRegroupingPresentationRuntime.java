package io.autoptu.cobblemon.fabric.world;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Projects a Minecraft-only regrouping cue for canonical WILD herd members outside authored cohesion.
 *
 * <p>The cue consumes only server-owned population identity, authored ecology behavior and observed Minecraft
 * positions. It does not move actors, create encounter legality, infer Cobblemon herd AI or apply PTU effects.</p>
 */
public final class WildHerdRegroupingPresentationRuntime implements ModInitializer {
    private static final int UPDATE_INTERVAL_TICKS = 20;
    private static final int BASE_PARTICLE_COUNT = 2;
    private static final int MAX_PARTICLE_COUNT = 6;
    private static final double EXCESS_BLOCKS_PER_EXTRA_PARTICLE = 4.0D;
    private static final double CUE_OFFSET_TOWARD_LEADER = 0.35D;
    private static final int CUE_TRAIL_POINTS = 3;
    private static final double BASE_CUE_HEIGHT = 0.18D;
    private static final double MAX_CUE_HEIGHT = 0.42D;
    private static final double EXTRA_HEIGHT_PER_EXCESS_BLOCK = 0.02D;

    record CueOffset(double x, double z) {
        CueOffset {
            if (!Double.isFinite(x) || !Double.isFinite(z)) {
                throw new IllegalArgumentException("cue offset must be finite");
            }
        }
    }

    @Override
    public void onInitialize() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getTime() % UPDATE_INTERVAL_TICKS != 0L) return;
            project(world);
        });
    }

    static int project(ServerWorld world) {
        if (world == null) return 0;
        List<WildEcologyProjectionRegistry.ProjectedActor> projections = WildEcologyProjectionRegistry.collect(world);
        int projected = 0;
        for (var member : projections) {
            if (!isActiveVisibleMember(member)) continue;
            var leader = nearestActiveLeader(member, projections);
            if (leader == null) continue;

            double dx = member.actor().getX() - leader.actor().getX();
            double dz = member.actor().getZ() - leader.actor().getZ();
            double cohesionDistance = member.behaviorProfile().cohesionDistance();
            if (!isOutsideCohesion(dx, dz, cohesionDistance)) continue;

            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
            int particleCount = cueParticleCount(horizontalDistance, cohesionDistance);
            double cueHeight = cueVerticalOffset(horizontalDistance, cohesionDistance);
            List<CueOffset> cueTrail = cueTrailTowardLeader(-dx, -dz);
            for (int index = 0; index < cueTrail.size(); index++) {
                CueOffset cueOffset = cueTrail.get(index);
                world.spawnParticles(
                        ParticleTypes.CLOUD,
                        member.actor().getX() + cueOffset.x(), member.actor().getY() + member.actor().getHeight() + cueHeight,
                        member.actor().getZ() + cueOffset.z(),
                        index == 0 ? particleCount : 1, 0.10D, 0.05D, 0.10D, 0.003D);
            }
            projected++;
        }
        return projected;
    }

    static boolean isOutsideCohesion(double dx, double dz, double cohesionDistance) {
        if (!Double.isFinite(dx) || !Double.isFinite(dz)) {
            throw new IllegalArgumentException("member offset must be finite");
        }
        if (!Double.isFinite(cohesionDistance) || cohesionDistance < 0.0D) {
            throw new IllegalArgumentException("cohesionDistance must be finite and non-negative");
        }
        double distanceSquared = dx * dx + dz * dz;
        double cohesionSquared = cohesionDistance * cohesionDistance;
        return distanceSquared > cohesionSquared;
    }

    static int cueParticleCount(double horizontalDistance, double cohesionDistance) {
        validateDistances(horizontalDistance, cohesionDistance);
        double excess = Math.max(0.0D, horizontalDistance - cohesionDistance);
        int extra = (int) Math.floor(excess / EXCESS_BLOCKS_PER_EXTRA_PARTICLE);
        return Math.min(MAX_PARTICLE_COUNT, BASE_PARTICLE_COUNT + extra);
    }

    static double cueVerticalOffset(double horizontalDistance, double cohesionDistance) {
        validateDistances(horizontalDistance, cohesionDistance);
        double excess = Math.max(0.0D, horizontalDistance - cohesionDistance);
        return Math.min(MAX_CUE_HEIGHT, BASE_CUE_HEIGHT + excess * EXTRA_HEIGHT_PER_EXCESS_BLOCK);
    }

    private static void validateDistances(double horizontalDistance, double cohesionDistance) {
        if (!Double.isFinite(horizontalDistance) || horizontalDistance < 0.0D) {
            throw new IllegalArgumentException("horizontalDistance must be finite and non-negative");
        }
        if (!Double.isFinite(cohesionDistance) || cohesionDistance < 0.0D) {
            throw new IllegalArgumentException("cohesionDistance must be finite and non-negative");
        }
    }

    static CueOffset cueOffsetTowardLeader(double leaderDx, double leaderDz) {
        if (!Double.isFinite(leaderDx) || !Double.isFinite(leaderDz)) {
            throw new IllegalArgumentException("leader offset must be finite");
        }
        double horizontalDistance = Math.hypot(leaderDx, leaderDz);
        if (horizontalDistance == 0.0D) return new CueOffset(0.0D, 0.0D);
        double scale = Math.min(CUE_OFFSET_TOWARD_LEADER, horizontalDistance) / horizontalDistance;
        return new CueOffset(leaderDx * scale, leaderDz * scale);
    }

    static List<CueOffset> cueTrailTowardLeader(double leaderDx, double leaderDz) {
        if (!Double.isFinite(leaderDx) || !Double.isFinite(leaderDz)) {
            throw new IllegalArgumentException("leader offset must be finite");
        }
        double horizontalDistance = Math.hypot(leaderDx, leaderDz);
        if (horizontalDistance == 0.0D) return List.of(new CueOffset(0.0D, 0.0D));
        double unitX = leaderDx / horizontalDistance;
        double unitZ = leaderDz / horizontalDistance;
        List<CueOffset> offsets = new ArrayList<>(CUE_TRAIL_POINTS);
        for (int index = 1; index <= CUE_TRAIL_POINTS; index++) {
            double distance = Math.min(CUE_OFFSET_TOWARD_LEADER * index, horizontalDistance);
            offsets.add(new CueOffset(unitX * distance, unitZ * distance));
            if (distance >= horizontalDistance) break;
        }
        return List.copyOf(offsets);
    }

    private static boolean isActiveVisibleMember(WildEcologyProjectionRegistry.ProjectedActor candidate) {
        if (candidate == null || candidate.socialRole() == WildSocialRole.ALPHA) return false;
        if (candidate.actor().isRemoved() || candidate.actor().isInvisible()) return false;
        return VisibleWildPokemonEncounterRuntime.isInteractionActive(candidate.actor().getUuid());
    }

    private static WildEcologyProjectionRegistry.ProjectedActor nearestActiveLeader(
            WildEcologyProjectionRegistry.ProjectedActor member,
            List<WildEcologyProjectionRegistry.ProjectedActor> projections
    ) {
        return projections.stream()
                .filter(candidate -> candidate != null)
                .filter(candidate -> candidate.socialRole() == WildSocialRole.ALPHA)
                .filter(candidate -> candidate.presentationCapabilities().herdLeaderPresentation())
                .filter(candidate -> member.populationKey().equals(candidate.populationKey()))
                .filter(candidate -> !candidate.actor().isRemoved() && !candidate.actor().isInvisible())
                .filter(candidate -> VisibleWildPokemonEncounterRuntime.isInteractionActive(candidate.actor().getUuid()))
                .min(Comparator.comparingDouble((WildEcologyProjectionRegistry.ProjectedActor candidate) ->
                                member.actor().squaredDistanceTo(candidate.actor()))
                        .thenComparing(candidate -> candidate.actor().getUuid()))
                .orElse(null);
    }
}
