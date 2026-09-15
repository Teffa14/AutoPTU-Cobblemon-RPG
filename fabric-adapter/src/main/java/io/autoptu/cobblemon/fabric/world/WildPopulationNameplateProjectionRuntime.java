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
 * Suppresses vanilla custom-name plates while canonical WILD presentation actors are dormant.
 *
 * <p>Nameplate visibility is Minecraft presentation state only. A hidden or interaction-inactive actor must not
 * leak its location through a floating vanilla name. The previous visibility policy is restored exactly when
 * presentation resumes or the actor leaves canonical WILD projection.</p>
 */
public final class WildPopulationNameplateProjectionRuntime implements ModInitializer {
    private static final Map<ActorKey, Boolean> PREVIOUS_NAMEPLATE_VISIBILITY = new HashMap<>();

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(WildPopulationNameplateProjectionRuntime::synchronizeAllWorlds);
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) ->
                PREVIOUS_NAMEPLATE_VISIBILITY.remove(new ActorKey(world.getRegistryKey(), entity.getUuid())));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PREVIOUS_NAMEPLATE_VISIBILITY.clear());
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
            synchronizeNameplate(
                    new ActorKey(worldKey, actorId),
                    actor.isCustomNameVisible(),
                    shouldSuppressNameplate(interactionActive, actor.isInvisible()),
                    actor::setCustomNameVisible
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
        Iterator<Map.Entry<ActorKey, Boolean>> iterator = PREVIOUS_NAMEPLATE_VISIBILITY.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ActorKey, Boolean> entry = iterator.next();
            ActorKey key = entry.getKey();
            if (!key.worldKey().equals(worldKey) || projectedActorIds.contains(key.actorId())) continue;
            var actor = world.getEntity(key.actorId());
            if (actor != null && actor.isCustomNameVisible() != entry.getValue()) {
                actor.setCustomNameVisible(entry.getValue());
            }
            iterator.remove();
        }
    }

    static void synchronizeNameplate(
            ActorKey actorKey,
            boolean currentlyVisible,
            boolean dormant,
            java.util.function.Consumer<Boolean> setVisible
    ) {
        if (dormant) {
            PREVIOUS_NAMEPLATE_VISIBILITY.putIfAbsent(actorKey, currentlyVisible);
            if (currentlyVisible) setVisible.accept(false);
            return;
        }
        Boolean previous = PREVIOUS_NAMEPLATE_VISIBILITY.remove(actorKey);
        if (previous != null && currentlyVisible != previous) setVisible.accept(previous);
    }

    static boolean shouldSuppressNameplate(boolean interactionActive, boolean invisible) {
        return !interactionActive || invisible;
    }

    static void clearSnapshotsForTest() {
        PREVIOUS_NAMEPLATE_VISIBILITY.clear();
    }

    record ActorKey(RegistryKey<World> worldKey, UUID actorId) {
        ActorKey {
            if (worldKey == null) throw new IllegalArgumentException("worldKey is required");
            if (actorId == null) throw new IllegalArgumentException("actorId is required");
        }
    }
}
