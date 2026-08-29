package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalPartyHealingDecision;
import io.autoptu.cobblemon.authority.CanonicalPartyHealingService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Authored Ouros inn/rest point using Minecraft's native bed as the physical surface.
 *
 * Ordinary beds remain vanilla. A gold block directly below either half of a bed opts the whole bed
 * into the Ouros inn interaction. Sneak-use with an empty main hand invokes the same
 * server-authoritative persistent party healing service as the Healing Machine; no Cobblemon
 * party/HP/BattleState data is read and no PTU recovery rule is invented in Minecraft.
 */
public final class FabricInnRestRuntime {
    private static final double MAX_INTERACTION_DISTANCE_SQUARED = 25.0D;

    private FabricInnRestRuntime() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient()
                    || hand != Hand.MAIN_HAND
                    || !(player instanceof ServerPlayerEntity serverPlayer)
                    || !serverPlayer.isSneaking()
                    || !serverPlayer.getStackInHand(hand).isEmpty()) {
                return ActionResult.PASS;
            }

            BlockPos bedPos = hitResult.getBlockPos();
            if (!isInnRestPoint(world, bedPos)) return ActionResult.PASS;
            if (!withinInteractionDistance(serverPlayer, bedPos)) {
                serverPlayer.sendMessage(Text.literal("You are too far away to rest at this inn."), false);
                return ActionResult.FAIL;
            }

            String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(serverPlayer.getUuid());
            CanonicalPartyHealingService service = new CanonicalPartyHealingService(
                    FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(serverPlayer.getServer()),
                    FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(serverPlayer.getServer())
            );
            CanonicalPartyHealingDecision decision = service.healParty(playerId);
            sendFeedback(serverPlayer, decision);
            return decision.changedState() || decision.outcome() == CanonicalPartyHealingDecision.Outcome.APPLIED
                    ? ActionResult.SUCCESS
                    : ActionResult.FAIL;
        });
    }

    static boolean isInnRestPoint(World world, BlockPos bedPos) {
        BlockState state = world.getBlockState(bedPos);
        if (!(state.getBlock() instanceof BedBlock)) return false;
        if (world.getBlockState(bedPos.down()).isOf(Blocks.GOLD_BLOCK)) return true;

        Direction facing = state.get(BedBlock.FACING);
        BedPart part = state.get(BedBlock.PART);
        BlockPos otherHalf = bedPos.offset(part == BedPart.FOOT ? facing : facing.getOpposite());
        BlockState otherState = world.getBlockState(otherHalf);
        if (!(otherState.getBlock() instanceof BedBlock)
                || otherState.get(BedBlock.FACING) != facing
                || otherState.get(BedBlock.PART) == part) {
            return false;
        }
        return world.getBlockState(otherHalf.down()).isOf(Blocks.GOLD_BLOCK);
    }

    static boolean withinInteractionDistance(ServerPlayerEntity player, BlockPos bedPos) {
        return player.squaredDistanceTo(
                bedPos.getX() + 0.5D,
                bedPos.getY() + 0.5D,
                bedPos.getZ() + 0.5D
        ) <= MAX_INTERACTION_DISTANCE_SQUARED;
    }

    private static void sendFeedback(ServerPlayerEntity player, CanonicalPartyHealingDecision decision) {
        switch (decision.outcome()) {
            case APPLIED -> player.sendMessage(Text.literal(
                    "Inn rest complete: " + decision.healedPokemon() + " healed, "
                            + decision.alreadyFullPokemon() + " already at full HP. "
                            + "Statuses and injuries were left unchanged."), false);
            case PARTIAL -> {
                player.sendMessage(Text.literal(
                        "Inn rest partially completed: " + decision.healedPokemon() + " healed."), false);
                player.sendMessage(Text.literal(
                        "Canonical party members not restored safely: "
                                + String.join(", ", decision.failedPokemonIds())), false);
            }
            case NO_PARTY -> player.sendMessage(Text.literal(
                    "This Trainer has no persistent AutoPTU party to rest."), false);
            case INVALID_REQUEST -> player.sendMessage(Text.literal(
                    "The inn rejected this canonical rest request: " + decision.reason()), false);
        }
    }
}
