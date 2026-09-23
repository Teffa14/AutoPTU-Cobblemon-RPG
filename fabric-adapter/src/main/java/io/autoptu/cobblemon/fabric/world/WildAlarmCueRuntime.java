package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Adds a short Minecraft-visible alarm cue when a server-authored wild actor first becomes alarmed.
 *
 * <p>The cue reuses the same proximity controller as ambient presentation and observes only server-side
 * player distance plus the authored ecology profile. It never supplies PTU detection, initiative,
 * targeting, movement legality, RNG, damage, moves, abilities, statuses or encounter outcomes.</p>
 */
public final class WildAlarmCueRuntime implements ModInitializer {
    private static final int UPDATE_INTERVAL_TICKS = 10;
    private static final Map<MinecraftServer, Map<UUID, AmbientPokemonBehaviorController>> CONTROLLERS =
            new IdentityHashMap<>();
    private static final Map<MinecraftServer, Map<UUID, AmbientPokemonBehaviorController.State>> LAST_STATES =
            new IdentityHashMap<>();

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % UPDATE_INTERVAL_TICKS == 0) update(server);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            synchronized (CONTROLLERS) {
                CONTROLLERS.remove(server);
                LAST_STATES.remove(server);
            }
        });
    }

    static void update(MinecraftServer server) {
        if (server == null) return;
        ServerWorld world = server.getOverworld();
        var projected = WildEcologyProjectionSource.collect(world);
        Map<UUID, AmbientPokemonBehaviorController> controllers;
        Map<UUID, AmbientPokemonBehaviorController.State> lastStates;
        synchronized (CONTROLLERS) {
            controllers = CONTROLLERS.computeIfAbsent(server, ignored -> new HashMap<>());
            lastStates = LAST_STATES.computeIfAbsent(server, ignored -> new HashMap<>());
        }
        var live = new HashSet<UUID>();

        for (var projection : projected) {
            PokemonEntity actor = projection.actor();
            UUID actorId = actor.getUuid();
            if (actor.isRemoved() || actor.isInvisible()
                    || !VisibleWildPokemonEncounterRuntime.isInteractionActive(actorId)) {
                controllers.remove(actorId);
                lastStates.remove(actorId);
                continue;
            }
            live.add(actorId);
            ServerPlayerEntity nearest = nearestPlayer(world, actor);
            AmbientPokemonBehaviorController controller = controllers.computeIfAbsent(
                    actorId,
                    ignored -> new AmbientPokemonBehaviorController(projection.behaviorProfile().proximityProfile()));
            AmbientPokemonBehaviorController.State state = controller.update(
                    nearest == null ? Double.POSITIVE_INFINITY : Math.sqrt(actor.squaredDistanceTo(nearest)),
                    nearest != null);
            AmbientPokemonBehaviorController.State previous = lastStates.put(actorId, state);
            if (enteredAlarm(previous, state)) {
                world.spawnParticles(
                        ParticleTypes.ANGRY_VILLAGER,
                        actor.getX(), actor.getY() + actor.getHeight() + 0.25D, actor.getZ(),
                        3, 0.18D, 0.12D, 0.18D, 0.0D);
            }
        }
        controllers.keySet().removeIf(id -> !live.contains(id));
        lastStates.keySet().removeIf(id -> !live.contains(id));
    }

    static boolean enteredAlarm(
            AmbientPokemonBehaviorController.State previous,
            AmbientPokemonBehaviorController.State current
    ) {
        return current == AmbientPokemonBehaviorController.State.ALARMED
                && previous != AmbientPokemonBehaviorController.State.ALARMED;
    }

    private static ServerPlayerEntity nearestPlayer(ServerWorld world, PokemonEntity actor) {
        ServerPlayerEntity nearest = null;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isSpectator()) continue;
            double distance = actor.squaredDistanceTo(player);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = player;
            }
        }
        return nearest;
    }
}
