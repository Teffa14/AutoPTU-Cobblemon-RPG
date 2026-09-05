package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.UUID;

/**
 * Generic Minecraft-only calm/rest presentation for every registered visible-wild population.
 *
 * Population/region code supplies only a projected actor, habitat center and server-authored
 * behavior profile through {@link WildEcologyProjectionRegistry}. This runtime contains no region,
 * species or PTU rule knowledge.
 */
public final class WildCalmIdleLookRuntime implements ModInitializer {
    private static final int UPDATE_INTERVAL_TICKS = 10;

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % UPDATE_INTERVAL_TICKS != 0) return;
            update(server.getOverworld());
        });
    }

    static void update(ServerWorld world) {
        if (world == null) return;
        for (WildEcologyProjectionRegistry.ProjectedActor projection : WildEcologyProjectionRegistry.collect(world)) {
            apply(world, projection);
        }
    }

    static boolean apply(ServerWorld world, WildEcologyProjectionRegistry.ProjectedActor projection) {
        if (world == null || projection == null) return false;
        PokemonEntity actor = projection.actor();
        WildBehaviorProfile profile = projection.behaviorProfile();
        if (profile.calmMovementActive(world.getTime())) return false;
        if (actor.isRemoved() || actor.isInvisible()) return false;
        if (!VisibleWildPokemonEncounterRuntime.isInteractionActive(actor.getUuid())) return false;
        if (!actor.getNavigation().isIdle()) return false;

        var velocity = actor.getVelocity();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (horizontalSpeed > profile.maxIdleHorizontalSpeed()) return false;
        if (hasNearbyPlayer(world, actor, profile.playerGuardRadius())) return false;

        float yaw = idleFacingYaw(
                actor.getUuid(),
                world.getTime(),
                actor.getX(),
                actor.getZ(),
                projection.habitatCenterX(),
                projection.habitatCenterZ(),
                profile.calmSegmentTicks(),
                profile.idleScanDegrees());
        actor.setYaw(yaw);
        actor.setHeadYaw(yaw);
        return true;
    }

    private static boolean hasNearbyPlayer(ServerWorld world, PokemonEntity actor, double radius) {
        double radiusSquared = radius * radius;
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isSpectator()) continue;
            if (actor.squaredDistanceTo(player) <= radiusSquared) return true;
        }
        return false;
    }

    static float idleFacingYaw(
            UUID actorId,
            long worldTime,
            double actorX,
            double actorZ,
            double centerX,
            double centerZ,
            long restWindowTicks,
            float scanDegrees
    ) {
        if (actorId == null
                || !Double.isFinite(actorX) || !Double.isFinite(actorZ)
                || !Double.isFinite(centerX) || !Double.isFinite(centerZ)
                || restWindowTicks <= 0L
                || !Float.isFinite(scanDegrees) || scanDegrees < 0.0F || scanDegrees > 180.0F) {
            throw new IllegalArgumentException("idle facing requires actor identity, finite coordinates and valid presentation policy");
        }

        double dx = centerX - actorX;
        double dz = centerZ - actorZ;
        float inwardYaw;
        if (Math.abs(dx) <= 0.000001D && Math.abs(dz) <= 0.000001D) {
            inwardYaw = 0.0F;
        } else {
            inwardYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        }

        long restWindow = Math.floorDiv(worldTime, restWindowTicks);
        long mixed = actorId.getMostSignificantBits()
                ^ Long.rotateLeft(actorId.getLeastSignificantBits(), 19)
                ^ (restWindow * 0x9E3779B97F4A7C15L);
        int scanIndex = Math.floorMod((int) (mixed ^ (mixed >>> 32)), 3);
        float scanOffset = switch (scanIndex) {
            case 0 -> -scanDegrees;
            case 1 -> 0.0F;
            default -> scanDegrees;
        };
        return wrapDegrees(inwardYaw + scanOffset);
    }

    private static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360.0F;
        if (wrapped >= 180.0F) wrapped -= 360.0F;
        if (wrapped < -180.0F) wrapped += 360.0F;
        return wrapped;
    }
}
