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
 * Adds short Minecraft-visible attention cues when a server-authored wild actor changes proximity state.
 *
 * <p>The cues reuse the same proximity controller as ambient presentation and observe only server-side
 * player distance plus the authored ecology profile. They never supply PTU detection, initiative,
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
            if (enteredWatching(previous, state)) {
                world.spawnParticles(
                        ParticleTypes.ENCHANT,
                        actor.getX(), actor.getY() + actor.getHeight() + 0.2D, actor.getZ(),
                        4, 0.2D, 0.1D, 0.2D, 0.0D);
            }
            if (enteredAlarm(previous, state)) {
                world.spawnParticles(
                        ParticleTypes.ANGRY_VILLAGER,
                        actor.getX(), actor.getY() + actor.getHeight() + 0.25D, actor.getZ(),
                        3, 0.18D, 0.12D, 0.18D, 0.0D);
            }
            if (enteredRecovering(previous, state)) {
                world.spawnParticles(
                        ParticleTypes.CLOUD,
                        actor.getX(), actor.getY() + actor.getHeight() * 0.55D, actor.getZ(),
                        3, 0.2D, 0.08D, 0.2D, 0.01D);
            }
        }
        controllers.keySet().removeIf(id -> !live.contains(id));
        lastStates.keySet().removeIf(id -> !live.contains(id));
    }

    static boolean enteredWatching(
            AmbientPokemonBehaviorController.State previous,
            AmbientPokemonBehaviorController.State current
    ) {
        return current == AmbientPokemonBehaviorController.State.WATCHING
                && previous != AmbientPokemonBehaviorController.State.WATCHING;
    }

    static boolean enteredAlarm(
            AmbientPokemonBehaviorController.State previous,
            AmbientPokemonBehaviorController.State current
    ) {
        return current == AmbientPokemonBehaviorController.State.ALARMED
                && previous != AmbientPokemonBehaviorController.State.ALARMED;
    }

    static boolean enteredRecovering(
            AmbientPokemonBehaviorController.State previous,
            AmbientPokemonBehaviorController.State current
    ) {
        return current == AmbientPokemonBehaviorController.State.RECOVERING
                && previous != null
                && previous != AmbientPokemonBehaviorController.State.RECOVERING;
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
