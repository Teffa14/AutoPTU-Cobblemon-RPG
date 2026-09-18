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
 * Protects canonical WILD presentation actors from vanilla Minecraft damage.
 *
 * <p>AutoPTU-Java remains authoritative for battle damage, HP, statuses and outcomes. A projected Cobblemon body is
 * therefore never allowed to make HP/death decisions through Minecraft damage, regardless of whether that presentation
 * actor is currently encounter-active or dormant. Native Minecraft health is kept at the presentation actor's native
 * maximum, native absorption and stuck-arrow damage residue are cleared, while fire, freezing, air depletion, accumulated
 * fall distance, residual hurt animation and residual death animation are cleared during projection. These presentation
 * repairs never read canonical Pokemon HP and prevent vanilla environmental state from implying or deferring canonical
 * Pokemon damage, status or faint. When an actor leaves the canonical WILD projection, its pre-projection invulnerability
 * flag is restored exactly.</p>
 */
public final class WildPopulationDamageProjectionRuntime implements ModInitializer {
    private static final Map<ActorKey, Boolean> PREVIOUS_INVULNERABILITY = new HashMap<>();

    @Override
    public void onInitialize() {
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
            synchronizeShield(new ActorKey(worldKey, actorId), actor.isInvulnerable(), actor::setInvulnerable);
            if (shouldRestoreNativeHealth(true, actor.getHealth(), actor.getMaxHealth())) actor.setHealth(actor.getMaxHealth());
            if (shouldClearNativeAbsorption(true, actor.getAbsorptionAmount())) actor.setAbsorptionAmount(0.0F);
            if (shouldClearNativeStuckArrows(true, actor.getStuckArrowCount())) actor.setStuckArrowCount(0);
            if (shouldExtinguishCanonicalProjection(true, actor.isOnFire())) actor.extinguish();
            if (shouldClearNativeFreezing(true, actor.getFrozenTicks())) actor.setFrozenTicks(0);
            if (shouldRestoreNativeAir(true, actor.getAir(), actor.getMaxAir())) actor.setAir(actor.getMaxAir());
            if (shouldClearNativeFallDistance(true, actor.fallDistance)) actor.fallDistance = 0.0F;
            if (shouldClearNativeHurtAnimation(true, actor.hurtTime)) actor.hurtTime = 0;
            if (shouldClearNativeDeathAnimation(true, actor.deathTime)) actor.deathTime = 0;
            synchronizedActors++;
        }

        restoreActorsLeavingProjection(world, worldKey, projectedActorIds);
        return synchronizedActors;
    }

    private static void restoreActorsLeavingProjection(ServerWorld world, RegistryKey<World> worldKey, Set<UUID> projectedActorIds) {
        Iterator<Map.Entry<ActorKey, Boolean>> iterator = PREVIOUS_INVULNERABILITY.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ActorKey, Boolean> entry = iterator.next();
            ActorKey key = entry.getKey();
            if (!key.worldKey().equals(worldKey) || projectedActorIds.contains(key.actorId())) continue;
            var actor = world.getEntity(key.actorId());
            if (actor != null && actor.isInvulnerable() != entry.getValue()) actor.setInvulnerable(entry.getValue());
            iterator.remove();
        }
    }

    private static void synchronizeShield(ActorKey actorKey, boolean currentlyInvulnerable, java.util.function.Consumer<Boolean> setInvulnerable) {
        PREVIOUS_INVULNERABILITY.putIfAbsent(actorKey, currentlyInvulnerable);
        if (!currentlyInvulnerable) setInvulnerable.accept(true);
    }

    static boolean shouldShieldCanonicalProjection(boolean projected) { return projected; }
    static boolean shouldRestoreNativeHealth(boolean projected, float health, float maxHealth) {
        return projected && Float.isFinite(health) && Float.isFinite(maxHealth) && maxHealth > 0.0F && health < maxHealth;
    }
    static boolean shouldClearNativeAbsorption(boolean projected, float absorption) {
        return projected && Float.isFinite(absorption) && absorption > 0.0F;
    }
    static boolean shouldClearNativeStuckArrows(boolean projected, int stuckArrowCount) { return projected && stuckArrowCount > 0; }
    static boolean shouldExtinguishCanonicalProjection(boolean projected, boolean onFire) { return projected && onFire; }
    static boolean shouldClearNativeFreezing(boolean projected, int frozenTicks) { return projected && frozenTicks > 0; }
    static boolean shouldRestoreNativeAir(boolean projected, int air, int maxAir) { return projected && air < maxAir; }
    static boolean shouldClearNativeFallDistance(boolean projected, float fallDistance) { return projected && fallDistance > 0.0F; }
    static boolean shouldClearNativeHurtAnimation(boolean projected, int hurtTime) { return projected && hurtTime > 0; }
    static boolean shouldClearNativeDeathAnimation(boolean projected, int deathTime) { return projected && deathTime > 0; }
    static boolean restoredInvulnerability(boolean previousInvulnerability) { return previousInvulnerability; }

    private record ActorKey(RegistryKey<World> worldKey, UUID actorId) {
        private ActorKey {
            if (worldKey == null) throw new IllegalArgumentException("worldKey is required");
            if (actorId == null) throw new IllegalArgumentException("actorId is required");
        }
    }
}
