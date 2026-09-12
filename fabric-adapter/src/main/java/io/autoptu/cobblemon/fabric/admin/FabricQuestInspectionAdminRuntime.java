package io.autoptu.cobblemon.fabric.admin;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.authority.CanonicalQuestCatalogue;
import io.autoptu.cobblemon.authority.CanonicalQuestJournalQueryService;
import io.autoptu.cobblemon.authority.CanonicalQuestObjectiveCatalogue;
import io.autoptu.cobblemon.authority.CanonicalQuestObjectiveService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Operator-only, read-only inspection of one Trainer's canonical quest journal and objective progress. */
public final class FabricQuestInspectionAdminRuntime {
    private FabricQuestInspectionAdminRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("admin")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.literal("quest")
                                        .then(CommandManager.literal("inspect")
                                                .then(CommandManager.argument("player", StringArgumentType.word())
                                                        .executes(context -> inspectJournal(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "player")))
                                                        .then(CommandManager.argument("quest", StringArgumentType.word())
                                                                .executes(context -> inspectQuest(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(context, "player"),
                                                                        StringArgumentType.getString(context, "quest"))))))))));
    }

    private static int inspectJournal(ServerCommandSource source, String playerName) {
        ResolvedPlayer resolved = resolve(source, playerName);
        if (resolved == null) return 0;

        try {
            var journal = query(source).inspect(resolved.playerId());
            source.sendFeedback(() -> Text.literal("AutoPTU quest inspection — " + resolved.displayName()), false);
            source.sendFeedback(() -> Text.literal("Canonical player: " + resolved.playerId()
                    + " | journal revision " + journal.revision()
                    + " | tracked " + valueOrNone(journal.trackedQuestId())), false);
            if (journal.quests().isEmpty()) {
                source.sendFeedback(() -> Text.literal("  no accepted quests"), false);
            } else {
                for (var quest : journal.quests()) {
                    source.sendFeedback(() -> Text.literal("  " + (quest.tracked() ? "[TRACKED] " : "")
                            + quest.questId() + " | " + quest.title()
                            + " | state " + quest.state()
                            + " | accepted revision " + quest.acceptedRevision()), false);
                }
            }
            source.sendFeedback(() -> Text.literal(
                    "Read-only canonical quest inspection complete; no objectives, rewards, progression or PTU state were mutated."), false);
            return 1;
        } catch (RuntimeException inconsistentQuestState) {
            source.sendError(Text.literal("Canonical quest state is inconsistent and cannot be inspected safely: "
                    + safeMessage(inconsistentQuestState)));
            return 0;
        }
    }

    private static int inspectQuest(ServerCommandSource source, String playerName, String questId) {
        ResolvedPlayer resolved = resolve(source, playerName);
        if (resolved == null) return 0;

        try {
            CanonicalQuestJournalQueryService.QuestSnapshot quest = query(source)
                    .inspectQuest(resolved.playerId(), questId);
            CanonicalQuestObjectiveService.QuestProgress progress = objectiveService(source)
                    .inspectQuest(resolved.playerId(), questId);

            source.sendFeedback(() -> Text.literal("AutoPTU quest inspection — " + resolved.displayName()), false);
            source.sendFeedback(() -> Text.literal(quest.title() + " [" + quest.questId() + "]"
                    + (quest.tracked() ? " [TRACKED]" : "")), false);
            source.sendFeedback(() -> Text.literal("State: " + quest.state()
                    + " | accepted revision " + quest.acceptedRevision()), false);
            source.sendFeedback(() -> Text.literal("Summary: " + quest.summary()), false);
            source.sendFeedback(() -> Text.literal("Objective: " + quest.objectiveText()), false);
            source.sendFeedback(() -> Text.literal("Objective progress: " + progress.completedCount()
                    + "/" + progress.totalCount()
                    + (progress.complete() ? " COMPLETE" : "")
                    + " | progress revision " + progress.revision()), false);
            if (progress.objectives().isEmpty()) {
                source.sendFeedback(() -> Text.literal("  no server-authored objective events configured"), false);
            } else {
                for (var objective : progress.objectives()) {
                    source.sendFeedback(() -> Text.literal("  " + (objective.completed() ? "[DONE] " : "[ ] ")
                            + objective.objective().objectiveId()
                            + " | " + objective.objective().description()), false);
                }
            }
            source.sendFeedback(() -> Text.literal(
                    "Read-only canonical quest inspection complete; no objective event, reward claim or PTU outcome was produced."), false);
            return 1;
        } catch (RuntimeException inconsistentQuestState) {
            source.sendError(Text.literal("Canonical quest state is inconsistent and cannot be inspected safely: "
                    + safeMessage(inconsistentQuestState)));
            return 0;
        }
    }

    private static ResolvedPlayer resolve(ServerCommandSource source, String playerName) {
        ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(playerName);
        if (target == null) {
            source.sendError(Text.literal("That Minecraft player must be online for canonical identity resolution."));
            return null;
        }
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(target.getUuid());
        if (FabricCanonicalPlayerStoreRuntime.requireRepository(source.getServer()).findPlayer(playerId).isEmpty()) {
            source.sendError(Text.literal("No canonical AutoPTU Trainer state exists for " + target.getGameProfile().getName() + "."));
            return null;
        }
        return new ResolvedPlayer(playerId, target.getGameProfile().getName());
    }

    private static CanonicalQuestJournalQueryService query(ServerCommandSource source) {
        return new CanonicalQuestJournalQueryService(
                CanonicalQuestCatalogue.DEFAULT,
                FabricCanonicalPlayerStoreRuntime.requireQuestJournalRepository(source.getServer()));
    }

    private static CanonicalQuestObjectiveService objectiveService(ServerCommandSource source) {
        return new CanonicalQuestObjectiveService(
                CanonicalQuestObjectiveCatalogue.DEFAULT,
                FabricCanonicalPlayerStoreRuntime.requireQuestJournalRepository(source.getServer()),
                FabricCanonicalPlayerStoreRuntime.requireQuestObjectiveRepository(source.getServer()));
    }

    private static String valueOrNone(String value) {
        return value == null || value.isBlank() ? "none" : value;
    }

    private static String safeMessage(RuntimeException error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }

    private record ResolvedPlayer(String playerId, String displayName) {}
}
