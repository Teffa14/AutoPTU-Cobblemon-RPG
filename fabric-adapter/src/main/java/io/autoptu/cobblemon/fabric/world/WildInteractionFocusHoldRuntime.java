package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Keeps the server-selected visible WILD interaction focus physically stable during the safe
 * interaction window. The hold is presentation-only and deliberately yields to the authored
 * ALARMED proximity band, so a frightened wild actor can still flee normally.
 *
 * This runtime is registered after the ambient/navigation presentation runtimes. It reuses the
 * same projection/binding/range resolver as the encounter cue and never reads Cobblemon Pokemon
 * payload, species, level, HP, moves, statuses, ownership, battle state or PTU rules.
 */
public final class WildInteractionFocusHoldRuntime implements ModInitializer {
    private static final int UPDATE_INTERVAL_TICKS = 10;

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % UPDATE_INTERVAL_TICKS == 0) reconcile(server.getOverworld());
        });
    }

    static void reconcile(ServerWorld world) {
        if (world == null || world.getServer() == null || world != world.getServer().getOverworld()) return;

        var projections = WildEcologyProjectionRegistry.collect(world);
        if (projections.isEmpty()) return;

        Map<UUID, WildEcologyProjectionRegistry.ProjectedActor> byActorId = new HashMap<>();
        for (var projection : projections) {
            if (projection == null || projection.actor().isRemoved() || projection.actor().isInvisible()) continue;
            byActorId.put(projection.actor().getUuid(), projection);
        }

        Set<UUID> heldActorIds = new HashSet<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isSpectator()) continue;
            WildHabitatCueRuntime.NearbyInteractionSnapshot focus =
                    WildHabitatCueRuntime.nearestInteractionActor(player, projections);
            if (focus == null || !heldActorIds.add(focus.actorId())) continue;

            var projection = byActorId.get(focus.actorId());
            if (projection == null) continue;
            PokemonEntity actor = projection.actor();
            double squaredDistance = actor.squaredDistanceTo(player);
            if (!shouldHoldInteractionFocus(squaredDistance, projection.behaviorProfile().alarmDistance())) continue;

            actor.getNavigation().stop();
            var velocity = actor.getVelocity();
            actor.setVelocity(0.0D, velocity.y, 0.0D);
            actor.velocityModified = true;
        }
    }

    static boolean shouldHoldInteractionFocus(double squaredDistance, double alarmDistance) {
        if (!Double.isFinite(squaredDistance) || squaredDistance < 0.0D
                || !Double.isFinite(alarmDistance) || alarmDistance <= 0.0D) return false;
        if (!VisibleWildPokemonEncounterRuntime.isWithinInteractionDistanceSquared(squaredDistance)) return false;
        return squaredDistance > alarmDistance * alarmDistance;
    }
}
