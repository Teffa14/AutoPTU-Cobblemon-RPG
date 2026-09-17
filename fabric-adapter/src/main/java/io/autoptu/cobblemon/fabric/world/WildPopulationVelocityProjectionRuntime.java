package io.autoptu.cobblemon.fabric.world;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityPose;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/**
 * Suspends residual Minecraft locomotion and native combat presentation state while a canonical WILD actor is dormant.
 *
 * <p>Velocity, sprinting/sneaking/swimming, native pose, navigation, vanilla/Cobblemon targets, attacker memory, vanilla fire,
 * accumulated fall distance, vanilla hurt/death animation time, native air depletion, vanilla freezing and embedded
 * projectile counters are presentation/world state only. AutoPTU-Java remains authoritative for tactical movement,
 * targeting, forced movement, reactions, damage, statuses and battle outcomes. Hidden or interaction-inactive actors must
 * not keep following a stale path, drift away from their server-authored ecology projection, retain native combat memory,
 * keep vanilla damage/death presentation alive, carry dormant physics into a later visible projection, surface with a
 * depleted Minecraft air meter, thaw into native freeze damage, reappear with stale arrow/stinger damage presentation, or
 * retain stale native locomotion/pose state.</p>
 */
public final class WildPopulationVelocityProjectionRuntime implements ModInitializer {
    private static final double EPSILON_SQUARED = 1.0e-8;

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(WildPopulationVelocityProjectionRuntime::synchronizeAllWorlds);
    }

    private static void synchronizeAllWorlds(MinecraftServer server) {
        if (server == null) return;
        for (ServerWorld world : server.getWorlds()) synchronize(world);
    }

    static int synchronize(ServerWorld world) {
        if (world == null) throw new IllegalArgumentException("world is required");
        int suspendedActors = 0;
        for (var projected : WildEcologyProjectionSource.collect(world)) {
            var actor = projected.actor();
            boolean interactionActive = VisibleWildPokemonEncounterRuntime.isInteractionActive(actor.getUuid());
            boolean dormant = shouldSuspendPresentation(interactionActive, actor.isInvisible());
            if (!dormant) continue;

            boolean changed = false;
            var navigation = actor.getNavigation();
            if (!navigation.isIdle()) {
                navigation.stop();
                changed = true;
            }

            if (actor.isSprinting()) {
                actor.setSprinting(false);
                changed = true;
            }

            if (actor.isSneaking()) {
                actor.setSneaking(false);
                changed = true;
            }

            if (actor.isSwimming()) {
                actor.setSwimming(false);
                changed = true;
            }

            if (hasResidualNativePose(actor.getPose())) {
                actor.setPose(EntityPose.STANDING);
                changed = true;
            }

            if (actor.getTarget() != null) {
                actor.setTarget(null);
                changed = true;
            }

            if (actor.getAttacker() != null) {
                actor.setAttacker(null);
                changed = true;
            }

            if (actor.isOnFire()) {
                actor.extinguish();
                changed = true;
            }

            if (hasAccumulatedFallDistance(actor.fallDistance)) {
                actor.fallDistance = 0.0F;
                changed = true;
            }

            if (hasNativeHurtPresentation(actor.hurtTime)) {
                actor.hurtTime = 0;
                changed = true;
            }

            if (hasNativeDeathPresentation(actor.deathTime)) {
                actor.deathTime = 0;
                changed = true;
            }

            if (hasDepletedNativeAir(actor.getAir(), actor.getMaxAir())) {
                actor.setAir(actor.getMaxAir());
                changed = true;
            }

            if (hasNativeFreezeProgress(actor.getFrozenTicks())) {
                actor.setFrozenTicks(0);
                changed = true;
            }

            if (hasNativeProjectilePresentation(actor.getStuckArrowCount(), actor.getStingerCount())) {
                actor.setStuckArrowCount(0);
                actor.setStingerCount(0);
                changed = true;
            }

            Vec3d velocity = actor.getVelocity();
            if (hasResidualMotion(velocity.lengthSquared())) {
                actor.setVelocity(Vec3d.ZERO);
                actor.velocityModified = true;
                changed = true;
            }

            if (changed) suspendedActors++;
        }
        return suspendedActors;
    }

    static boolean shouldSuspendPresentation(boolean interactionActive, boolean invisible) {
        return !interactionActive || invisible;
    }

    static boolean hasResidualMotion(double velocitySquared) {
        return velocitySquared > EPSILON_SQUARED;
    }

    static boolean hasResidualNativePose(EntityPose pose) {
        return pose != null && pose != EntityPose.STANDING;
    }

    static boolean hasAccumulatedFallDistance(float fallDistance) {
        return fallDistance > 0.0F;
    }

    static boolean hasNativeHurtPresentation(int hurtTime) {
        return hurtTime > 0;
    }

    static boolean hasNativeDeathPresentation(int deathTime) {
        return deathTime > 0;
    }

    static boolean hasDepletedNativeAir(int air, int maxAir) {
        return air < maxAir;
    }

    static boolean hasNativeFreezeProgress(int frozenTicks) {
        return frozenTicks > 0;
    }

    static boolean hasNativeProjectilePresentation(int stuckArrowCount, int stingerCount) {
        return stuckArrowCount > 0 || stingerCount > 0;
    }

    static boolean shouldStopResidualMotion(boolean interactionActive, boolean invisible, double velocitySquared) {
        return shouldSuspendPresentation(interactionActive, invisible) && hasResidualMotion(velocitySquared);
    }

    static boolean shouldClearNativeSprint(boolean interactionActive, boolean invisible, boolean sprinting) {
        return shouldSuspendPresentation(interactionActive, invisible) && sprinting;
    }

    static boolean shouldClearNativeSneak(boolean interactionActive, boolean invisible, boolean sneaking) {
        return shouldSuspendPresentation(interactionActive, invisible) && sneaking;
    }

    static boolean shouldClearNativeSwimming(boolean interactionActive, boolean invisible, boolean swimming) {
        return shouldSuspendPresentation(interactionActive, invisible) && swimming;
    }

    static boolean shouldResetNativePose(boolean interactionActive, boolean invisible, EntityPose pose) {
        return shouldSuspendPresentation(interactionActive, invisible) && hasResidualNativePose(pose);
    }

    static boolean shouldClearFire(boolean interactionActive, boolean invisible, boolean onFire) {
        return shouldSuspendPresentation(interactionActive, invisible) && onFire;
    }

    static boolean shouldClearFallDistance(boolean interactionActive, boolean invisible, float fallDistance) {
        return shouldSuspendPresentation(interactionActive, invisible) && hasAccumulatedFallDistance(fallDistance);
    }

    static boolean shouldClearNativeHurtPresentation(boolean interactionActive, boolean invisible, int hurtTime) {
        return shouldSuspendPresentation(interactionActive, invisible) && hasNativeHurtPresentation(hurtTime);
    }

    static boolean shouldClearNativeDeathPresentation(boolean interactionActive, boolean invisible, int deathTime) {
        return shouldSuspendPresentation(interactionActive, invisible) && hasNativeDeathPresentation(deathTime);
    }

    static boolean shouldRestoreNativeAir(boolean interactionActive, boolean invisible, int air, int maxAir) {
        return shouldSuspendPresentation(interactionActive, invisible) && hasDepletedNativeAir(air, maxAir);
    }

    static boolean shouldClearNativeFreezeProgress(boolean interactionActive, boolean invisible, int frozenTicks) {
        return shouldSuspendPresentation(interactionActive, invisible) && hasNativeFreezeProgress(frozenTicks);
    }

    static boolean shouldClearNativeProjectilePresentation(
            boolean interactionActive, boolean invisible, int stuckArrowCount, int stingerCount) {
        return shouldSuspendPresentation(interactionActive, invisible)
                && hasNativeProjectilePresentation(stuckArrowCount, stingerCount);
    }
}
