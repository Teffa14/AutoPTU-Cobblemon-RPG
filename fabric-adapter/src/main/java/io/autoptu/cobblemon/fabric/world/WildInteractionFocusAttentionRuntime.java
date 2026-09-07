package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Makes the server-selected visible WILD interaction focus acknowledge the player during the same
 * safe interaction window used by the focus hold. This is presentation-only: actor identity,
 * authored alarm distance and server-observed positions are the only inputs. It deliberately yields
 * inside the ALARMED band and never reads Cobblemon Pokemon gameplay payload or PTU battle state.
 */
public final class WildInteractionFocusAttentionRuntime implements ModInitializer {
    private static final int UPDATE_INTERVAL_TICKS = 10;

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % UPDATE_INTERVAL_TICKS == 0) reconcile(server.getOverworld());
        });
    }

    static void reconcile(ServerWorld world) {
        if (world == null || world.getServer() == null || world != world.getServer().getOverworld()) return;

        List<WildEcologyProjectionRegistry.ProjectedActor> projections = WildEcologyProjectionRegistry.collect(world);
        if (projections.isEmpty()) return;

        Map<UUID, WildEcologyProjectionRegistry.ProjectedActor> byActorId = new HashMap<>();
        for (var projection : projections) {
            if (projection == null || projection.actor().isRemoved() || projection.actor().isInvisible()) continue;
            byActorId.put(projection.actor().getUuid(), projection);
        }

        Map<UUID, FocusCandidate> focusByActor = new HashMap<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isSpectator()) continue;
            WildHabitatCueRuntime.NearbyInteractionSnapshot focus =
                    WildHabitatCueRuntime.nearestInteractionActor(player, projections);
            if (focus == null) continue;

            var projection = byActorId.get(focus.actorId());
            if (projection == null) continue;
            PokemonEntity actor = projection.actor();
            double squaredDistance = actor.squaredDistanceTo(player);
            if (!shouldFaceInteractionFocus(squaredDistance, projection.behaviorProfile().alarmDistance())) continue;

            FocusCandidate candidate = new FocusCandidate(player, squaredDistance);
            focusByActor.merge(focus.actorId(), candidate, WildInteractionFocusAttentionRuntime::preferredCandidate);
        }

        for (var entry : focusByActor.entrySet()) {
            var projection = byActorId.get(entry.getKey());
            if (projection == null) continue;
            facePlayer(projection.actor(), entry.getValue().player());
        }
    }

    static boolean shouldFaceInteractionFocus(double squaredDistance, double alarmDistance) {
        return WildInteractionFocusHoldRuntime.shouldHoldInteractionFocus(squaredDistance, alarmDistance);
    }

    static UUID preferredPlayerIdentity(double firstDistanceSquared, UUID firstPlayerId,
                                        double secondDistanceSquared, UUID secondPlayerId) {
        if (firstPlayerId == null) return secondPlayerId;
        if (secondPlayerId == null) return firstPlayerId;
        int distanceOrder = Double.compare(firstDistanceSquared, secondDistanceSquared);
        if (distanceOrder < 0) return firstPlayerId;
        if (distanceOrder > 0) return secondPlayerId;
        return Comparator.<UUID>naturalOrder().compare(firstPlayerId, secondPlayerId) <= 0 ? firstPlayerId : secondPlayerId;
    }

    private static FocusCandidate preferredCandidate(FocusCandidate first, FocusCandidate second) {
        UUID preferred = preferredPlayerIdentity(
                first.squaredDistance(), first.player().getUuid(),
                second.squaredDistance(), second.player().getUuid());
        return preferred.equals(first.player().getUuid()) ? first : second;
    }

    private static void facePlayer(PokemonEntity actor, ServerPlayerEntity player) {
        double dx = player.getX() - actor.getX();
        double dz = player.getZ() - actor.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = WildCalmIdleLookRuntime.idleFacingPitch(
                actor.getY() + actor.getStandingEyeHeight(),
                player.getY() + player.getStandingEyeHeight(),
                actor.getX(), actor.getZ(), player.getX(), player.getZ());
        actor.setYaw(yaw);
        actor.setHeadYaw(yaw);
        actor.setPitch(pitch);
    }

    private record FocusCandidate(ServerPlayerEntity player, double squaredDistance) {}
}
