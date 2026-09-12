package io.autoptu.cobblemon.fabric.admin;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.authority.BattleArenaSnapshot;
import io.autoptu.cobblemon.authority.CanonicalStarterCatalogue;
import io.autoptu.cobblemon.authority.CanonicalStarterSelectionDecision;
import io.autoptu.cobblemon.authority.CanonicalStarterSelectionService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/** Operator-only starter recovery/provisioning through the canonical one-time starter service. */
public final class FabricStarterGrantAdminRuntime {
    private static final CanonicalStarterCatalogue CATALOGUE = new CanonicalStarterCatalogue();

    private FabricStarterGrantAdminRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("admin")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.literal("grant")
                                        .then(CommandManager.literal("starter")
                                                .then(CommandManager.argument("player", StringArgumentType.word())
                                                        .then(CommandManager.argument("starter", StringArgumentType.word())
                                                                .executes(context -> grant(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(context, "player"),
                                                                        StringArgumentType.getString(context, "starter"))))))))));
    }

    private static int grant(ServerCommandSource source, String playerName, String requestedStarter) {
        ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(playerName);
        if (target == null) {
            source.sendError(Text.literal("That Minecraft player must be online for canonical identity resolution."));
            return 0;
        }

        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(target.getUuid());
        var playerRepository = FabricCanonicalPlayerStoreRuntime.requireRepository(source.getServer());
        if (playerRepository.findPlayer(playerId).isEmpty()) {
            source.sendError(Text.literal(
                    "No canonical AutoPTU Trainer state exists for " + target.getGameProfile().getName() + "."));
            return 0;
        }

        BlockPos pos = target.getBlockPos();
        BattleArenaSnapshot arena = new BattleArenaSnapshot(
                target.getServerWorld().getRegistryKey().getValue().toString(),
                pos.getX(), pos.getY(), pos.getZ(),
                1, 0,
                0, 1
        );
        CanonicalStarterSelectionDecision decision = new CanonicalStarterSelectionService(
                CATALOGUE,
                FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(source.getServer()),
                FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(source.getServer())
        ).choose(playerId, requestedStarter, arena);

        return switch (decision.outcome()) {
            case CHOSEN -> {
                source.sendFeedback(() -> Text.literal(
                        "Granted canonical starter " + displayName(decision.speciesId())
                                + " to " + target.getGameProfile().getName()
                                + " as " + decision.pokemonId() + "."), true);
                target.sendMessage(Text.literal(
                        "Your AutoPTU starter is now " + displayName(decision.speciesId()) + "."), false);
                yield 1;
            }
            case ALREADY_CHOSEN -> {
                source.sendFeedback(() -> Text.literal(
                        "No change: " + target.getGameProfile().getName()
                                + " already owns canonical starter/party state"
                                + (decision.speciesId().isBlank() ? "." : " beginning with " + displayName(decision.speciesId()) + ".")), false);
                yield 1;
            }
            case INVALID_STARTER -> {
                source.sendError(Text.literal(
                        "Unknown canonical starter '" + requestedStarter + "'. Allowed: " + configuredStarterIds()));
                yield 0;
            }
            case INVALID_REQUEST, CONFLICT -> {
                source.sendError(Text.literal("Canonical starter grant was not committed: " + safeDetail(decision.detail())));
                yield 0;
            }
        };
    }

    private static String configuredStarterIds() {
        return CATALOGUE.configuredStarters().stream()
                .map(CanonicalStarterCatalogue.StarterOption::speciesId)
                .reduce((left, right) -> left + ", " + right)
                .orElse("none");
    }

    private static String displayName(String speciesId) {
        return CATALOGUE.findConfigured(speciesId)
                .map(CanonicalStarterCatalogue.StarterOption::displayName)
                .orElse(speciesId);
    }

    private static String safeDetail(String detail) {
        return detail == null || detail.isBlank() ? "canonical service rejected the request" : detail;
    }
}
