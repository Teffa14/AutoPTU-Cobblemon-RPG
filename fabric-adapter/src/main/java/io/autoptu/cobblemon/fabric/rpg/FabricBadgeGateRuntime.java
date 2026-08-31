package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalBadgeGateService;
import io.autoptu.cobblemon.authority.CanonicalTrainerRecordQueryService;
import io.autoptu.cobblemon.authority.FileCanonicalTrainerRecordRepository;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.nio.file.Path;

/** Physical per-Trainer gate consuming only durable server-owned badge state. */
public final class FabricBadgeGateRuntime {
    static final double MAX_INTERACTION_DISTANCE_SQUARED = 25.0D;
    private static final double PASS_OFFSET = 1.55D;

    private FabricBadgeGateRuntime() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient() || hand != Hand.MAIN_HAND || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }
            BlockPos gatePos = hitResult.getBlockPos();
            if (!isBadgeGate(world, gatePos)) return ActionResult.PASS;
            if (serverPlayer.squaredDistanceTo(gatePos.getX() + 0.5D, gatePos.getY() + 0.5D, gatePos.getZ() + 0.5D)
                    > MAX_INTERACTION_DISTANCE_SQUARED) {
                serverPlayer.sendMessage(Text.literal("You are too far away to use this Ouros badge gate."), false);
                return ActionResult.FAIL;
            }

            String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(serverPlayer.getUuid());
            if (FabricCanonicalPlayerStoreRuntime.requireRepository(serverPlayer.getServer()).findPlayer(playerId).isEmpty()) {
                serverPlayer.sendMessage(Text.literal("Canonical Trainer state is not loaded."), false);
                return ActionResult.FAIL;
            }

            var decision = badgeGateService(serverPlayer).canPass(playerId, CanonicalBadgeGateService.CEDAR_LEAGUE_GATE_ID);
            if (!decision.allowed()) {
                serverPlayer.sendMessage(Text.literal("Cedar League Gate locked: " + decision.reason() + "."), true);
                return ActionResult.FAIL;
            }
            if (!passThrough(serverPlayer, gatePos)) {
                serverPlayer.sendMessage(Text.literal("Cedar League Gate cannot open a safe passage from this side."), true);
                return ActionResult.FAIL;
            }
            serverPlayer.sendMessage(Text.literal("Cedar League Gate recognized your canonical Cedar Gym Badge."), true);
            return ActionResult.SUCCESS;
        });
    }

    static boolean isBadgeGate(World world, BlockPos pos) {
        return world.getBlockState(pos).isOf(FabricRpgContent.CEDAR_LEAGUE_BADGE_GATE);
    }

    static CanonicalBadgeGateService badgeGateService(ServerPlayerEntity player) {
        return new CanonicalBadgeGateService(new CanonicalTrainerRecordQueryService(
                new FileCanonicalTrainerRecordRepository(canonicalStateRoot(player))));
    }

    static boolean passThrough(ServerPlayerEntity player, BlockPos gatePos) {
        ServerWorld world = player.getServerWorld();
        double centerX = gatePos.getX() + 0.5D;
        double centerZ = gatePos.getZ() + 0.5D;
        double dx = player.getX() - centerX;
        double dz = player.getZ() - centerZ;
        double targetX = centerX;
        double targetZ = centerZ;
        if (Math.abs(dx) >= Math.abs(dz)) {
            double sign = dx >= 0.0D ? 1.0D : -1.0D;
            targetX = centerX - sign * PASS_OFFSET;
        } else {
            double sign = dz >= 0.0D ? 1.0D : -1.0D;
            targetZ = centerZ - sign * PASS_OFFSET;
        }
        BlockPos feet = BlockPos.ofFloored(targetX, player.getY(), targetZ);
        if (!world.getBlockState(feet).getCollisionShape(world, feet).isEmpty()) return false;
        BlockPos head = feet.up();
        if (!world.getBlockState(head).getCollisionShape(world, head).isEmpty()) return false;
        player.teleport(world, targetX, player.getY(), targetZ, player.getYaw(), player.getPitch());
        return true;
    }

    private static Path canonicalStateRoot(ServerPlayerEntity player) {
        return player.getServer().getSavePath(WorldSavePath.ROOT)
                .resolve("autoptu")
                .resolve("canonical-state")
                .normalize();
    }
}
