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
 * AutoPTU canonical healing exposed through Cobblemon's Healing Machine block as presentation only.
 *
 * Cobblemon supplies the block/model/world placement. AutoPTU does not consult Cobblemon party HP,
 * battle state, healing eligibility, machine lifecycle, charge, results, or Pokemon state. The
 * authenticated server interaction is consumed before Cobblemon's native healing logic can become
 * authority, then the existing server-owned canonical healing service resolves and persists HP.
 */
public final class FabricHealingStationRuntime {
    private static final double MAX_INTERACTION_DISTANCE_SQUARED = 25.0D;

    private FabricHealingStationRuntime() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient() || hand != Hand.MAIN_HAND || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }

            BlockPos machinePos = hitResult.getBlockPos();
            if (!isCobblemonHealingMachine(world, machinePos)) {
                return ActionResult.PASS;
            }
            if (!withinInteractionDistance(serverPlayer, machinePos)) {
                serverPlayer.sendMessage(Text.literal("You are too far away to use this healing machine."), false);
                return ActionResult.FAIL;
            }

            healCanonicalParty(serverPlayer);

            // Consume the server interaction here. Cobblemon's block/model remains the world surface,
            // but its party, battle state and native healing outcome are never consulted or executed.
            return ActionResult.SUCCESS;
        });
    }

    static boolean isCobblemonHealingMachine(World world, BlockPos pos) {
        return world.getBlockState(pos).isOf(CobblemonBlocks.HEALING_MACHINE);
    }

    static boolean withinInteractionDistance(ServerPlayerEntity player, BlockPos machinePos) {
        double x = machinePos.getX() + 0.5D;
        double y = machinePos.getY() + 0.5D;
        double z = machinePos.getZ() + 0.5D;
        return player.squaredDistanceTo(x, y, z) <= MAX_INTERACTION_DISTANCE_SQUARED;
    }

    private static void healCanonicalParty(ServerPlayerEntity player) {
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        CanonicalPartyHealingService service = new CanonicalPartyHealingService(
                FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(player.getServer()),
                FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(player.getServer())
        );
        CanonicalPartyHealingDecision decision = service.healParty(playerId);
        sendFeedback(player, decision);
    }

    private static void sendFeedback(ServerPlayerEntity player, CanonicalPartyHealingDecision decision) {
        switch (decision.outcome()) {
            case APPLIED -> player.sendMessage(Text.literal(
                    "AutoPTU healing complete: " + decision.healedPokemon() + " healed, "
                            + decision.alreadyFullPokemon() + " already at full HP. "
                            + "Statuses and injuries were left unchanged."), false);
            case PARTIAL -> {
                player.sendMessage(Text.literal(
                        "AutoPTU healing partially completed: " + decision.healedPokemon() + " healed."), false);
                player.sendMessage(Text.literal(
                        "Canonical party members not healed safely: "
                                + String.join(", ", decision.failedPokemonIds())), false);
            }
            case NO_PARTY -> player.sendMessage(Text.literal(
                    "This Trainer has no persistent AutoPTU party to heal."), false);
            case INVALID_REQUEST -> player.sendMessage(Text.literal(
                    "AutoPTU rejected this canonical healing request: " + decision.reason()), false);
        }
    }
}
