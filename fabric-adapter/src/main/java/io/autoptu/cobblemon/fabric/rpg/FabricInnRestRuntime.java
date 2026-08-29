package io.autoptu.cobblemon.fabric.rpg;

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
 * Ouros/PTU recovery bed backed by a distinct namespaced mod block.
 *
 * The physical facility is never inferred from an arrangement of vanilla/Cobblemon blocks. The
 * datapack recipe may combine a vanilla bed with a Cobblemon Healing Machine, but world identity is
 * always FabricRpgContent.PTU_RECOVERY_BED. Party HP recovery delegates to canonical server state.
 */
public final class FabricInnRestRuntime {
    private static final double MAX_INTERACTION_DISTANCE_SQUARED = 25.0D;

    private FabricInnRestRuntime() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient()
                    || hand != Hand.MAIN_HAND
                    || !(player instanceof ServerPlayerEntity serverPlayer)
                    || !serverPlayer.getStackInHand(hand).isEmpty()) {
                return ActionResult.PASS;
            }

            BlockPos bedPos = hitResult.getBlockPos();
            if (!isInnRestPoint(world, bedPos)) return ActionResult.PASS;
            if (!withinInteractionDistance(serverPlayer, bedPos)) {
                serverPlayer.sendMessage(Text.literal("You are too far away to use the PTU recovery bed."), false);
                return ActionResult.FAIL;
            }

            float oldPlayerHealth = serverPlayer.getHealth();
            serverPlayer.setHealth(serverPlayer.getMaxHealth());
            boolean playerHealed = serverPlayer.getHealth() > oldPlayerHealth;

            String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(serverPlayer.getUuid());
            CanonicalPartyHealingService service = new CanonicalPartyHealingService(
                    FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(serverPlayer.getServer()),
                    FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(serverPlayer.getServer())
            );
            CanonicalPartyHealingDecision decision = service.healParty(playerId);
            sendFeedback(serverPlayer, decision, playerHealed);
            return playerHealed
                            || decision.changedState()
                            || decision.outcome() == CanonicalPartyHealingDecision.Outcome.APPLIED
                    ? ActionResult.SUCCESS
                    : ActionResult.FAIL;
        });
    }

    static boolean isInnRestPoint(World world, BlockPos bedPos) {
        return world.getBlockState(bedPos).isOf(FabricRpgContent.PTU_RECOVERY_BED);
    }

    static boolean withinInteractionDistance(ServerPlayerEntity player, BlockPos bedPos) {
        return player.squaredDistanceTo(
                bedPos.getX() + 0.5D,
                bedPos.getY() + 0.5D,
                bedPos.getZ() + 0.5D
        ) <= MAX_INTERACTION_DISTANCE_SQUARED;
    }

    private static void sendFeedback(
            ServerPlayerEntity player,
            CanonicalPartyHealingDecision decision,
            boolean playerHealed
    ) {
        String playerSummary = playerHealed ? " Your Minecraft health was restored." : "";
        switch (decision.outcome()) {
            case APPLIED -> player.sendMessage(Text.literal(
                    "PTU recovery complete: " + decision.healedPokemon() + " Pokemon healed, "
                            + decision.alreadyFullPokemon() + " already at full HP."
                            + playerSummary
                            + " Canonical statuses and injuries remain unchanged until their authoritative recovery contract exists."), false);
            case PARTIAL -> {
                player.sendMessage(Text.literal(
                        "PTU recovery partially completed: " + decision.healedPokemon() + " Pokemon healed."
                                + playerSummary), false);
                player.sendMessage(Text.literal(
                        "Canonical party members not restored safely: "
                                + String.join(", ", decision.failedPokemonIds())), false);
            }
            case NO_PARTY -> player.sendMessage(Text.literal(
                    "Your Minecraft health was checked, but this Trainer has no persistent AutoPTU party to recover."), false);
            case INVALID_REQUEST -> player.sendMessage(Text.literal(
                    "The PTU recovery bed rejected the canonical party request: " + decision.reason()
                            + playerSummary), false);
        }
    }
}
