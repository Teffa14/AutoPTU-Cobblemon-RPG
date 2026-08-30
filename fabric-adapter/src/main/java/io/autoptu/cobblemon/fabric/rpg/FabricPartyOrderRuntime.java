package io.autoptu.cobblemon.fabric.rpg;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.autoptu.cobblemon.authority.CanonicalPartyOrderService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Fabric fallback surface for persistent server-authoritative party ordering. */
public final class FabricPartyOrderRuntime {
    private FabricPartyOrderRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("party")
                                .then(CommandManager.literal("move")
                                        .then(CommandManager.argument("from", IntegerArgumentType.integer(1, 6))
                                                .then(CommandManager.argument("to", IntegerArgumentType.integer(1, 6))
                                                        .executes(context -> move(
                                                                context.getSource(),
                                                                IntegerArgumentType.getInteger(context, "from"),
                                                                IntegerArgumentType.getInteger(context, "to")
                                                        ))))))));
    }

    private static int move(ServerCommandSource source, int fromSlot, int toSlot) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null || player.getServer() == null) {
            source.sendError(Text.literal("Party order must be changed by an authenticated player."));
            return 0;
        }

        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        CanonicalPartyOrderService service = new CanonicalPartyOrderService(
                FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(player.getServer())
        );
        CanonicalPartyOrderService.Decision decision = service.move(playerId, fromSlot, toSlot);
        switch (decision.outcome()) {
            case APPLIED -> {
                source.sendFeedback(() -> Text.literal(
                        "Moved canonical party slot " + fromSlot + " to " + toSlot + "."), false);
                return 1;
            }
            case ALREADY_ORDERED -> {
                source.sendFeedback(() -> Text.literal("That Pokemon is already in party slot " + toSlot + "."), false);
                return 1;
            }
            case INVALID_SLOT, NO_PARTY, CONCURRENT_WRITE -> {
                source.sendError(Text.literal("Party reorder rejected: " + decision.reason()));
                return 0;
            }
        }
        return 0;
    }
}
