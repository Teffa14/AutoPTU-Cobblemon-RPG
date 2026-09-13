package io.autoptu.cobblemon.fabric.admin;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.authority.CanonicalBagQueryService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

/** Operator-only read-only inspection of server-owned canonical item reservations. */
public final class FabricReservationsAdminRuntime implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("admin")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.literal("reservations")
                                        .then(CommandManager.argument("player", StringArgumentType.word())
                                                .executes(context -> inspect(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "player"))))))));
    }

    private static int inspect(ServerCommandSource source, String playerName) {
        ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(playerName);
        if (target == null) {
            source.sendError(Text.literal("That Minecraft player must be online for canonical identity resolution."));
            return 0;
        }

        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(target.getUuid());
        try {
            var playerRepository = FabricCanonicalPlayerStoreRuntime.requireRepository(source.getServer());
            if (playerRepository.findPlayer(playerId).isEmpty()) {
                source.sendError(Text.literal("No canonical AutoPTU Trainer state exists for "
                        + target.getGameProfile().getName() + "."));
                return 0;
            }

            CanonicalBagQueryService.BagSnapshot bag = new CanonicalBagQueryService(
                    FabricCanonicalPlayerStoreRuntime.requireAssetRepository(source.getServer()))
                    .inspect(playerId);
            List<CanonicalBagQueryService.BagEntry> reservations = bag.entries().stream()
                    .filter(CanonicalBagQueryService.BagEntry::transactionLocked)
                    .toList();

            emit(source, target.getGameProfile().getName(), reservations, bag.totalReserved());
            return 1;
        } catch (RuntimeException inconsistentState) {
            source.sendError(Text.literal(
                    "Canonical reservation inspection failed safely while reading server-owned state; details were redacted."));
            return 0;
        }
    }

    private static void emit(
            ServerCommandSource source,
            String playerName,
            List<CanonicalBagQueryService.BagEntry> reservations,
            int totalReserved
    ) {
        source.sendFeedback(() -> Text.literal("AutoPTU canonical reservations — " + playerName), false);
        source.sendFeedback(() -> Text.literal("Identity: authenticated online player; persistent identifiers redacted."), false);

        if (reservations.isEmpty()) {
            source.sendFeedback(() -> Text.literal("Reservations: none"), false);
        } else {
            source.sendFeedback(() -> Text.literal("Reservations: " + reservations.size()
                    + " active lock(s) | reserved quantity " + totalReserved), false);
            int index = 1;
            for (CanonicalBagQueryService.BagEntry entry : reservations) {
                int outputIndex = index++;
                source.sendFeedback(() -> Text.literal("  reservation " + outputIndex
                        + " | template " + entry.templateId()
                        + " | reserved " + entry.reservedQuantity()
                        + " | stack qty " + entry.quantity()
                        + " | available " + entry.availableQuantity()
                        + " | consumed " + entry.reservationConsumed()
                        + " | item revision " + entry.revision()
                        + " | reservation/item IDs redacted"), false);
            }
        }

        source.sendFeedback(() -> Text.literal(
                "Read-only reservation inspection complete; no inventory, reservation, battle or PTU state was mutated."), false);
    }
}
