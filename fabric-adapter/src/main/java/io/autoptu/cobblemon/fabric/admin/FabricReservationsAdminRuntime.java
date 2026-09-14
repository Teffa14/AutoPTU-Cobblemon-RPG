package io.autoptu.cobblemon.fabric.admin;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.fabric.rpg.FabricReservationInspectionService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Operator-only command surface for read-only canonical reservation inspection. */
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

        FabricReservationInspectionService.InspectionResult result =
                FabricReservationInspectionService.inspect(source.getServer(), target);
        if (!result.success()) {
            source.sendError(Text.literal("Canonical reservation inspection failed safely ["
                    + result.errorCode()
                    + "]; persistent identifiers and repository details remain redacted."));
            return 0;
        }

        emit(source, result);
        return 1;
    }

    private static void emit(
            ServerCommandSource source,
            FabricReservationInspectionService.InspectionResult result
    ) {
        source.sendFeedback(() -> Text.literal("AutoPTU canonical reservations — " + result.playerName()), false);
        source.sendFeedback(() -> Text.literal(
                "Identity: authenticated online player; persistent player/item/reservation/transaction IDs redacted."), false);

        if (result.reservations().isEmpty()) {
            source.sendFeedback(() -> Text.literal("Reservations: none"), false);
        } else {
            source.sendFeedback(() -> Text.literal("Reservations: " + result.reservations().size()
                    + " active lock(s) | locked stacks " + result.transactionLocks()
                    + " | reserved quantity " + result.totalReserved()), false);
            int index = 1;
            for (FabricReservationInspectionService.ReservationView reservation : result.reservations()) {
                int outputIndex = index++;
                source.sendFeedback(() -> Text.literal("  reservation " + outputIndex
                        + " | template " + reservation.templateId()
                        + " | reserved " + reservation.reservedQuantity()
                        + " | stack qty " + reservation.stackQuantity()
                        + " | available " + reservation.availableQuantity()
                        + " | consumed " + reservation.consumed()
                        + " | item revision " + reservation.itemRevision()
                        + " | IDs redacted"), false);
            }
        }

        source.sendFeedback(() -> Text.literal(
                "Read-only reservation inspection complete; no inventory, reservation, battle or PTU state was mutated."), false);
    }
}
