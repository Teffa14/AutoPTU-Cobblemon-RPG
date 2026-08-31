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
        FabricTrainerProgressionRuntime.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("trainer")
                                .executes(context -> show(context.getSource()))
                                .then(CommandManager.literal("skills")
                                        .executes(context -> showSkills(context.getSource())))
                                .then(CommandManager.literal("classes")
                                        .executes(context -> showClasses(context.getSource())))
                                .then(CommandManager.literal("features")
                                        .executes(context -> showFeatures(context.getSource()))))));
    }

    private static int show(ServerCommandSource source) {
        TrainerRequest request = resolve(source);
        if (request == null) return 0;
        for (String line : formatLines(request.summary())) request.player().sendMessage(Text.literal(line), false);
        return 1;
    }

    private static int showSkills(ServerCommandSource source) {
        TrainerRequest request = resolve(source);
        if (request == null) return 0;
        for (String line : formatSkillLines(request.summary())) request.player().sendMessage(Text.literal(line), false);
        return 1;
    }

    private static int showClasses(ServerCommandSource source) {
        TrainerRequest request = resolve(source);
        if (request == null) return 0;
        for (String line : formatClassLines(request.summary())) request.player().sendMessage(Text.literal(line), false);
        return 1;
    }

    private static int showFeatures(ServerCommandSource source) {
        TrainerRequest request = resolve(source);
        if (request == null) return 0;
        for (String line : formatFeatureLines(request.summary())) request.player().sendMessage(Text.literal(line), false);
        return 1;
    }

    private static TrainerRequest resolve(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("AutoPTU Trainer must be requested by an authenticated player."));
            return null;
        }

        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        CanonicalTrainerSummaryService service = new CanonicalTrainerSummaryService(
                FabricCanonicalPlayerStoreRuntime.requireRepository(player.getServer()));
        CanonicalTrainerSummaryService.Summary summary = service.find(playerId).orElse(null);
        if (summary == null) {
            source.sendError(Text.literal("Canonical Trainer state is unavailable."));
            return null;
        }
        return new TrainerRequest(player, summary);
    }

    static List<String> formatLines(CanonicalTrainerSummaryService.Summary summary) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("AutoPTU Trainer");
        lines.add("Classes: " + joinOrNone(summary.trainerClasses()));
        lines.add("Skills: " + formatSkills(summary.skills()));
        lines.add("Features: " + joinOrNone(summary.trainerFeatures()));
        lines.add("Pokemon capabilities: " + joinOrNone(summary.availablePokemonCapabilities()));
        lines.add("Action points: " + summary.actionPoints());
        lines.add("Initiative modifier: " + signed(summary.initiativeModifier()));
        lines.add("Initiative Speed: " + (summary.explicitInitiativeSpeed() == null ? "unavailable" : summary.explicitInitiativeSpeed()));
        lines.add("Team: " + (summary.teamId().isBlank() ? "none" : summary.teamId()));
        lines.add("Revision: " + summary.revision());
        return List.copyOf(lines);
    }

    static List<String> formatSkillLines(CanonicalTrainerSummaryService.Summary summary) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("AutoPTU Trainer skills");
        if (summary.skills().isEmpty()) {
            lines.add("No canonical Trainer skills are available.");
        } else {
            for (CanonicalTrainerSummaryService.Skill skill : summary.skills()) {
                lines.add(skill.id() + ": " + skill.rank());
            }
        }
        lines.add("Revision: " + summary.revision());
        return List.copyOf(lines);
    }

    static List<String> formatClassLines(CanonicalTrainerSummaryService.Summary summary) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("AutoPTU Trainer classes");
        if (summary.trainerClasses().isEmpty()) {
            lines.add("No canonical Trainer classes are available.");
        } else {
            for (String trainerClass : summary.trainerClasses()) {
                lines.add(trainerClass);
            }
        }
        lines.add("Revision: " + summary.revision());
        return List.copyOf(lines);
    }

    static List<String> formatFeatureLines(CanonicalTrainerSummaryService.Summary summary) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("AutoPTU Trainer features");
        if (summary.trainerFeatures().isEmpty()) {
            lines.add("No canonical Trainer features are available.");
        } else {
            lines.addAll(summary.trainerFeatures());
        }
        lines.add("Revision: " + summary.revision());
        return List.copyOf(lines);
    }

    private static String formatSkills(List<CanonicalTrainerSummaryService.Skill> skills) {
        return skills.isEmpty()
                ? "none"
                : skills.stream().map(skill -> skill.id() + " " + skill.rank()).reduce((a, b) -> a + ", " + b).orElse("none");
    }

    private static String joinOrNone(List<String> values) {
        return values.isEmpty() ? "none" : String.join(", ", values);
    }

    private static String signed(int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }

    private record TrainerRequest(ServerPlayerEntity player, CanonicalTrainerSummaryService.Summary summary) {}
}
