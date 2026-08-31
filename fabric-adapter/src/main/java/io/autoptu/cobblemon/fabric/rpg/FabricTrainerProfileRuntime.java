package io.autoptu.cobblemon.fabric.rpg;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.authority.CanonicalTrainerProfileService;
import io.autoptu.cobblemon.authority.FileCanonicalTrainerProfileRepository;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Path;

/** Minecraft-visible profile card and bounded cosmetic selection over server-owned Trainer identity. */
public final class FabricTrainerProfileRuntime {
    private FabricTrainerProfileRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("trainer")
                                .then(CommandManager.literal("profile")
                                        .executes(context -> show(context.getSource()))
                                        .then(CommandManager.literal("title")
                                                .then(CommandManager.argument("titleId", StringArgumentType.word())
                                                        .executes(context -> chooseTitle(context.getSource(), StringArgumentType.getString(context, "titleId")))))
                                        .then(CommandManager.literal("theme")
                                                .then(CommandManager.argument("themeId", StringArgumentType.word())
                                                        .executes(context -> chooseTheme(context.getSource(), StringArgumentType.getString(context, "themeId")))))))));
    }

    private static int show(ServerCommandSource source) {
        var context = resolve(source);
        if (context == null) return 0;
        var snapshot = context.service().inspect(context.playerId());
        context.player().sendMessage(Text.literal("Trainer Profile — " + snapshot.titleDisplayName()
                + " — card " + snapshot.cardThemeId()
                + " — revision " + snapshot.revision()), false);
        context.player().sendMessage(Text.literal("Titles: " + String.join(", ", CanonicalTrainerProfileService.authoredTitles().keySet())
                + " — themes: " + String.join(", ", CanonicalTrainerProfileService.authoredCardThemes())), false);
        return 1;
    }

    private static int chooseTitle(ServerCommandSource source, String titleId) {
        var context = resolve(source);
        if (context == null) return 0;
        try {
            var snapshot = context.service().chooseTitle(context.playerId(), titleId);
            context.player().sendMessage(Text.literal("Trainer title set to " + snapshot.titleDisplayName() + "."), false);
            return 1;
        } catch (IllegalArgumentException error) {
            source.sendError(Text.literal("Unknown server-authored Trainer title: " + titleId));
            return 0;
        }
    }

    private static int chooseTheme(ServerCommandSource source, String themeId) {
        var context = resolve(source);
        if (context == null) return 0;
        try {
            var snapshot = context.service().chooseCardTheme(context.playerId(), themeId);
            context.player().sendMessage(Text.literal("Trainer card theme set to " + snapshot.cardThemeId() + "."), false);
            return 1;
        } catch (IllegalArgumentException error) {
            source.sendError(Text.literal("Unknown server-authored Trainer card theme: " + themeId));
            return 0;
        }
    }

    private static Context resolve(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("AutoPTU Trainer profile must be requested by an authenticated player."));
            return null;
        }
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        if (FabricCanonicalPlayerStoreRuntime.requireRepository(player.getServer()).findPlayer(playerId).isEmpty()) {
            source.sendError(Text.literal("Canonical Trainer state is not loaded."));
            return null;
        }
        return new Context(player, playerId, new CanonicalTrainerProfileService(
                new FileCanonicalTrainerProfileRepository(canonicalStateRoot(player))));
    }

    private static Path canonicalStateRoot(ServerPlayerEntity player) {
        return player.getServer().getSavePath(WorldSavePath.ROOT)
                .resolve("autoptu")
                .resolve("canonical-state")
                .normalize();
    }

    private record Context(ServerPlayerEntity player, String playerId, CanonicalTrainerProfileService service) { }
}
