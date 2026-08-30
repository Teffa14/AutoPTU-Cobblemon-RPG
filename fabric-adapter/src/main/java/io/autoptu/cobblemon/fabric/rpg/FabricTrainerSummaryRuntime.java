package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalTrainerSummaryService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Authenticated read-only fallback for the persistent canonical Trainer profile. */
public final class FabricTrainerSummaryRuntime {
    private FabricTrainerSummaryRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("trainer")
                                .executes(context -> show(context.getSource())))));
    }

    private static int show(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("AutoPTU Trainer must be requested by an authenticated player."));
            return 0;
        }

        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        CanonicalTrainerSummaryService service = new CanonicalTrainerSummaryService(
                FabricCanonicalPlayerStoreRuntime.requireRepository(player.getServer()));
        CanonicalTrainerSummaryService.Summary summary = service.find(playerId).orElse(null);
        if (summary == null) {
            source.sendError(Text.literal("Canonical Trainer state is unavailable."));
            return 0;
        }

        for (String line : formatLines(summary)) player.sendMessage(Text.literal(line), false);
        return 1;
    }

    static List<String> formatLines(CanonicalTrainerSummaryService.Summary summary) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("AutoPTU Trainer");
        lines.add("Classes: " + joinOrNone(summary.trainerClasses()));
        lines.add("Skills: " + (summary.skills().isEmpty()
                ? "none"
                : summary.skills().stream().map(skill -> skill.id() + " " + skill.rank()).reduce((a, b) -> a + ", " + b).orElse("none")));
        lines.add("Features: " + joinOrNone(summary.trainerFeatures()));
        lines.add("Pokemon capabilities: " + joinOrNone(summary.availablePokemonCapabilities()));
        lines.add("Action points: " + summary.actionPoints());
        lines.add("Initiative modifier: " + signed(summary.initiativeModifier()));
        lines.add("Initiative Speed: " + (summary.explicitInitiativeSpeed() == null ? "unavailable" : summary.explicitInitiativeSpeed()));
        lines.add("Team: " + (summary.teamId().isBlank() ? "none" : summary.teamId()));
        lines.add("Revision: " + summary.revision());
        return List.copyOf(lines);
    }

    private static String joinOrNone(List<String> values) {
        return values.isEmpty() ? "none" : String.join(", ", values);
    }

    private static String signed(int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }
}
