package io.autoptu.cobblemon.fabric.admin;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.authority.CanonicalStarterCatalogue;
import io.autoptu.cobblemon.fabric.rpg.FabricStarterGrantService;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Operator-only starter provisioning backed by the reusable canonical starter service. */
public final class FabricStarterGrantAdminRuntime {
    private static final CanonicalStarterCatalogue CATALOGUE = new CanonicalStarterCatalogue();
    private static final FabricStarterGrantService SERVICE = new FabricStarterGrantService();

    private FabricStarterGrantAdminRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var starter = CommandManager.literal("starter")
                    .then(CommandManager.argument("player", StringArgumentType.word())
                            .then(CommandManager.argument("species", StringArgumentType.word())
                                    .executes(context -> grant(
                                            context.getSource(),
                                            StringArgumentType.getString(context, "player"),
                                            StringArgumentType.getString(context, "species")
                                    ))));
            var grant = CommandManager.literal("grant").then(starter);
            var admin = CommandManager.literal("admin")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(grant);
            dispatcher.register(CommandManager.literal("autoptu").then(admin));
        });
    }

    private static int grant(ServerCommandSource source, String playerName, String requestedStarter) {
        ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(playerName);
        if (target == null) {
            source.sendError(Text.literal("That Minecraft player must be online for canonical identity resolution."));
            return 0;
        }

        FabricStarterGrantService.Result result = SERVICE.grant(target, requestedStarter);
        return switch (result.outcome()) {
            case GRANTED -> {
                source.sendFeedback(() -> Text.literal(
                        "Granted canonical starter " + displayName(result.speciesId())
                                + " to " + target.getGameProfile().getName()
                                + " as " + result.pokemonId() + "."), true);
                target.sendMessage(Text.literal(
                        "Your AutoPTU starter is now " + displayName(result.speciesId()) + "."), false);
                yield 1;
            }
            case ALREADY_CHOSEN -> {
                source.sendFeedback(() -> Text.literal(
                        "No change: " + target.getGameProfile().getName()
                                + " already owns canonical starter/party state"
                                + (result.speciesId().isBlank()
                                ? "."
                                : " beginning with " + displayName(result.speciesId()) + ".")), false);
                yield 1;
            }
            case INVALID_STARTER -> {
                source.sendError(Text.literal(
                        "Unknown canonical starter '" + requestedStarter + "'. Allowed: " + configuredStarterIds()));
                yield 0;
            }
            case MISSING_TRAINER -> {
                source.sendError(Text.literal(
                        "No canonical AutoPTU Trainer state exists for " + target.getGameProfile().getName() + "."));
                yield 0;
            }
            case REJECTED -> {
                source.sendError(Text.literal(
                        "Canonical starter grant was not committed: " + safeDetail(result.detail())));
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
