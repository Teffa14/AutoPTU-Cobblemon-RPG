package io.autoptu.cobblemon.fabric.rpg;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.authority.CanonicalQuestCatalogue;
import io.autoptu.cobblemon.authority.CanonicalQuestJournalQueryService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Read-only quest journal fallback backed exclusively by durable canonical server state. */
public final class FabricQuestJournalRuntime {
    private FabricQuestJournalRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("journal")
                                .executes(context -> showJournal(context.getSource())))
                        .then(CommandManager.literal("quests")
                                .executes(context -> showJournal(context.getSource())))
                        .then(CommandManager.literal("quest")
                                .then(CommandManager.argument("id", StringArgumentType.word())
                                        .executes(context -> showQuest(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "id")
                                        ))))));
    }

    private static int showJournal(ServerCommandSource source) {
        ServerPlayerEntity player = requireCanonicalPlayer(source);
        if (player == null) return 0;
        var snapshot = query(player).inspect(canonicalPlayerId(player));
        for (String line : formatJournalLines(snapshot)) player.sendMessage(Text.literal(line), false);
        return 1;
    }

    private static int showQuest(ServerCommandSource source, String questId) {
        ServerPlayerEntity player = requireCanonicalPlayer(source);
        if (player == null) return 0;
        try {
            var quest = query(player).inspectQuest(canonicalPlayerId(player), questId);
            for (String line : formatQuestLines(quest)) player.sendMessage(Text.literal(line), false);
            return 1;
        } catch (IllegalArgumentException error) {
            source.sendError(Text.literal(error.getMessage()));
            return 0;
        }
    }

    private static ServerPlayerEntity requireCanonicalPlayer(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("AutoPTU journal must be requested by an authenticated player."));
            return null;
        }
        String playerId = canonicalPlayerId(player);
        if (FabricCanonicalPlayerStoreRuntime.requireRepository(player.getServer()).findPlayer(playerId).isEmpty()) {
            source.sendError(Text.literal("Canonical Trainer state is not loaded."));
            return null;
        }
        return player;
    }

    private static CanonicalQuestJournalQueryService query(ServerPlayerEntity player) {
        return new CanonicalQuestJournalQueryService(
                CanonicalQuestCatalogue.DEFAULT,
                FabricCanonicalPlayerStoreRuntime.requireQuestJournalRepository(player.getServer())
        );
    }

    private static String canonicalPlayerId(ServerPlayerEntity player) {
        return FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
    }

    static List<String> formatJournalLines(CanonicalQuestJournalQueryService.JournalSnapshot journal) {
        List<String> lines = new ArrayList<>();
        lines.add("AutoPTU quest journal");
        if (journal.quests().isEmpty()) {
            lines.add("No accepted quests.");
        } else {
            for (var quest : journal.quests()) {
                lines.add(quest.questId() + " | " + quest.title() + " | " + quest.state());
            }
        }
        lines.add("Journal revision: " + journal.revision());
        return List.copyOf(lines);
    }

    static List<String> formatQuestLines(CanonicalQuestJournalQueryService.QuestSnapshot quest) {
        return List.of(
                quest.title() + " [" + quest.questId() + "]",
                "State: " + quest.state(),
                quest.summary(),
                "Objective: " + quest.objectiveText(),
                "Accepted revision: " + quest.acceptedRevision()
        );
    }
}
