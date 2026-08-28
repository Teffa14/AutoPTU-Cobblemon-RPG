package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalCraftIngredientDepositService;
import io.autoptu.cobblemon.authority.CraftIngredientDepositHandoff;
import io.autoptu.cobblemon.authority.FileCraftIngredientDepositHandoffRepository;
import io.autoptu.cobblemon.authority.WorldTaskCatalogue;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * Normal-world bridge from a server-observed Minecraft ingredient to canonical RPG inventory.
 *
 * <p>Sneak-use the authored crafting workstation while holding a supported ingredient. The server
 * reads the real held stack and transfers exactly one item per interaction. A durable handoff is
 * written before the Minecraft stack changes, the changed player inventory is explicitly saved,
 * and the canonical deposit is keyed by that same handoff. Reconnect recovery can therefore finish
 * a persisted withdrawal without duplicating an already-applied canonical item.</p>
 */
public final class FabricCraftIngredientDepositRuntime {
    private FabricCraftIngredientDepositRuntime() {}

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                recoverPending(server, handler.getPlayer()));

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
            CanonicalCraftIngredientDepositService depositService = depositService(serverPlayer.getServer());
            if (!depositService.supports(templateId)) {
                serverPlayer.sendMessage(Text.literal(
                        templateId + " is not used by an authored AutoPTU crafting recipe."), false);
                return ActionResult.FAIL;
            }

            FileCraftIngredientDepositHandoffRepository journal =
                    FabricCanonicalPlayerStoreRuntime.requireCraftDepositHandoffRepository(serverPlayer.getServer());
            String handoffId = "minecraft-craft-deposit:" + UUID.randomUUID();
            CraftIngredientDepositHandoff prepared = new CraftIngredientDepositHandoff(
                    handoffId,
                    playerId,
                    templateId,
                    serverPlayer.getInventory().selectedSlot,
                    held.getCount(),
                    1,
                    CraftIngredientDepositHandoff.Phase.PREPARED);
            if (!journal.createIfAbsent(prepared)) {
                serverPlayer.sendMessage(Text.literal("Could not open a durable ingredient transfer."), false);
                return ActionResult.FAIL;
            }

            // The durable PREPARED record exists before Minecraft inventory truth changes.
            held.decrement(1);
            serverPlayer.getInventory().markDirty();
            // PlayerManager exposes per-player save only as protected in Yarn 1.21.1. Saving all
            // player data is the public server-owned durability boundary available to the adapter.
            serverPlayer.getServer().getPlayerManager().saveAllPlayerData();

            CraftIngredientDepositHandoff withdrawn = prepared.withPhase(CraftIngredientDepositHandoff.Phase.WITHDRAWN);
            if (!journal.replaceIfPhase(
                    handoffId, CraftIngredientDepositHandoff.Phase.PREPARED, withdrawn)) {
                // Do not restore the stack here. Its persisted state is now journal-recoverable.
                recoverPending(serverPlayer.getServer(), serverPlayer);
                serverPlayer.sendMessage(Text.literal(
                        "Ingredient transfer was journaled and will finish from server recovery."), false);
                return ActionResult.SUCCESS;
            }

            CanonicalCraftIngredientDepositService.DepositResult result =
                    applyWithdrawn(journal, depositService, withdrawn);
            if (!result.applied()) {
                serverPlayer.sendMessage(Text.literal(
                        "Ingredient was safely withdrawn but canonical deposit is pending recovery: " + result.detail()), false);
                return ActionResult.SUCCESS;
            }

            serverPlayer.sendMessage(Text.literal(
                    "Deposited 1x " + templateId + " into canonical AutoPTU inventory. Available canonical total: "
                            + result.canonicalQuantity() + "."), false);
            return ActionResult.SUCCESS;
        });
    }

    static void recoverPending(MinecraftServer server, ServerPlayerEntity player) {
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        FileCraftIngredientDepositHandoffRepository journal =
                FabricCanonicalPlayerStoreRuntime.requireCraftDepositHandoffRepository(server);
        CanonicalCraftIngredientDepositService depositService = depositService(server);
        int recovered = 0;
        int unresolved = 0;

        for (CraftIngredientDepositHandoff pending : journal.findPendingForPlayer(playerId)) {
            CraftIngredientDepositHandoff current = journal.find(pending.handoffId()).orElse(pending);
            if (current.phase() == CraftIngredientDepositHandoff.Phase.CANONICAL_APPLIED) {
                if (journal.replaceIfPhase(
                        current.handoffId(), current.phase(),
                        current.withPhase(CraftIngredientDepositHandoff.Phase.COMMITTED))) {
                    recovered++;
                }
                continue;
            }

            if (current.phase() == CraftIngredientDepositHandoff.Phase.PREPARED) {
                if (depositService.isHandoffApplied(
                        current.handoffId(), current.playerId(), current.itemTemplateId(), current.quantity())) {
                    CraftIngredientDepositHandoff applied =
                            current.withPhase(CraftIngredientDepositHandoff.Phase.CANONICAL_APPLIED);
                    if (journal.replaceIfPhase(current.handoffId(), current.phase(), applied)) {
                        journal.replaceIfPhase(
                                applied.handoffId(), applied.phase(),
                                applied.withPhase(CraftIngredientDepositHandoff.Phase.COMMITTED));
                        recovered++;
                    }
                    continue;
                }

                PreparedState observed = observePreparedInventory(player, current);
                if (observed == PreparedState.NOT_WITHDRAWN) {
                    journal.replaceIfPhase(
                            current.handoffId(), current.phase(),
                            current.withPhase(CraftIngredientDepositHandoff.Phase.ABORTED));
                    continue;
                }
                if (observed == PreparedState.AMBIGUOUS) {
                    unresolved++;
                    continue;
                }

                CraftIngredientDepositHandoff withdrawn =
                        current.withPhase(CraftIngredientDepositHandoff.Phase.WITHDRAWN);
                if (!journal.replaceIfPhase(current.handoffId(), current.phase(), withdrawn)) {
                    unresolved++;
                    continue;
                }
                current = withdrawn;
            }

            if (current.phase() == CraftIngredientDepositHandoff.Phase.WITHDRAWN) {
                CanonicalCraftIngredientDepositService.DepositResult result =
                        applyWithdrawn(journal, depositService, current);
                if (result.applied()) recovered++;
                else unresolved++;
            }
        }

        if (recovered > 0) {
            player.sendMessage(Text.literal(
                    "Recovered " + recovered + " durable AutoPTU ingredient transfer(s)."), false);
        }
        if (unresolved > 0) {
            player.sendMessage(Text.literal(
                    unresolved + " AutoPTU ingredient transfer(s) remain safely journaled for operator recovery."), false);
        }
    }

    private static CanonicalCraftIngredientDepositService.DepositResult applyWithdrawn(
            FileCraftIngredientDepositHandoffRepository journal,
            CanonicalCraftIngredientDepositService depositService,
            CraftIngredientDepositHandoff withdrawn
    ) {
        CanonicalCraftIngredientDepositService.DepositResult result = depositService.depositHandoff(
                withdrawn.handoffId(), withdrawn.playerId(), withdrawn.itemTemplateId(), withdrawn.quantity());
        if (!result.applied()) return result;

        CraftIngredientDepositHandoff applied =
                withdrawn.withPhase(CraftIngredientDepositHandoff.Phase.CANONICAL_APPLIED);
        if (journal.replaceIfPhase(withdrawn.handoffId(), withdrawn.phase(), applied)) {
            journal.replaceIfPhase(
                    applied.handoffId(), applied.phase(),
                    applied.withPhase(CraftIngredientDepositHandoff.Phase.COMMITTED));
        } else {
            CraftIngredientDepositHandoff current = journal.find(withdrawn.handoffId()).orElse(withdrawn);
            if (current.phase() == CraftIngredientDepositHandoff.Phase.CANONICAL_APPLIED) {
                journal.replaceIfPhase(
                        current.handoffId(), current.phase(),
                        current.withPhase(CraftIngredientDepositHandoff.Phase.COMMITTED));
            }
        }
        return result;
    }

    private static PreparedState observePreparedInventory(
            ServerPlayerEntity player,
            CraftIngredientDepositHandoff handoff
    ) {
        if (handoff.inventorySlot() >= player.getInventory().size()) return PreparedState.AMBIGUOUS;
        ItemStack observed = player.getInventory().getStack(handoff.inventorySlot());
        int withdrawnCount = handoff.beforeCount() - handoff.quantity();
        if (withdrawnCount == 0 && observed.isEmpty()) return PreparedState.WITHDRAWN;
        if (observed.isEmpty()) return PreparedState.AMBIGUOUS;
        String observedTemplate = Registries.ITEM.getId(observed.getItem()).toString();
        if (!observedTemplate.equals(handoff.itemTemplateId())) return PreparedState.AMBIGUOUS;
        if (observed.getCount() == handoff.beforeCount()) return PreparedState.NOT_WITHDRAWN;
        if (observed.getCount() == withdrawnCount) return PreparedState.WITHDRAWN;
        return PreparedState.AMBIGUOUS;
    }

    private static CanonicalCraftIngredientDepositService depositService(MinecraftServer server) {
        return new CanonicalCraftIngredientDepositService(
                FabricCanonicalPlayerStoreRuntime.requireAssetRepository(server),
                new WorldTaskCatalogue());
    }

    private enum PreparedState {
        NOT_WITHDRAWN,
        WITHDRAWN,
        AMBIGUOUS
    }
}
