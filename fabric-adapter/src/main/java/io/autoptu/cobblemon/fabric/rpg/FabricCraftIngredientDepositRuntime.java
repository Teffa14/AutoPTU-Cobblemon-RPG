package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalCraftIngredientDepositService;
import io.autoptu.cobblemon.authority.WorldTaskCatalogue;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

/**
 * Normal-world bridge from a server-observed Minecraft ingredient to canonical RPG inventory.
 *
 * <p>Sneak-use the authored crafting workstation while holding a supported ingredient. The server
 * reads the real held stack and transfers exactly one item per interaction. Client packets never
 * provide trusted item ids or quantities.</p>
 */
public final class FabricCraftIngredientDepositRuntime {
    private FabricCraftIngredientDepositRuntime() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient()
                    || hand != Hand.MAIN_HAND
                    || !(player instanceof ServerPlayerEntity serverPlayer)
                    || !serverPlayer.isSneaking()) {
                return ActionResult.PASS;
            }

            BlockPos stationHead = hitResult.getBlockPos();
            if (!FabricCraftingWorkstationRuntime.isCraftingWorkstation(world, stationHead)) {
                return ActionResult.PASS;
            }
            if (!FabricCraftingWorkstationRuntime.withinInteractionDistance(serverPlayer, stationHead)) {
                serverPlayer.sendMessage(Text.literal(
                        "You are too far away to deposit an AutoPTU crafting ingredient."), false);
                return ActionResult.FAIL;
            }

            String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(serverPlayer.getUuid());
            if (FabricCanonicalPlayerStoreRuntime.requireRepository(serverPlayer.getServer())
                    .findPlayer(playerId).isEmpty()) {
                serverPlayer.sendMessage(Text.literal(
                        "Canonical Trainer state is unavailable for this player."), false);
                return ActionResult.FAIL;
            }

            ItemStack held = serverPlayer.getStackInHand(Hand.MAIN_HAND);
            if (held.isEmpty()) {
                serverPlayer.sendMessage(Text.literal(
                        "Hold an authored crafting ingredient, then sneak-use the workstation."), false);
                return ActionResult.FAIL;
            }

            String templateId = Registries.ITEM.getId(held.getItem()).toString();
            CanonicalCraftIngredientDepositService depositService = new CanonicalCraftIngredientDepositService(
                    FabricCanonicalPlayerStoreRuntime.requireAssetRepository(serverPlayer.getServer()),
                    new WorldTaskCatalogue());
            if (!depositService.supports(templateId)) {
                serverPlayer.sendMessage(Text.literal(
                        templateId + " is not used by an authored AutoPTU crafting recipe."), false);
                return ActionResult.FAIL;
            }

            // Consume only server-observed inventory. Restore immediately if canonical persistence rejects the write.
            held.decrement(1);
            CanonicalCraftIngredientDepositService.DepositResult result =
                    depositService.deposit(playerId, templateId, 1);
            if (!result.applied()) {
                held.increment(1);
                serverPlayer.sendMessage(Text.literal(
                        "Canonical ingredient deposit failed: " + result.detail()), false);
                return ActionResult.FAIL;
            }

            serverPlayer.sendMessage(Text.literal(
                    "Deposited 1x " + templateId + " into canonical AutoPTU inventory. Canonical total: "
                            + result.canonicalQuantity() + "."), false);
            return ActionResult.SUCCESS;
        });
    }
}
