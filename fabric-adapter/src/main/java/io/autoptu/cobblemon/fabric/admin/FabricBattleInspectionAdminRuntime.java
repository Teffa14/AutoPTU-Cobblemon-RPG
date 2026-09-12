package io.autoptu.cobblemon.fabric.admin;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.fabric.battle.FabricBattleChoiceRuntime;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Operator-only, read-only inspection of one active server-owned AutoPTU battle binding. */
public final class FabricBattleInspectionAdminRuntime {
    private FabricBattleInspectionAdminRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("admin")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.literal("battle")
                                        .then(CommandManager.literal("inspect")
                                                .then(CommandManager.argument("battleId", StringArgumentType.word())
                                                        .executes(context -> inspect(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "battleId")))))))));
    }

    private static int inspect(ServerCommandSource source, String battleId) {
        String requested = battleId == null ? "" : battleId.strip();
        if (requested.isEmpty()) {
            source.sendError(Text.literal("Battle ID must not be blank."));
            return 0;
        }

        ServerPlayerEntity participant = null;
        for (ServerPlayerEntity candidate : source.getServer().getPlayerManager().getPlayerList()) {
            String candidateBattleId = FabricBattleChoiceRuntime.spectateId(candidate.getUuid());
            if (requested.equals(candidateBattleId)) {
                participant = candidate;
                break;
            }
        }

        if (participant == null) {
            source.sendError(Text.literal("No active server-owned AutoPTU battle matches that ID."));
            return 0;
        }
        ServerPlayerEntity inspectedParticipant = participant;

        FabricBattleChoiceRuntime.BattleStatusView status;
        try {
            status = FabricBattleChoiceRuntime.status(inspectedParticipant.getUuid());
        } catch (RuntimeException unavailable) {
            source.sendError(Text.literal("Active AutoPTU battle binding cannot be inspected safely: "
                    + safeMessage(unavailable)));
            return 0;
        }
        if (!status.bound()) {
            source.sendError(Text.literal("The requested battle binding disappeared during inspection."));
            return 0;
        }

        String canonicalPlayerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(inspectedParticipant.getUuid());
        String participantName = inspectedParticipant.getGameProfile().getName();
        Integer legalChoiceCount = status.authoritativeLegalChoiceCount();

        source.sendFeedback(() -> Text.literal("AutoPTU battle inspection — " + requested), false);
        source.sendFeedback(() -> Text.literal("Participant: " + participantName
                + " | canonical player " + canonicalPlayerId
                + " | UUID " + inspectedParticipant.getUuidAsString()), false);
        source.sendFeedback(() -> Text.literal("Bound actor: " + status.actorId()), false);
        if (legalChoiceCount == null) {
            source.sendFeedback(() -> Text.literal("Authoritative legal choices: unavailable"), false);
        } else {
            source.sendFeedback(() -> Text.literal("Authoritative legal choices: " + legalChoiceCount), false);
        }
        source.sendFeedback(() -> Text.literal(
                "Read-only active binding inspection complete; turn, HP, faint, result, RNG, legality and PTU outcomes are not reconstructed from Minecraft state."), false);
        return 1;
    }

    private static String safeMessage(RuntimeException error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }
}
