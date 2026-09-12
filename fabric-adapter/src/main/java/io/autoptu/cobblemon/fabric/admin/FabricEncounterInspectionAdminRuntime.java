package io.autoptu.cobblemon.fabric.admin;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.fabric.battle.WorldEncounterTriggerRequestService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import io.autoptu.cobblemon.fabric.world.AuthoritativeWildEncounterProjectionService;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Optional;

/** Operator-only inspection and controlled server-authored encounter projection tools. */
public final class FabricEncounterInspectionAdminRuntime {
    private static final AuthoritativeWildEncounterProjectionService PROJECTION =
            new AuthoritativeWildEncounterProjectionService();

    private FabricEncounterInspectionAdminRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("admin")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.literal("encounter")
                                        .then(CommandManager.literal("inspect")
                                                .then(CommandManager.argument("player", StringArgumentType.word())
                                                        .executes(context -> inspect(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "player")))))
                                        .then(CommandManager.literal("spawn")
                                                .then(CommandManager.argument("table_or_blueprint", StringArgumentType.word())
                                                        .executes(context -> spawn(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "table_or_blueprint")))))))));
    }

    private static int inspect(ServerCommandSource source, String playerName) {
        ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(playerName);
        if (target == null) {
            source.sendError(Text.literal("That Minecraft player must be online for canonical identity resolution."));
            return 0;
        }

        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(target.getUuid());
        if (FabricCanonicalPlayerStoreRuntime.requireRepository(source.getServer()).findPlayer(playerId).isEmpty()) {
            source.sendError(Text.literal("No canonical AutoPTU Trainer state exists for " + target.getGameProfile().getName() + "."));
            return 0;
        }

        Optional<WorldEncounterTriggerRequestService.Request> pending;
        try {
            pending = FabricCanonicalPlayerStoreRuntime
                    .requireActiveEncounterSessionRepository(source.getServer())
                    .findPending(playerId);
        } catch (RuntimeException inconsistentState) {
            source.sendError(Text.literal("Canonical encounter session state cannot be inspected safely: "
                    + safeMessage(inconsistentState)));
            return 0;
        }

        source.sendFeedback(() -> Text.literal("AutoPTU encounter inspection — " + target.getGameProfile().getName()), false);
        source.sendFeedback(() -> Text.literal("Canonical player: " + playerId + " | UUID " + target.getUuidAsString()), false);
        if (pending.isEmpty()) {
            source.sendFeedback(() -> Text.literal("Active encounter request: none"), false);
        } else {
            WorldEncounterTriggerRequestService.Request request = pending.get();
            source.sendFeedback(() -> Text.literal("Encounter: " + request.canonicalEncounterId()), false);
            source.sendFeedback(() -> Text.literal("Visible actor correlation: " + valueOrNone(request.externalWildActorId())), false);
            source.sendFeedback(() -> Text.literal("World source: " + request.zoneId() + "/" + request.contextId()), false);
            source.sendFeedback(() -> Text.literal("Observed at: " + request.dimensionId()
                    + " " + request.blockX() + "," + request.blockY() + "," + request.blockZ()
                    + " | server tick " + request.serverTick()), false);
        }
        source.sendFeedback(() -> Text.literal(
                "Read-only canonical encounter inspection complete; no battle-start legality, combatants, RNG or outcome were inferred or mutated."), false);
        return 1;
    }

    private static int spawn(ServerCommandSource source, String authoredId) {
        ServerPlayerEntity operator = source.getPlayer();
        if (operator == null) {
            source.sendError(Text.literal("Encounter projection must be requested by an in-world operator."));
            return 0;
        }

        AuthoritativeWildEncounterProjectionService.ProjectionResult result;
        try {
            result = PROJECTION.project(operator.getServerWorld(), authoredId);
        } catch (RuntimeException rejected) {
            source.sendError(Text.literal("AutoPTU rejected encounter projection: " + safeMessage(rejected)));
            return 0;
        }

        source.sendFeedback(() -> Text.literal("AutoPTU projected server-authored "
                + result.kind().name().toLowerCase() + " " + result.authoredId()
                + " with " + result.encounters().size() + " visible actor(s)."), false);
        for (AuthoritativeWildEncounterProjectionService.ProjectedEncounter encounter : result.encounters()) {
            source.sendFeedback(() -> Text.literal("  " + encounter.canonicalEncounterId()
                    + " -> actor " + encounter.presentationEntityUuid()), false);
        }
        source.sendFeedback(() -> Text.literal(
                "Species, level, stats, moves, HP and PTU legality came only from server-authored canonical content; no battle was started."), false);
        return result.encounters().size();
    }

    private static String valueOrNone(String value) {
        return value == null || value.isBlank() ? "none" : value;
    }

    private static String safeMessage(RuntimeException error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }
}
