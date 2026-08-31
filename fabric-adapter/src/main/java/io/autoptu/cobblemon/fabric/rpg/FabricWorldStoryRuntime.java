package io.autoptu.cobblemon.fabric.rpg;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.authority.CanonicalWorldStoryCatalogue;
import io.autoptu.cobblemon.authority.CanonicalWorldStoryService;
import io.autoptu.cobblemon.authority.FileCanonicalWorldStoryRepository;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Path;

/** Minecraft read/request surface backed by server-authored world-story choices. */
public final class FabricWorldStoryRuntime {
    private FabricWorldStoryRuntime() {}

    public static void register() {
        FabricMailRuntime.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("story")
                                .executes(context -> inspect(context.getSource()))
                                .then(CommandManager.literal("choose")
                                        .then(CommandManager.argument("nodeId", StringArgumentType.word())
                                                .then(CommandManager.argument("choiceId", StringArgumentType.word())
                                                        .executes(context -> choose(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "nodeId"),
                                                                StringArgumentType.getString(context, "choiceId")))))))));
    }

    private static int inspect(ServerCommandSource source) {
        ServerPlayerEntity player = requirePlayer(source);
        if (player == null) return 0;
        var snapshot = service(player).inspect(playerId(player));
        player.sendMessage(Text.literal("World story — choices " + snapshot.selectedChoices().size()
                + " — flags " + snapshot.storyFlags().size()
                + " — revision " + snapshot.revision()), false);
        if (!snapshot.selectedChoices().isEmpty()) {
            snapshot.selectedChoices().forEach((node, choice) ->
                    player.sendMessage(Text.literal("  " + node + " = " + choice), false));
        }
        return 1;
    }

    private static int choose(ServerCommandSource source, String nodeId, String choiceId) {
        ServerPlayerEntity player = requirePlayer(source);
        if (player == null) return 0;
        try {
            var result = service(player).choose(playerId(player), nodeId, choiceId);
            player.sendMessage(Text.literal(result.newlyCommitted()
                    ? "Story choice committed: " + nodeId + " = " + choiceId
                    : "Story choice already committed: " + nodeId + " = " + choiceId), false);
            return 1;
        } catch (IllegalArgumentException | IllegalStateException error) {
            source.sendError(Text.literal(error.getMessage()));
            return 0;
        }
    }

    private static ServerPlayerEntity requirePlayer(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("AutoPTU world story must be requested by an authenticated player."));
            return null;
        }
        if (FabricCanonicalPlayerStoreRuntime.requireRepository(player.getServer()).findPlayer(playerId(player)).isEmpty()) {
            source.sendError(Text.literal("Canonical Trainer state is not loaded."));
            return null;
        }
        return player;
    }

    private static CanonicalWorldStoryService service(ServerPlayerEntity player) {
        return new CanonicalWorldStoryService(
                CanonicalWorldStoryCatalogue.DEFAULT,
                new FileCanonicalWorldStoryRepository(canonicalStateRoot(player)));
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
