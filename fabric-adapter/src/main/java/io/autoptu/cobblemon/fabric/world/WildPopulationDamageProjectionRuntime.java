package io.autoptu.cobblemon.fabric.world;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Protects dormant canonical WILD presentation actors from vanilla Minecraft damage.
 *
 * <p>AutoPTU-Java remains authoritative for battle damage, HP and outcomes. This runtime only prevents a hidden or
 * otherwise interaction-inactive Cobblemon presentation body from being destroyed by environmental/vanilla damage
 * while the canonical WILD identity remains suspended. When presentation resumes, the actor's pre-suspension
 * invulnerability flag is restored exactly.</p>
 */
public final class WildPopulationDamageProjectionRuntime implements ModInitializer {
    private static final Map<UUID, Boolean> PREVIOUS_INVULNERABILITY = new HashMap<>();

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(WildPopulationDamageProjectionRuntime::synchronizeAllWorlds);
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> PREVIOUS_INVULNERABILITY.remove(entity.getUuid()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PREVIOUS_INVULNERABILITY.clear());
    }

    private static void synchronizeAllWorlds(MinecraftServer server) {
        if (server == null
                || server.getTicks() % WildPopulationRuntime.presenceReconcileIntervalTicks() != 0) {
            return;
        }
        for (ServerWorld world : server.getWorlds()) synchronize(world);
    }

    static int synchronize(ServerWorld world) {
        if (world == null) throw new IllegalArgumentException("world is required");
        int synchronizedActors = 0;
        Set<UUID> projectedActorIds = new HashSet<>();
        for (var projected : WildEcologyProjectionSource.collect(world)) {
            var actor = projected.actor();
            UUID actorId = actor.getUuid();
            projectedActorIds.add(actorId);
            boolean interactionActive = VisibleWildPokemonEncounterRuntime.isInteractionActive(actorId);
            boolean dormant = shouldShield(interactionActive, actor.isInvisible());
            synchronizeShield(actorId, actor.isInvulnerable(), dormant, actor::setInvulnerable);
            synchronizedActors++;
        }

        // A projected actor can leave this world's ecology between reconciliation passes without unloading the entity.
        // Drop only stale snapshots here; never mutate an actor that is no longer a canonical WILD projection.
        PREVIOUS_INVULNERABILITY.keySet().removeIf(id -> !projectedActorIds.contains(id));
        return synchronizedActors;
    }

    private static void synchronizeShield(
            UUID actorId,
            boolean currentlyInvulnerable,
            boolean dormant,
            java.util.function.Consumer<Boolean> setInvulnerable
    ) {
        if (dormant) {
            PREVIOUS_INVULNERABILITY.putIfAbsent(actorId, currentlyInvulnerable);
            if (!currentlyInvulnerable) setInvulnerable.accept(true);
            return;
        }

        Boolean previous = PREVIOUS_INVULNERABILITY.remove(actorId);
        if (previous != null && currentlyInvulnerable != previous) {
            setInvulnerable.accept(previous);
        }
    }

    static boolean shouldShield(boolean interactionActive, boolean invisible) {
        return !interactionActive || invisible;
    }

    static boolean restoredInvulnerability(boolean previousInvulnerability) {
        return previousInvulnerability;
    }
}
