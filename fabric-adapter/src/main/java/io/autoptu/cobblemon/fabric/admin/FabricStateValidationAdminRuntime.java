package io.autoptu.cobblemon.fabric.admin;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.authority.CanonicalPlayerStateValidationService;
import io.autoptu.cobblemon.fabric.rpg.FabricStateValidationService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Operator-only command surface for read-only canonical RPG state validation. */
public final class FabricStateValidationAdminRuntime implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("admin")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.literal("state")
                                        .then(CommandManager.literal("validate")
                                                .executes(context -> validateAllOnline(context.getSource()))
                                                .then(CommandManager.argument("player", StringArgumentType.word())
                                                        .executes(context -> validateNamed(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "player")))))))));
    }

    private static int validateNamed(ServerCommandSource source, String playerName) {
        ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(playerName);
        if (target == null) {
            source.sendError(Text.literal("That Minecraft player must be online for canonical identity resolution."));
            return 0;
        }

        FabricStateValidationService.ValidationResult result =
                FabricStateValidationService.validate(source.getServer(), target);
        emitTargetReport(source, result);
        source.sendFeedback(() -> Text.literal(
                "Read-only state validation complete; no RPG/PTU state was mutated or repaired."), false);
        return result.valid() ? 1 : 0;
    }

    private static int validateAllOnline(ServerCommandSource source) {
        var players = source.getServer().getPlayerManager().getPlayerList();
        if (players.isEmpty()) {
            source.sendFeedback(() -> Text.literal(
                    "AutoPTU state validation: no online players are available for authenticated identity resolution."), false);
            return 1;
        }

        int valid = 0;
        int invalid = 0;
        long warnings = 0;
        for (ServerPlayerEntity player : players) {
            FabricStateValidationService.ValidationResult result =
                    FabricStateValidationService.validate(source.getServer(), player);
            emitTargetReport(source, result);
            if (result.valid()) valid++; else invalid++;
            warnings += result.warningCount();
        }

        int checked = valid + invalid;
        int finalValid = valid;
        int finalInvalid = invalid;
        long finalWarnings = warnings;
        source.sendFeedback(() -> Text.literal("AutoPTU state validation summary: checked " + checked
                + " online player(s) | valid " + finalValid
                + " | invalid " + finalInvalid
                + " | warning(s) " + finalWarnings + "."), false);
        source.sendFeedback(() -> Text.literal(
                "Read-only state validation complete; no RPG/PTU state was mutated or repaired."), false);
        return invalid == 0 ? 1 : 0;
    }

    private static void emitTargetReport(
            ServerCommandSource source,
            FabricStateValidationService.ValidationResult result
    ) {
        source.sendFeedback(() -> Text.literal("AutoPTU state validation — " + result.playerName()
                + " | canonical " + result.playerId()), false);
        if (result.repositoryErrorCode() != null) {
            source.sendFeedback(() -> Text.literal("ERROR " + result.repositoryErrorCode()
                    + ": " + result.repositoryErrorMessage()), false);
            return;
        }
        if (result.report().issues().isEmpty()) {
            source.sendFeedback(() -> Text.literal(
                    "VALID: Trainer, party, bag and progression projections are structurally consistent."), false);
            return;
        }
        for (CanonicalPlayerStateValidationService.Issue issue : result.report().issues()) {
            source.sendFeedback(() -> Text.literal(issue.severity() + " " + issue.code()
                    + ": " + issue.message()), false);
        }
        source.sendFeedback(() -> Text.literal("Result: " + result.report().errorCount() + " error(s), "
                + result.report().warningCount() + " warning(s)."), false);
    }
}
