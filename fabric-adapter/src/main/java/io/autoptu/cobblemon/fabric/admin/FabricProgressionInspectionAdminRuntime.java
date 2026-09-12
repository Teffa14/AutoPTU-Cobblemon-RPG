package io.autoptu.cobblemon.fabric.admin;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.authority.CanonicalTrainerSummaryService;
import io.autoptu.cobblemon.authority.FileCanonicalTrainerProgressionRepository;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Path;

/** Operator-only, read-only inspection of persistent canonical Trainer progression. */
public final class FabricProgressionInspectionAdminRuntime {
    private FabricProgressionInspectionAdminRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("admin")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.literal("progression")
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
        CanonicalTrainerSummaryService.Summary trainer = new CanonicalTrainerSummaryService(playerRepository)
                .find(playerId)
                .orElse(null);
        if (trainer == null) {
            source.sendError(Text.literal("No canonical AutoPTU Trainer state exists for " + target.getGameProfile().getName() + "."));
            return 0;
        }

        FileCanonicalTrainerProgressionRepository.ProgressionState progression;
        try {
            progression = new FileCanonicalTrainerProgressionRepository(canonicalStateRoot(source))
                    .find(playerId)
                    .orElse(null);
        } catch (RuntimeException inconsistentProgression) {
            source.sendError(Text.literal("Canonical progression state is inconsistent and cannot be inspected safely: "
                    + safeMessage(inconsistentProgression)));
            return 0;
        }

        source.sendFeedback(() -> Text.literal("AutoPTU progression inspection — " + target.getGameProfile().getName()), false);
        source.sendFeedback(() -> Text.literal("Canonical player: " + playerId + " | UUID " + target.getUuidAsString()), false);

        if (progression == null) {
            source.sendFeedback(() -> Text.literal("Progression: unavailable (no persisted progression record)"), false);
        } else {
            source.sendFeedback(() -> Text.literal("Progression: Trainer level " + progression.trainerLevel()
                    + " | XP " + progression.trainerXp()
                    + " | revision " + progression.revision()), false);
        }

        source.sendFeedback(() -> Text.literal("Trainer progression projection: AP " + trainer.actionPoints()
                + " | classes " + display(trainer.trainerClasses())
                + " | Features " + display(trainer.trainerFeatures())
                + " | Trainer revision " + trainer.revision()), false);
        source.sendFeedback(() -> Text.literal(
                "XP thresholds and pending unlocks are unavailable because the canonical progression schema does not persist them; no PTU progression policy was inferred locally."), false);
        source.sendFeedback(() -> Text.literal(
                "Read-only canonical progression inspection complete; no progression, Trainer, Cobblemon, or PTU state was mutated."), false);
        return 1;
    }

    private static Path canonicalStateRoot(ServerCommandSource source) {
        return source.getServer().getSavePath(WorldSavePath.ROOT)
                .resolve("autoptu")
                .resolve("canonical-state")
                .normalize();
    }

    private static String display(java.util.List<String> values) {
        return values == null || values.isEmpty() ? "none" : String.join(", ", values);
    }

    private static String safeMessage(RuntimeException error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }
}
