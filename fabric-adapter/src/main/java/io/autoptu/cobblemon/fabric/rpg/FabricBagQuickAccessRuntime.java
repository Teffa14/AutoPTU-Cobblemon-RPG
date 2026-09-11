package io.autoptu.cobblemon.fabric.rpg;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;

/**
 * Normal Minecraft access to the canonical bag without trusting Minecraft inventory contents.
 * Sneak + use with a vanilla Bundle is only an input gesture. The server ignores the Bundle's
 * contents and re-reads the authenticated Trainer's canonical bag before projecting any stacks.
 */
public final class FabricBagQuickAccessRuntime {
    private FabricBagQuickAccessRuntime() {}

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (hand != Hand.MAIN_HAND
                    || !player.isSneaking()
                    || !player.getStackInHand(hand).isOf(Items.BUNDLE)) {
                return TypedActionResult.pass(player.getStackInHand(hand));
            }
            if (world.isClient()) {
                return TypedActionResult.success(player.getStackInHand(hand));
            }
            if (!(player instanceof ServerPlayerEntity serverPlayer) || serverPlayer.getServer() == null) {
                return TypedActionResult.fail(player.getStackInHand(hand));
            }

            int shown = FabricBagRuntime.showPlayerBag(serverPlayer);
            return shown > 0
                    ? TypedActionResult.success(player.getStackInHand(hand))
                    : TypedActionResult.fail(player.getStackInHand(hand));
        });
    }
}
