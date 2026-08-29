package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalFastTravelService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Minecraft-native fast travel surface.
 *
 * Lodestones stay normal vanilla blocks. Sneak-use with an empty main hand asks the server to
 * travel to the server-owned Overworld spawn. Compass use is deliberately left to vanilla so the
 * normal lodestone/compass mechanic remains intact. Minecraft performs the actual teleport; the
 * AutoPTU layer only validates canonical Trainer/source/destination authority.
 */
public final class FabricFastTravelRuntime {
    static final String OVERWORLD_SPAWN_DESTINATION = "overworld_spawn";
    private static final double MAX_DISTANCE_SQUARED = 25.0D;
    private static final CanonicalFastTravelService SERVICE = new CanonicalFastTravelService(MAX_DISTANCE_SQUARED);

    private FabricFastTravelRuntime() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient()
                    || hand != Hand.MAIN_HAND
                    || !(player instanceof ServerPlayerEntity serverPlayer)
                    || !serverPlayer.isSneaking()
                    || !serverPlayer.getStackInHand(hand).isEmpty()
                    || !isFastTravelPoint(world, hitResult.getBlockPos())) {
                return ActionResult.PASS;
            }

            BlockPos source = hitResult.getBlockPos();
            String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(serverPlayer.getUuid());
            boolean trainerExists = FabricCanonicalPlayerStoreRuntime.requireRepository(serverPlayer.getServer())
                    .findPlayer(playerId)
                    .isPresent();
            ServerWorld destinationWorld = serverPlayer.getServer().getOverworld();
            BlockPos destination = destinationWorld.getSpawnPos();

            CanonicalFastTravelService.Decision decision = SERVICE.canTravel(
                    new CanonicalFastTravelService.Request(
                            playerId,
                            trainerExists,
                            sourceId(world, source),
                            isFastTravelPoint(world, source),
                            serverPlayer.squaredDistanceTo(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D),
                            OVERWORLD_SPAWN_DESTINATION,
                            true,
                            true
                    )
            );
            if (!decision.allowed()) {
                serverPlayer.sendMessage(Text.literal("Fast travel denied: " + decision.reason()), true);
                return ActionResult.FAIL;
            }

            serverPlayer.teleport(
                    destinationWorld,
                    destination.getX() + 0.5D,
                    destination.getY() + 0.1D,
                    destination.getZ() + 0.5D,
                    serverPlayer.getYaw(),
                    serverPlayer.getPitch()
            );
            serverPlayer.sendMessage(Text.literal("Fast traveled using Minecraft's lodestone network."), false);
            return ActionResult.SUCCESS;
        });
    }

    static boolean isFastTravelPoint(World world, BlockPos pos) {
        return world.getBlockState(pos).isOf(Blocks.LODESTONE);
    }

    static String sourceId(World world, BlockPos pos) {
        return world.getRegistryKey().getValue() + ":" + pos.getX() + ":" + pos.getY() + ":" + pos.getZ();
    }
}
