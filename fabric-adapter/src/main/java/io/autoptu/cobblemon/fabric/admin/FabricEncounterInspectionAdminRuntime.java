package io.autoptu.cobblemon.fabric.admin;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.fabric.battle.WorldEncounterTriggerRequestService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Optional;

/** Operator-only, read-only inspection of one Trainer's durable visible-world encounter request. */
public final class FabricEncounterInspectionAdminRuntime {
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
                                                                StringArgumentType.getString(context, "player")))))))));
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

    private static String valueOrNone(String value) {
        return value == null || value.isBlank() ? "none" : value;
    }

    private static String safeMessage(RuntimeException error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }
}
