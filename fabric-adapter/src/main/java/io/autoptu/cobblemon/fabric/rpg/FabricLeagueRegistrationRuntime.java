package io.autoptu.cobblemon.fabric.rpg;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.authority.CanonicalLeagueChallengeCatalogue;
import io.autoptu.cobblemon.authority.CanonicalLeagueRegistrationService;
import io.autoptu.cobblemon.authority.FileCanonicalLeagueRegistrationRepository;
import io.autoptu.cobblemon.authority.FileCanonicalTrainerRecordRepository;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Path;

/** Minecraft fallback surface for durable Gym/League registration. */
public final class FabricLeagueRegistrationRuntime {
    private FabricLeagueRegistrationRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("league")
                                .executes(context -> show(context.getSource()))
                                .then(CommandManager.literal("register")
                                        .then(CommandManager.argument("challengeId", StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    CanonicalLeagueChallengeCatalogue.DEFAULT.challenges()
                                                            .forEach(challenge -> builder.suggest(challenge.challengeId()));
                                                    return builder.buildFuture();
                                                })
                                                .executes(context -> register(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "challengeId"))))))));
    }

    private static int show(ServerCommandSource source) {
        ServerPlayerEntity player = authenticated(source);
        if (player == null) return 0;
        try {
            var summary = service(player).inspect(playerId(player));
            player.sendMessage(Text.literal("Ouros Gym/League — registrations " + summary.registrations().size()
                    + " — records " + summary.wins() + "W/" + summary.losses() + "L"
                    + " — badges " + summary.badgeIds().size()
                    + " — tournaments " + summary.tournamentRecordIds().size()), false);
            if (summary.registrations().isEmpty()) {
                player.sendMessage(Text.literal("No active Gym/League registrations. Use /autoptu league register <challengeId>."), false);
            } else {
                for (var registration : summary.registrations()) {
                    player.sendMessage(Text.literal("[REGISTERED] " + registration.displayName()
                            + " — " + registration.challengeId()), false);
                }
            }
            player.sendMessage(Text.literal("Registration never starts or resolves a PTU battle; AutoPTU authorizes those steps separately."), false);
            return 1;
        } catch (IllegalArgumentException | IllegalStateException error) {
            source.sendError(Text.literal("AutoPTU Gym/League state cannot be displayed safely."));
            return 0;
        }
    }

    private static int register(ServerCommandSource source, String challengeId) {
        ServerPlayerEntity player = authenticated(source);
        if (player == null) return 0;
        try {
            var result = service(player).register(playerId(player), challengeId);
            player.sendMessage(Text.literal((result.newlyRegistered() ? "Registered: " : "Already registered: ")
                    + result.challenge().displayName()
                    + ". Battle start and result remain pending authoritative AutoPTU handoff."), false);
            return 1;
        } catch (IllegalArgumentException error) {
            source.sendError(Text.literal(error.getMessage()));
            return 0;
        } catch (IllegalStateException error) {
            source.sendError(Text.literal("AutoPTU could not safely persist that Gym/League registration."));
            return 0;
        }
    }

    private static ServerPlayerEntity authenticated(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) source.sendError(Text.literal("Gym/League actions require an authenticated player."));
        return player;
    }

    private static CanonicalLeagueRegistrationService service(ServerPlayerEntity player) {
        Path root = canonicalStateRoot(player);
        return new CanonicalLeagueRegistrationService(
                CanonicalLeagueChallengeCatalogue.DEFAULT,
                FabricCanonicalPlayerStoreRuntime.requireRepository(player.getServer()),
                new FileCanonicalLeagueRegistrationRepository(root),
                new FileCanonicalTrainerRecordRepository(root));
    }

    private static String playerId(ServerPlayerEntity player) {
        return FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
    }

    private static Path canonicalStateRoot(ServerPlayerEntity player) {
        return player.getServer().getSavePath(WorldSavePath.ROOT)
                .resolve("autoptu")
                .resolve("canonical-state")
                .normalize();
    }
}
