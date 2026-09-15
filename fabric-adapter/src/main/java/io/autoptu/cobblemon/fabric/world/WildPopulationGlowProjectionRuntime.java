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
 * Suppresses vanilla glowing outlines while canonical WILD presentation actors are dormant.
 *
 * <p>Glowing is Minecraft presentation state only. Hidden or interaction-inactive actors must not leak their
 * location through a vanilla outline while canonical ecology presence is suspended. The actor's prior glowing
 * policy is restored exactly when presentation resumes or the actor leaves the canonical WILD projection.</p>
 */
public final class WildPopulationGlowProjectionRuntime implements ModInitializer {
    private static final Map<ActorKey, Boolean> PREVIOUS_GLOWING = new HashMap<>();

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(WildPopulationGlowProjectionRuntime::synchronizeAllWorlds);
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) ->
                PREVIOUS_GLOWING.remove(new ActorKey(world.getRegistryKey(), entity.getUuid())));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PREVIOUS_GLOWING.clear());
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
            synchronizeGlow(
                    new ActorKey(worldKey, actorId),
                    actor.isGlowing(),
                    shouldSuppressGlow(interactionActive, actor.isInvisible()),
                    actor::setGlowing
            );
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
        Iterator<Map.Entry<ActorKey, Boolean>> iterator = PREVIOUS_GLOWING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ActorKey, Boolean> entry = iterator.next();
            ActorKey key = entry.getKey();
            if (!key.worldKey().equals(worldKey) || projectedActorIds.contains(key.actorId())) continue;
            var actor = world.getEntity(key.actorId());
            if (actor != null && actor.isGlowing() != entry.getValue()) actor.setGlowing(entry.getValue());
            iterator.remove();
        }
    }

    static void synchronizeGlow(
            ActorKey actorKey,
            boolean currentlyGlowing,
            boolean dormant,
            java.util.function.Consumer<Boolean> setGlowing
    ) {
        if (dormant) {
            PREVIOUS_GLOWING.putIfAbsent(actorKey, currentlyGlowing);
            if (currentlyGlowing) setGlowing.accept(false);
            return;
        }
        Boolean previous = PREVIOUS_GLOWING.remove(actorKey);
        if (previous != null && currentlyGlowing != previous) setGlowing.accept(previous);
    }

    static boolean shouldSuppressGlow(boolean interactionActive, boolean invisible) {
        return !interactionActive || invisible;
    }

    static void clearSnapshotsForTest() {
        PREVIOUS_GLOWING.clear();
    }

    record ActorKey(RegistryKey<World> worldKey, UUID actorId) {
        ActorKey {
            if (worldKey == null) throw new IllegalArgumentException("worldKey is required");
            if (actorId == null) throw new IllegalArgumentException("actorId is required");
        }
    }
}
