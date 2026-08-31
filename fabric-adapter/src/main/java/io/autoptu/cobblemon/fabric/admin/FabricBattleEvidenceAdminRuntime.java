package io.autoptu.cobblemon.fabric.admin;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.fabric.presentation.FabricSemanticBattleTrace;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/** Operator-only Minecraft surface for inspecting semantic AutoPTU battle evidence. */
public final class FabricBattleEvidenceAdminRuntime implements ModInitializer {
    private static final int MAX_VISIBLE_EVENTS = 20;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("admin")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.literal("evidence")
                                        .then(CommandManager.literal("battle")
                                                .then(CommandManager.argument("reservationId", StringArgumentType.word())
                                                        .executes(context -> showBattleEvidence(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "reservationId")))))))));
    }

    private static int showBattleEvidence(ServerCommandSource source, String reservationId) {
        var events = FabricSemanticBattleTrace.snapshot(reservationId);
        if (events.isEmpty()) {
            source.sendError(Text.literal("No active authoritative semantic trace exists for that reservation."));
            return 0;
        }

        int start = Math.max(0, events.size() - MAX_VISIBLE_EVENTS);
        source.sendFeedback(() -> Text.literal(
                "AutoPTU semantic battle evidence: showing " + (events.size() - start)
                        + " of " + events.size() + " authoritative events"), false);
        for (int index = start; index < events.size(); index++) {
            var event = events.get(index);
            source.sendFeedback(() -> Text.literal(
                    event.sequence() + " | " + event.kind() + " | " + event.stableKey()), false);
        }
        return 1;
    }
}
