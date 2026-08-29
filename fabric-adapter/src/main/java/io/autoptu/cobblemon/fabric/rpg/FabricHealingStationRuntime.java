package io.autoptu.cobblemon.fabric.rpg;

import com.cobblemon.mod.common.CobblemonBlocks;
import io.autoptu.cobblemon.authority.CanonicalPartyHealingDecision;
import io.autoptu.cobblemon.authority.CanonicalPartyHealingService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Normal-world healing interaction backed by the same canonical service as /autoptu healparty.
 *
 * The physical surface is Cobblemon's real Healing Machine block. Cobblemon owns the model,
 * animation/block state, crafting and ordinary Minecraft presentation; AutoPTU intercepts the
 * interaction only to keep persistent party HP server-authoritative. Cobblemon party/HP/BattleState
 * is never read as RPG truth.
 */
public final class FabricHealingStationRuntime {
    private static final double MAX_INTERACTION_DISTANCE_SQUARED = 25.0D;

    private FabricHealingStationRuntime() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient() || hand != Hand.MAIN_HAND || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }

            BlockPos stationHead = hitResult.getBlockPos();
            if (!isHealingStation(world, stationHead)) {
                return ActionResult.PASS;
            }
            if (!withinInteractionDistance(serverPlayer, stationHead)) {
                serverPlayer.sendMessage(Text.literal("You are too far away to use this healing machine."), false);
                return ActionResult.FAIL;
            }

            String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(serverPlayer.getUuid());
            CanonicalPartyHealingService service = new CanonicalPartyHealingService(
                    FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(serverPlayer.getServer()),
                    FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(serverPlayer.getServer())
            );
            CanonicalPartyHealingDecision decision = service.healParty(playerId);
            sendFeedback(serverPlayer, decision);
            // Consume the use so Cobblemon's native party store does not also become gameplay authority.
            return decision.changedState() || decision.outcome() == CanonicalPartyHealingDecision.Outcome.APPLIED
                    ? ActionResult.SUCCESS
                    : ActionResult.FAIL;
        });
    }

    static boolean isHealingStation(World world, BlockPos head) {
        return world.getBlockState(head).isOf(CobblemonBlocks.HEALING_MACHINE);
    }

    static boolean withinInteractionDistance(ServerPlayerEntity player, BlockPos stationHead) {
        double x = stationHead.getX() + 0.5D;
        double y = stationHead.getY() + 0.5D;
        double z = stationHead.getZ() + 0.5D;
        return player.squaredDistanceTo(x, y, z) <= MAX_INTERACTION_DISTANCE_SQUARED;
    }

    private static void sendFeedback(ServerPlayerEntity player, CanonicalPartyHealingDecision decision) {
        switch (decision.outcome()) {
            case APPLIED -> player.sendMessage(Text.literal(
                    "Healing Machine complete: " + decision.healedPokemon() + " healed, "
                            + decision.alreadyFullPokemon() + " already at full HP. "
                            + "Statuses and injuries were left unchanged."), false);
            case PARTIAL -> {
                player.sendMessage(Text.literal(
                        "Healing Machine partially completed: " + decision.healedPokemon() + " healed."), false);
                player.sendMessage(Text.literal(
                        "Canonical party members not healed safely: "
                                + String.join(", ", decision.failedPokemonIds())), false);
            }
            case NO_PARTY -> player.sendMessage(Text.literal(
                    "This Trainer has no persistent AutoPTU party to heal."), false);
            case INVALID_REQUEST -> player.sendMessage(Text.literal(
                    "The Healing Machine rejected this canonical request: " + decision.reason()), false);
        }
    }
}
