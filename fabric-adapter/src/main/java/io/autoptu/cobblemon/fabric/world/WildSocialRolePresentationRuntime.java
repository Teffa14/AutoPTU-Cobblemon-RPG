package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

/**
 * Projects explicitly server-authored wild presentation capabilities into Minecraft-only visuals.
 *
 * <p>The ecology descriptor must explicitly request native Alpha presentation and herd-leader presentation.
 * The runtime mirrors only that request into Cobblemon's synchronized entity-only alpha presentation flag while
 * deliberately leaving {@code Pokemon.isAlpha} untouched. Cobblemon persistent alpha gameplay, alpha movesets,
 * stats and herd AI therefore remain outside RPG authority.</p>
 */
public final class WildSocialRolePresentationRuntime implements ModInitializer {
    private static final int UPDATE_INTERVAL_TICKS = 20;
    private static final int BASE_LEADER_PARTICLES = 2;
    private static final int MAX_LEADER_PARTICLES = 8;
    private static final double BASE_LEADER_SPREAD = 0.12D;
    private static final double MAX_LEADER_SPREAD = 0.30D;
    private static final double SPREAD_PER_GATHERED_MEMBER = 0.03D;
    private static final double BASE_LEADER_MARKER_HEIGHT = 0.35D;
    private static final double MAX_LEADER_MARKER_HEIGHT = 0.65D;
    private static final double HEIGHT_PER_GATHERED_MEMBER = 0.05D;

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
        for (var projection : projections) {
            var actor = projection.actor();
            if (actor.isRemoved()) continue;
            if (!VisibleWildPokemonEncounterRuntime.isInteractionActive(actor.getUuid())) continue;

            var capabilities = projection.presentationCapabilities();
            projectNativeAlphaVisual(actor, capabilities.nativeAlphaVisual());
            if (!capabilities.herdLeaderPresentation() || actor.isInvisible()) continue;

            int gatheredMembers = gatheredHerdMemberCount(projection, projections);
            double markerSpread = markerSpreadForHerdMemberCount(gatheredMembers);
            double markerHeight = markerHeightForHerdMemberCount(gatheredMembers);
            world.spawnParticles(
                    ParticleTypes.END_ROD,
                    actor.getX(), actor.getY() + actor.getHeight() + markerHeight, actor.getZ(),
                    particleCountForHerdMemberCount(gatheredMembers), markerSpread, 0.08D, markerSpread, 0.005D);
            projected++;
        }
        return projected;
    }

    /**
     * Counts only active, visible, same-population Minecraft projections inside the ecology-authored cohesion
     * envelope. The count controls marker density, spread and height only; it does not create herd AI, encounter
     * or PTU semantics.
     */
    static int gatheredHerdMemberCount(
            WildEcologyProjectionRegistry.ProjectedActor leader,
            List<WildEcologyProjectionRegistry.ProjectedActor> projections
    ) {
        if (leader == null) throw new IllegalArgumentException("leader is required");
        if (projections == null) throw new IllegalArgumentException("projections are required");
        double cohesion = leader.behaviorProfile().cohesionDistance();
        double cohesionSquared = cohesion * cohesion;
        int gathered = 0;
        for (var candidate : projections) {
            if (candidate == null || candidate.actor().isRemoved() || candidate.actor().isInvisible()) continue;
            if (candidate.actor().getUuid().equals(leader.actor().getUuid())) continue;
            if (!leader.populationKey().equals(candidate.populationKey())) continue;
            if (!VisibleWildPokemonEncounterRuntime.isInteractionActive(candidate.actor().getUuid())) continue;
            double dx = candidate.actor().getX() - leader.actor().getX();
            double dz = candidate.actor().getZ() - leader.actor().getZ();
            if (dx * dx + dz * dz <= cohesionSquared) gathered++;
        }
        return gathered;
    }

    static int particleCountForHerdMemberCount(int gatheredMembers) {
        requireNonNegativeGatheredMembers(gatheredMembers);
        long requested = (long) BASE_LEADER_PARTICLES + gatheredMembers;
        return (int) Math.min(MAX_LEADER_PARTICLES, requested);
    }

    static double markerSpreadForHerdMemberCount(int gatheredMembers) {
        requireNonNegativeGatheredMembers(gatheredMembers);
        double requested = BASE_LEADER_SPREAD + gatheredMembers * SPREAD_PER_GATHERED_MEMBER;
        return Math.min(MAX_LEADER_SPREAD, requested);
    }

    static double markerHeightForHerdMemberCount(int gatheredMembers) {
        requireNonNegativeGatheredMembers(gatheredMembers);
        double requested = BASE_LEADER_MARKER_HEIGHT + gatheredMembers * HEIGHT_PER_GATHERED_MEMBER;
        return Math.min(MAX_LEADER_MARKER_HEIGHT, requested);
    }

    private static void requireNonNegativeGatheredMembers(int gatheredMembers) {
        if (gatheredMembers < 0) throw new IllegalArgumentException("gatheredMembers must be non-negative");
    }

    /** Mirrors only Cobblemon's synchronized entity presentation bit, never Pokemon#setIsAlpha. */
    static boolean projectNativeAlphaVisual(PokemonEntity actor, boolean alpha) {
        if (actor == null || actor.isRemoved()) return false;
        var alphaData = PokemonEntity.getIS_ALPHA();
        boolean current = actor.getDataTracker().get(alphaData);
        if (current == alpha) return false;
        actor.getDataTracker().set(alphaData, alpha);
        return true;
    }
}
