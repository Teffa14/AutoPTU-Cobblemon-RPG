package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalFastTravelCatalogue;
import io.autoptu.cobblemon.authority.CanonicalFastTravelService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * Read-only server-authoritative fast-travel catalogue command.
 *
 * <p>The command accepts no destination metadata from the client. It enumerates only the authored
 * catalogue and evaluates current readiness through the same {@link CanonicalFastTravelService}
 * used by the lodestone interaction. It performs no teleport; execution belongs to the normal
 * world interaction and the explicit destination command surface.
 */
public final class FabricTravelCommandRuntime {
    private static final int SEARCH_RADIUS = 5;
    private static final CanonicalFastTravelService SERVICE =
            new CanonicalFastTravelService(FabricFastTravelRuntime.MAX_DISTANCE_SQUARED);

    private FabricTravelCommandRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("travel")
                                .executes(context -> listDestinations(context.getSource())))));
    }

    private static int listDestinations(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Travel destinations must be requested by an authenticated player."));
            return 0;
        }

        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        boolean trainerExists = FabricCanonicalPlayerStoreRuntime.requireRepository(player.getServer())
                .findPlayer(playerId)
                .isPresent();
        ServerWorld world = player.getServerWorld();
        BlockPos sourcePoint = findNearestObservedTravelPoint(world, player);

        player.sendMessage(Text.literal("AutoPTU travel destinations:"), false);
        for (CanonicalFastTravelCatalogue.Destination destination : CanonicalFastTravelCatalogue.destinations()) {
            boolean sourceObserved = sourcePoint != null;
            String sourcePointId = sourceObserved
                    ? FabricFastTravelRuntime.sourceId(world, sourcePoint)
                    : "server:no_observed_lodestone";
            double distanceSquared = sourceObserved
                    ? player.squaredDistanceTo(
                            sourcePoint.getX() + 0.5D,
                            sourcePoint.getY() + 0.5D,
                            sourcePoint.getZ() + 0.5D)
                    : Double.POSITIVE_INFINITY;

            CanonicalFastTravelService.Decision decision = SERVICE.canTravel(
                    new CanonicalFastTravelService.Request(
                            playerId,
                            trainerExists,
                            sourcePointId,
                            sourceObserved,
                            distanceSquared,
                            destination.id(),
                            CanonicalFastTravelCatalogue.find(destination.id()).isPresent(),
                            true
                    )
            );
            String state = decision.allowed() ? "READY" : "UNAVAILABLE: " + decision.reason();
            player.sendMessage(Text.literal(
                    "- " + destination.displayName() + " [" + destination.id() + "] — " + state), false);
        }
        return 1;
    }

    static BlockPos findNearestObservedTravelPoint(ServerWorld world, ServerPlayerEntity player) {
        BlockPos origin = player.getBlockPos();
        BlockPos best = null;
        double bestDistanceSquared = Double.POSITIVE_INFINITY;

        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dy = -SEARCH_RADIUS; dy <= SEARCH_RADIUS; dy++) {
                for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                    BlockPos candidate = origin.add(dx, dy, dz);
                    if (!FabricFastTravelRuntime.isFastTravelPoint(world, candidate)) continue;
                    double distanceSquared = player.squaredDistanceTo(
                            candidate.getX() + 0.5D,
                            candidate.getY() + 0.5D,
                            candidate.getZ() + 0.5D);
                    if (distanceSquared <= FabricFastTravelRuntime.MAX_DISTANCE_SQUARED
                            && distanceSquared < bestDistanceSquared) {
                        best = candidate.toImmutable();
                        bestDistanceSquared = distanceSquared;
                    }
                }
            }
        }
        return best;
    }
}
