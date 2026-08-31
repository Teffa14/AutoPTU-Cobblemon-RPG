package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalProgressionGateCatalogue;
import io.autoptu.cobblemon.authority.CanonicalProgressionGateService;
import io.autoptu.cobblemon.authority.CanonicalTrainerRecordQueryService;
import io.autoptu.cobblemon.authority.FileCanonicalTrainerRecordRepository;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.nio.file.Path;

/** Physical per-Trainer progression gate backed only by durable canonical RPG records. */
public final class FabricBadgeGateRuntime {
    private static final double MAX_DISTANCE_SQUARED = 25.0D;
    private static final double CROSS_DISTANCE = 1.75D;

    private FabricBadgeGateRuntime() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient()
                    || hand != Hand.MAIN_HAND
                    || !(player instanceof ServerPlayerEntity serverPlayer)
                    || !world.getBlockState(hitResult.getBlockPos()).isOf(FabricRpgContent.CEDAR_BADGE_GATE)) {
                return ActionResult.PASS;
            }
            return attemptPass(serverPlayer, hitResult.getBlockPos()) ? ActionResult.SUCCESS : ActionResult.FAIL;
        });
    }

    static boolean attemptPass(ServerPlayerEntity player, BlockPos gatePos) {
        if (player.squaredDistanceTo(gatePos.getX() + 0.5D, gatePos.getY() + 0.5D, gatePos.getZ() + 0.5D)
                > MAX_DISTANCE_SQUARED) {
            player.sendMessage(Text.literal("Cedar League Gate denied: too far from gate."), true);
            return false;
        }
        if (!player.getServerWorld().getBlockState(gatePos).isOf(FabricRpgContent.CEDAR_BADGE_GATE)) {
            player.sendMessage(Text.literal("Cedar League Gate denied: authored gate is no longer present."), true);
            return false;
        }

        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        if (FabricCanonicalPlayerStoreRuntime.requireRepository(player.getServer()).findPlayer(playerId).isEmpty()) {
            player.sendMessage(Text.literal("Cedar League Gate denied: canonical Trainer is unavailable."), true);
            return false;
        }

        var decision = service(player).canPass(playerId, CanonicalProgressionGateCatalogue.CEDAR_BADGE_GATE_ID);
        if (!decision.allowed()) {
            player.sendMessage(Text.literal(decision.gate().displayName() + " locked — requires "
                    + decision.gate().requiredBadgeId() + "."), false);
            return false;
        }

        double gateX = gatePos.getX() + 0.5D;
        double gateZ = gatePos.getZ() + 0.5D;
        double deltaX = gateX - player.getX();
        double deltaZ = gateZ - player.getZ();
        double destinationX = player.getX();
        double destinationZ = player.getZ();
        if (Math.abs(deltaX) >= Math.abs(deltaZ)) {
            destinationX = gateX + Math.copySign(CROSS_DISTANCE, deltaX);
            destinationZ = gateZ;
        } else {
            destinationX = gateX;
            destinationZ = gateZ + Math.copySign(CROSS_DISTANCE, deltaZ);
        }

        BlockPos feet = BlockPos.ofFloored(destinationX, player.getY(), destinationZ);
        World world = player.getServerWorld();
        if (!world.getBlockState(feet).getCollisionShape(world, feet).isEmpty()
                || !world.getBlockState(feet.up()).getCollisionShape(world, feet.up()).isEmpty()) {
            player.sendMessage(Text.literal("Cedar League Gate cannot open safely: passage is obstructed."), true);
            return false;
        }

        player.teleport(player.getServerWorld(), destinationX, player.getY(), destinationZ, player.getYaw(), player.getPitch());
        player.sendMessage(Text.literal(decision.gate().displayName() + " recognizes your canonical badge record."), false);
        return true;
    }

    private static CanonicalProgressionGateService service(ServerPlayerEntity player) {
        Path root = player.getServer().getSavePath(WorldSavePath.ROOT)
                .resolve("autoptu")
                .resolve("canonical-state")
                .normalize();
        return new CanonicalProgressionGateService(
                new CanonicalTrainerRecordQueryService(new FileCanonicalTrainerRecordRepository(root)));
    }
}
