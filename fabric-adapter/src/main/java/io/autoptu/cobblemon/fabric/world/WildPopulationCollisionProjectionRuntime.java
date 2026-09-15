package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps Minecraft collision and dormant-body physics aligned with the server-owned WILD presence projection.
 *
 * <p>Canonical encounter identity and interaction eligibility remain authoritative elsewhere. This runtime only
 * projects dormant presence into Minecraft physics: hibernating or otherwise inactive visible-WILD presentation
 * actors stop participating in collision, suspend vanilla gravity and remain pinned at the position where
 * presentation was suspended. Active visible actors regain their prior gravity policy and normal collision. It does
 * not derive PTU movement legality, forced movement, initiative, damage, HP, statuses, capture or battle outcomes.</p>
 */
public final class WildPopulationCollisionProjectionRuntime implements ModInitializer {
    private static final double REANCHOR_EPSILON_SQUARED = 1.0E-8D;
    private static final Map<UUID, DormantAnchor> DORMANT_ANCHORS = new HashMap<>();

    @Override
    public void onInitialize() {
        // Physics advances every tick, so dormant anchoring must run every tick rather than only at the slower
        // population-presence reconciliation cadence. Otherwise gravity can move a no-clip presentation actor.
        ServerTickEvents.END_SERVER_TICK.register(WildPopulationCollisionProjectionRuntime::synchronizeAllWorlds);
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            DormantAnchor anchor = DORMANT_ANCHORS.remove(entity.getUuid());
            if (anchor != null && entity instanceof PokemonEntity actor) actor.setNoGravity(anchor.previousNoGravity());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> DORMANT_ANCHORS.clear());
    }

    private static void synchronizeAllWorlds(MinecraftServer server) {
        if (server == null) return;
        for (ServerWorld world : server.getWorlds()) synchronize(world);
    }

    static int synchronize(ServerWorld world) {
        if (world == null) throw new IllegalArgumentException("world is required");
        int synchronizedActors = 0;
        for (var projected : WildEcologyProjectionSource.collect(world)) {
            var actor = projected.actor();
            boolean interactionActive = VisibleWildPokemonEncounterRuntime.isInteractionActive(actor.getUuid());
            boolean dormant = shouldDisableCollision(interactionActive, actor.isInvisible());
            actor.noClip = dormant;
            synchronizeDormantPhysics(actor, dormant);
            synchronizedActors++;
        }
        return synchronizedActors;
    }

    private static void synchronizeDormantPhysics(PokemonEntity actor, boolean dormant) {
        UUID actorId = actor.getUuid();
        if (!dormant) {
            DormantAnchor anchor = DORMANT_ANCHORS.remove(actorId);
            if (anchor != null && actor.hasNoGravity() != anchor.previousNoGravity()) {
                actor.setNoGravity(anchor.previousNoGravity());
            }
            return;
        }

        DormantAnchor anchor = DORMANT_ANCHORS.computeIfAbsent(
                actorId,
                ignored -> new DormantAnchor(actor.getX(), actor.getY(), actor.getZ(), actor.hasNoGravity())
        );
        if (!actor.hasNoGravity()) actor.setNoGravity(true);
        actor.getNavigation().stop();
        actor.setVelocity(0.0D, 0.0D, 0.0D);
        actor.velocityModified = true;
        if (shouldReanchor(actor.getX(), actor.getY(), actor.getZ(), anchor.x(), anchor.y(), anchor.z())) {
            actor.requestTeleport(anchor.x(), anchor.y(), anchor.z());
        }
    }

    static boolean shouldDisableCollision(boolean interactionActive, boolean invisible) {
        return !interactionActive || invisible;
    }

    static boolean shouldReanchor(
            double actorX,
            double actorY,
            double actorZ,
            double anchorX,
            double anchorY,
            double anchorZ
    ) {
        double dx = actorX - anchorX;
        double dy = actorY - anchorY;
        double dz = actorZ - anchorZ;
        return dx * dx + dy * dy + dz * dz > REANCHOR_EPSILON_SQUARED;
    }

    private record DormantAnchor(double x, double y, double z, boolean previousNoGravity) {}
}
