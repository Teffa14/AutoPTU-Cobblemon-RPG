package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalFastTravelCatalogue;
import io.autoptu.cobblemon.authority.CanonicalFastTravelService;
import io.autoptu.cobblemon.authority.CanonicalWorldMapCatalogue;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import java.util.Optional;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Minecraft-native fast travel. Discovery and destination coordinates are server authoritative. */
public final class FabricFastTravelRuntime {
    static final String OVERWORLD_SPAWN_DESTINATION = CanonicalFastTravelCatalogue.OVERWORLD_SPAWN_ID;
    static final double MAX_DISTANCE_SQUARED = 25.0D;
    private static final CanonicalFastTravelService SERVICE = new CanonicalFastTravelService(MAX_DISTANCE_SQUARED);

    private FabricFastTravelRuntime() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient() || hand != Hand.MAIN_HAND || !(player instanceof ServerPlayerEntity serverPlayer)
                    || !serverPlayer.isSneaking() || !serverPlayer.getStackInHand(hand).isEmpty()
                    || !isFastTravelPoint(world, hitResult.getBlockPos())) return ActionResult.PASS;
            boolean traveled = attemptTravel(serverPlayer, hitResult.getBlockPos(), OVERWORLD_SPAWN_DESTINATION);
            return traveled ? ActionResult.SUCCESS : ActionResult.FAIL;
        });
    }

    static boolean attemptTravel(ServerPlayerEntity player, BlockPos source, String requestedDestinationId) {
        MinecraftServer server = player.getServer();
        ServerWorld sourceWorld = player.getServerWorld();
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        boolean trainerExists = FabricCanonicalPlayerStoreRuntime.requireRepository(server).findPlayer(playerId).isPresent();
        Optional<CanonicalFastTravelCatalogue.Destination> destination = CanonicalFastTravelCatalogue.find(requestedDestinationId);
        CanonicalFastTravelService.Decision decision = SERVICE.canTravel(new CanonicalFastTravelService.Request(
                playerId, trainerExists, sourceId(sourceWorld, source), isFastTravelPoint(sourceWorld, source),
                player.squaredDistanceTo(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D),
                requestedDestinationId, destination.isPresent(),
                destination.filter(value -> destinationUnlocked(server, playerId, value)).isPresent(),
                destination.filter(value -> destinationAvailable(server, value)).isPresent()));
        if (!decision.allowed()) {
            player.sendMessage(Text.literal("Fast travel denied: " + decision.reason()), true);
            return false;
        }
        CanonicalFastTravelCatalogue.Destination authoredDestination = destination.orElseThrow();
        if (!teleportToDestination(player, authoredDestination)) {
            player.sendMessage(Text.literal("Fast travel denied: server destination mapping is unavailable."), true);
            return false;
        }
        player.sendMessage(Text.literal("Fast traveled to " + authoredDestination.displayName() + "."), false);
        return true;
    }

    static boolean destinationUnlocked(MinecraftServer server, String playerId, CanonicalFastTravelCatalogue.Destination destination) {
        return FabricCanonicalPlayerStoreRuntime.requireLocationDiscoveryRepository(server).find(playerId)
                .map(state -> state.locationIds().contains(destination.id())).orElse(false);
    }

    static boolean destinationAvailable(MinecraftServer server, CanonicalFastTravelCatalogue.Destination destination) {
        if (server.getOverworld() == null) return false;
        if (CanonicalFastTravelCatalogue.OVERWORLD_SPAWN_ID.equals(destination.id())) return true;
        return CanonicalWorldMapCatalogue.DEFAULT.site(destination.id()).isPresent();
    }

    private static boolean teleportToDestination(ServerPlayerEntity player, CanonicalFastTravelCatalogue.Destination destination) {
        ServerWorld destinationWorld = player.getServer().getOverworld();
        if (destinationWorld == null) return false;
        BlockPos destinationPos;
        if (CanonicalFastTravelCatalogue.OVERWORLD_SPAWN_ID.equals(destination.id())) {
            destinationPos = destinationWorld.getSpawnPos();
        } else {
            var site = CanonicalWorldMapCatalogue.DEFAULT.site(destination.id()).orElse(null);
            if (site == null || !"minecraft:overworld".equals(site.dimensionId())) return false;
            destinationPos = new BlockPos(site.x(), site.y(), site.z());
        }
        player.teleport(destinationWorld, destinationPos.getX() + 0.5D, destinationPos.getY() + 0.1D,
                destinationPos.getZ() + 0.5D, player.getYaw(), player.getPitch());
        return true;
    }

    static boolean isFastTravelPoint(World world, BlockPos pos) { return world.getBlockState(pos).isOf(Blocks.LODESTONE); }
    static String sourceId(World world, BlockPos pos) {
        return world.getRegistryKey().getValue() + ":" + pos.getX() + ":" + pos.getY() + ":" + pos.getZ();
    }
}
