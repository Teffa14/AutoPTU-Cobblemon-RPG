package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.ActionKind;
import io.autoptu.cobblemon.authority.CanonicalPlayerActionService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;

/**
 * Normal Minecraft access to the canonical party screen without a slash command or dev fixture.
 * Sneak + use with an empty main hand is only an input gesture. The server re-resolves canonical
 * Trainer and party state before opening the existing server-authored management screen.
 */
public final class FabricPartyQuickAccessRuntime {
    static final String CONTEXT_ID = "party_quick_access";
    private static final CanonicalPlayerActionService ACTIONS = new CanonicalPlayerActionService();

    private FabricPartyQuickAccessRuntime() {}

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (hand != Hand.MAIN_HAND || !player.isSneaking() || !player.getStackInHand(hand).isEmpty()) {
                return TypedActionResult.pass(player.getStackInHand(hand));
            }
            if (world.isClient()) {
                return TypedActionResult.success(player.getStackInHand(hand));
            }
            if (!(player instanceof ServerPlayerEntity serverPlayer) || serverPlayer.getServer() == null) {
                return TypedActionResult.fail(player.getStackInHand(hand));
            }

            String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(serverPlayer.getUuid());
            boolean trainerExists = FabricCanonicalPlayerStoreRuntime.requireRepository(serverPlayer.getServer())
                    .findPlayer(playerId)
                    .isPresent();
            CanonicalPlayerActionService.Decision decision = ACTIONS.canPerform(
                    new CanonicalPlayerActionService.Request(
                            playerId,
                            trainerExists,
                            ActionKind.INTERACT,
                            CONTEXT_ID,
                            true
                    )
            );
            if (!decision.allowed()) {
                serverPlayer.sendMessage(Text.literal("Ouros action unavailable: " + decision.reason()), true);
                return TypedActionResult.fail(player.getStackInHand(hand));
            }
            if (FabricPartyManagementRuntime.queryParty(serverPlayer) == null) {
                serverPlayer.sendMessage(Text.literal("Choose a starter before opening your Ouros party."), true);
                return TypedActionResult.success(player.getStackInHand(hand));
            }

            FabricPartyManagementRuntime.openScreen(serverPlayer);
            return TypedActionResult.success(player.getStackInHand(hand));
        });
    }
}
