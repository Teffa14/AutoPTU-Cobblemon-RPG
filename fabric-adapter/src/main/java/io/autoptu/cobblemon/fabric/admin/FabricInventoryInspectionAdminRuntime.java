package io.autoptu.cobblemon.fabric.admin;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.authority.CanonicalBagQueryService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Operator-only, read-only inspection of the persistent canonical inventory aggregate. */
public final class FabricInventoryInspectionAdminRuntime {
    private FabricInventoryInspectionAdminRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("admin")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.literal("inventory")
                                        .then(CommandManager.literal("inspect")
                                                .then(CommandManager.argument("player", StringArgumentType.word())
                                                        .executes(context -> inspect(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "player")))))))));
    }

    private static int inspect(ServerCommandSource source, String playerName) {
        ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(playerName);
        if (target == null) {
            source.sendError(Text.literal("That Minecraft player must be online for canonical identity resolution."));
            return 0;
        }

        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(target.getUuid());
        var playerRepository = FabricCanonicalPlayerStoreRuntime.requireRepository(source.getServer());
        if (playerRepository.findPlayer(playerId).isEmpty()) {
            source.sendError(Text.literal("No canonical AutoPTU Trainer state exists for " + target.getGameProfile().getName() + "."));
            return 0;
        }

        CanonicalBagQueryService.BagSnapshot bag;
        try {
            bag = new CanonicalBagQueryService(
                    FabricCanonicalPlayerStoreRuntime.requireAssetRepository(source.getServer()))
                    .inspect(playerId);
        } catch (RuntimeException inconsistentInventory) {
            source.sendError(Text.literal("Canonical inventory state is inconsistent and cannot be inspected safely: "
                    + safeMessage(inconsistentInventory)));
            return 0;
        }

        source.sendFeedback(() -> Text.literal("AutoPTU inventory inspection — " + target.getGameProfile().getName()), false);
        source.sendFeedback(() -> Text.literal("Canonical player: " + playerId + " | UUID " + target.getUuidAsString()), false);
        source.sendFeedback(() -> Text.literal("Inventory: " + bag.entries().size() + " stack(s)"
                + " | quantity " + bag.totalQuantity()
                + " | available " + bag.totalAvailable()
                + " | reserved " + bag.totalReserved()
                + " | locks " + bag.transactionLocks()), false);

        if (bag.entries().isEmpty()) {
            source.sendFeedback(() -> Text.literal("  empty"), false);
        } else {
            for (CanonicalBagQueryService.BagEntry entry : bag.entries()) {
                String reservation = entry.reservationId() == null || entry.reservationId().isBlank()
                        ? "none"
                        : entry.reservationId();
                source.sendFeedback(() -> Text.literal("  " + entry.templateId()
                        + " | instance " + entry.itemInstanceId()
                        + " | qty " + entry.quantity()
                        + " | available " + entry.availableQuantity()
                        + " | reserved " + entry.reservedQuantity()
                        + " | reservation " + reservation
                        + " | consumed " + entry.reservationConsumed()
                        + " | revision " + entry.revision()), false);
            }
        }

        source.sendFeedback(() -> Text.literal(
                "Read-only canonical inventory inspection complete; Cobblemon gameplay data and PTU item legality/effects were not consulted or mutated."), false);
        return 1;
    }

    private static String safeMessage(RuntimeException error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }
}
