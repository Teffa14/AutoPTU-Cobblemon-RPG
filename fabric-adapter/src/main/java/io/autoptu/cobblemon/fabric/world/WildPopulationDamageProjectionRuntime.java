package io.autoptu.cobblemon.fabric.world;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
    private static final Map<ActorKey, Boolean> PREVIOUS_INVULNERABILITY = new HashMap<>();

    @Override
    public void onInitialize() {
        // Vanilla/environmental damage can happen on any entity tick. Keep the shield aligned every server tick rather
        // than waiting for the slower population-presence cadence.
        ServerTickEvents.END_SERVER_TICK.register(WildPopulationDamageProjectionRuntime::synchronizeAllWorlds);
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) ->
                PREVIOUS_INVULNERABILITY.remove(new ActorKey(world.getRegistryKey(), entity.getUuid())));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PREVIOUS_INVULNERABILITY.clear());
    }

    private static void synchronizeAllWorlds(MinecraftServer server) {
        if (server == null) return;
        for (ServerWorld world : server.getWorlds()) synchronize(world);
    }

    static int synchronize(ServerWorld world) {
        if (world == null) throw new IllegalArgumentException("world is required");
        int synchronizedActors = 0;
        Set<UUID> projectedActorIds = new HashSet<>();
        RegistryKey<World> worldKey = world.getRegistryKey();
        for (var projected : WildEcologyProjectionSource.collect(world)) {
            var actor = projected.actor();
            UUID actorId = actor.getUuid();
            projectedActorIds.add(actorId);
            boolean interactionActive = VisibleWildPokemonEncounterRuntime.isInteractionActive(actorId);
            boolean dormant = shouldShield(interactionActive, actor.isInvisible());
            synchronizeShield(new ActorKey(worldKey, actorId), actor.isInvulnerable(), dormant, actor::setInvulnerable);
            synchronizedActors++;
        }

        restoreActorsLeavingProjection(world, worldKey, projectedActorIds);
        return synchronizedActors;
    }

    private static void restoreActorsLeavingProjection(
            ServerWorld world,
            RegistryKey<World> worldKey,
            Set<UUID> projectedActorIds
    ) {
        Iterator<Map.Entry<ActorKey, Boolean>> iterator = PREVIOUS_INVULNERABILITY.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ActorKey, Boolean> entry = iterator.next();
            ActorKey key = entry.getKey();
            if (!key.worldKey().equals(worldKey) || projectedActorIds.contains(key.actorId())) continue;

            // This runtime owns the temporary shield only while the actor is a canonical WILD projection. If the
            // entity remains loaded but leaves that projection, release the temporary Minecraft state before
            // forgetting our snapshot. A different dimension cannot erase this world's snapshot because worldKey is
            // part of the key.
            var actor = world.getEntity(key.actorId());
            if (actor != null && actor.isInvulnerable() != entry.getValue()) {
                actor.setInvulnerable(entry.getValue());
            }
            iterator.remove();
        }
    }

    private static void synchronizeShield(
            ActorKey actorKey,
            boolean currentlyInvulnerable,
            boolean dormant,
            java.util.function.Consumer<Boolean> setInvulnerable
    ) {
        if (dormant) {
            PREVIOUS_INVULNERABILITY.putIfAbsent(actorKey, currentlyInvulnerable);
            if (!currentlyInvulnerable) setInvulnerable.accept(true);
            return;
        }

        Boolean previous = PREVIOUS_INVULNERABILITY.remove(actorKey);
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

    private record ActorKey(RegistryKey<World> worldKey, UUID actorId) {
        private ActorKey {
            if (worldKey == null) throw new IllegalArgumentException("worldKey is required");
            if (actorId == null) throw new IllegalArgumentException("actorId is required");
        }
    }
}
