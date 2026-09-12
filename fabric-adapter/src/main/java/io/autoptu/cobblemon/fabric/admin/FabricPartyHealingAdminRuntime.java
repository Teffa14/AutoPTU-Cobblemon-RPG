package io.autoptu.cobblemon.fabric.admin;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.authority.CanonicalPartyHealingDecision;
import io.autoptu.cobblemon.authority.CanonicalPartyHealingService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Operator-only HP restoration through the canonical persistent party healing authority. */
public final class FabricPartyHealingAdminRuntime implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("admin")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.literal("heal")
                                        .then(CommandManager.argument("player", StringArgumentType.word())
                                                .executes(context -> heal(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "player"))))))));
    }

    private static int heal(ServerCommandSource source, String playerName) {
        ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(playerName);
        if (target == null) {
            source.sendError(Text.literal("That Minecraft player must be online for canonical identity resolution."));
            return 0;
        }

        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(target.getUuid());
        var playerRepository = FabricCanonicalPlayerStoreRuntime.requireRepository(source.getServer());
        if (playerRepository.findPlayer(playerId).isEmpty()) {
            source.sendError(Text.literal("No canonical AutoPTU Trainer state exists for "
                    + target.getGameProfile().getName() + "."));
            return 0;
        }

        CanonicalPartyHealingDecision decision = new CanonicalPartyHealingService(
                FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(source.getServer()),
                FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(source.getServer()))
                .healParty(playerId);

        return switch (decision.outcome()) {
            case APPLIED -> {
                source.sendFeedback(() -> Text.literal(successMessage(target, decision)), true);
                yield 1;
            }
            case PARTIAL -> {
                source.sendFeedback(() -> Text.literal(successMessage(target, decision)), true);
                source.sendError(Text.literal("Some canonical party members could not be healed safely for "
                        + target.getGameProfile().getName() + ": "
                        + String.join(", ", decision.failedPokemonIds())));
                yield decision.changedState() ? 1 : 0;
            }
            case NO_PARTY -> {
                source.sendError(Text.literal("No persistent AutoPTU party is configured for "
                        + target.getGameProfile().getName() + "."));
                yield 0;
            }
            case INVALID_REQUEST -> {
                source.sendError(Text.literal("AutoPTU rejected the canonical healing request: " + decision.reason()));
                yield 0;
            }
        };
    }

    private static String successMessage(ServerPlayerEntity target, CanonicalPartyHealingDecision decision) {
        return "AutoPTU admin healing — " + target.getGameProfile().getName()
                + ": " + decision.healedPokemon() + " healed, "
                + decision.alreadyFullPokemon() + " already at full HP. "
                + "Statuses and injuries were left unchanged.";
    }
}
