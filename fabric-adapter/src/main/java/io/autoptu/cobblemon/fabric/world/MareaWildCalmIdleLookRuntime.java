package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * Minecraft-only idle presentation for canonical Marea roaming actors.
 *
 * During the authored CALM rest cadence, an otherwise idle actor turns toward the interior of its
 * current habitat with a small deterministic scan offset. Nearby players, active navigation and
 * meaningful horizontal motion always suppress this behavior so WATCHING/ALARMED/recovery and
 * native pathing keep priority. This runtime never reads Cobblemon Pokemon gameplay payload or
 * supplies PTU legality, movement, initiative, RNG or outcomes.
 */
public final class MareaWildCalmIdleLookRuntime implements ModInitializer {
    private static final int UPDATE_INTERVAL_TICKS = 10;
    private static final double MAX_IDLE_HORIZONTAL_SPEED = 0.001D;
    private static final double PLAYER_GUARD_RADIUS = 14.0D;

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % UPDATE_INTERVAL_TICKS != 0) return;
            update(server.getOverworld());
        });
    }

    static void update(ServerWorld world) {
        if (world == null || MareaWildAmbientBehaviorRuntime.calmWanderActive(world.getTime())) return;

        for (var population : CanonicalWildPopulationCatalogue.DEFAULT.populations()) {
            if (!population.siteId().startsWith("ouros.marea.")) continue;
            var projectedSiteId = MareaWildMigrationProjection.projectedSiteId(population, world.getTime());
            if (projectedSiteId.isEmpty()) continue;

            for (var encounter : CanonicalWildPopulationCatalogue.DEFAULT.members(population)) {
                var boundUuid = VisibleWildPokemonEncounterRuntime.boundEntityUuid(encounter.canonicalEncounterId());
                if (boundUuid.isEmpty()) continue;
                var loaded = world.getEntity(boundUuid.get());
                if (!(loaded instanceof PokemonEntity actor) || actor.isRemoved() || actor.isInvisible()) continue;
                if (!VisibleWildPokemonEncounterRuntime.isInteractionActive(actor.getUuid())) continue;
                if (!actor.getNavigation().isIdle()) continue;

                var velocity = actor.getVelocity();
                double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
                if (horizontalSpeed > MAX_IDLE_HORIZONTAL_SPEED) continue;
                if (hasNearbyPlayer(world, actor, PLAYER_GUARD_RADIUS)) continue;

                BlockPos anchor = MareaVisibleWildPokemonRuntime.projectedPresentationAnchor(
                        encounter,
                        projectedSiteId.get());
                float yaw = idleFacingYaw(
                        actor.getUuid(),
                        world.getTime(),
                        actor.getX(),
                        actor.getZ(),
                        anchor.getX() + 0.5D,
                        anchor.getZ() + 0.5D);
                actor.setYaw(yaw);
                actor.setHeadYaw(yaw);
            }
        }
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
            double centerZ
    ) {
        if (actorId == null
                || !Double.isFinite(actorX) || !Double.isFinite(actorZ)
                || !Double.isFinite(centerX) || !Double.isFinite(centerZ)) {
            throw new IllegalArgumentException("idle facing requires actor identity and finite coordinates");
        }

        double dx = centerX - actorX;
        double dz = centerZ - actorZ;
        float inwardYaw;
        if (Math.abs(dx) <= 0.000001D && Math.abs(dz) <= 0.000001D) {
            inwardYaw = 0.0F;
        } else {
            inwardYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        }

        long restWindow = Math.floorDiv(worldTime, 80L);
        long mixed = actorId.getMostSignificantBits()
                ^ Long.rotateLeft(actorId.getLeastSignificantBits(), 19)
                ^ (restWindow * 0x9E3779B97F4A7C15L);
        int scanIndex = Math.floorMod((int) (mixed ^ (mixed >>> 32)), 3);
        float scanOffset = switch (scanIndex) {
            case 0 -> -35.0F;
            case 1 -> 0.0F;
            default -> 35.0F;
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
