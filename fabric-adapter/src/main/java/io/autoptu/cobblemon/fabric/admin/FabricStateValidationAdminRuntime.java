package io.autoptu.cobblemon.fabric.admin;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.authority.CanonicalBagQueryService;
import io.autoptu.cobblemon.authority.CanonicalPartyQueryService;
import io.autoptu.cobblemon.authority.CanonicalPartySummary;
import io.autoptu.cobblemon.authority.CanonicalPlayerStateValidationService;
import io.autoptu.cobblemon.authority.CanonicalTrainerSummaryService;
import io.autoptu.cobblemon.authority.FileCanonicalTrainerProgressionRepository;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Operator-only read-only structural validation for canonical RPG state.
 *
 * <p>The optional player argument validates one authenticated online player. Without an argument,
 * every online player with server-owned canonical identity is checked. This runtime does not repair
 * state and does not evaluate PTU legality, progression policy, battle outcomes, item effects, or
 * Cobblemon gameplay payloads.</p>
 */
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

        ValidationResult result = validateTarget(source.getServer(), target);
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
            ValidationResult result = validateTarget(source.getServer(), player);
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

    private static ValidationResult validateTarget(MinecraftServer server, ServerPlayerEntity target) {
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(target.getUuid());
        String playerName = target.getGameProfile().getName();
        try {
            var playerRepository = FabricCanonicalPlayerStoreRuntime.requireRepository(server);
            if (playerRepository.findPlayer(playerId).isEmpty()) {
                return ValidationResult.repositoryFailure(
                        playerName,
                        playerId,
                        "CANONICAL_TRAINER_MISSING",
                        "No canonical AutoPTU Trainer state exists for the authenticated player.");
            }

            CanonicalTrainerSummaryService.Summary trainer = new CanonicalTrainerSummaryService(playerRepository)
                    .find(playerId)
                    .orElseThrow(() -> new IllegalStateException("canonical Trainer disappeared during validation"));
            CanonicalPartySummary party = new CanonicalPartyQueryService(
                    FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(server),
                    FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(server))
                    .findParty(playerId)
                    .orElse(null);
            CanonicalBagQueryService.BagSnapshot bag = new CanonicalBagQueryService(
                    FabricCanonicalPlayerStoreRuntime.requireAssetRepository(server))
                    .inspect(playerId);
            Optional<FileCanonicalTrainerProgressionRepository.ProgressionState> progression =
                    new FileCanonicalTrainerProgressionRepository(canonicalStateRoot(server)).find(playerId);

            CanonicalPlayerStateValidationService.ValidationReport report =
                    new CanonicalPlayerStateValidationService().validate(playerId, trainer, party, bag, progression);
            return ValidationResult.fromReport(playerName, report);
        } catch (RuntimeException inconsistentState) {
            return ValidationResult.repositoryFailure(
                    playerName,
                    playerId,
                    "REPOSITORY_CONSISTENCY_READ_FAILED",
                    safeMessage(inconsistentState));
        }
    }

    private static void emitTargetReport(ServerCommandSource source, ValidationResult result) {
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

    private static Path canonicalStateRoot(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT)
                .resolve("autoptu")
                .resolve("canonical-state")
                .normalize();
    }

    private static String safeMessage(RuntimeException error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }

    private record ValidationResult(
            String playerName,
            String playerId,
            CanonicalPlayerStateValidationService.ValidationReport report,
            String repositoryErrorCode,
            String repositoryErrorMessage
    ) {
        static ValidationResult fromReport(
                String playerName,
                CanonicalPlayerStateValidationService.ValidationReport report
        ) {
            return new ValidationResult(playerName, report.playerId(), report, null, null);
        }

        static ValidationResult repositoryFailure(
                String playerName,
                String playerId,
                String code,
                String message
        ) {
            return new ValidationResult(playerName, playerId, null, code, message);
        }

        boolean valid() {
            return repositoryErrorCode == null && report.valid();
        }

        long warningCount() {
            return report == null ? 0L : report.warningCount();
        }
    }
}
