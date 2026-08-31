package io.autoptu.cobblemon.fabric.rpg;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.authority.CanonicalRivalCatalogue;
import io.autoptu.cobblemon.authority.CanonicalRivalStateQueryService;
import io.autoptu.cobblemon.authority.FileCanonicalRivalStateRepository;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Path;

/** Read-only Minecraft surface for durable server-owned rival narrative state. */
public final class FabricRivalRuntime {
    private FabricRivalRuntime() {}

    public static void register() {
        FabricTrainerRecordRuntime.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("rival")
                                .then(CommandManager.argument("rivalId", StringArgumentType.word())
                                        .executes(context -> show(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "rivalId")))))));
    }

    private static int show(ServerCommandSource source, String rivalId) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("AutoPTU rival state must be requested by an authenticated player."));
            return 0;
        }
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        if (FabricCanonicalPlayerStoreRuntime.requireRepository(player.getServer()).findPlayer(playerId).isEmpty()) {
            source.sendError(Text.literal("Canonical Trainer state is not loaded."));
            return 0;
        }
        try {
            var snapshot = new CanonicalRivalStateQueryService(
                    CanonicalRivalCatalogue.DEFAULT,
                    new FileCanonicalRivalStateRepository(canonicalStateRoot(player))
            ).inspect(playerId, rivalId);
            player.sendMessage(Text.literal(snapshot.displayName()
                    + " — history " + snapshot.historyEventKeys().size()
                    + " — story flags " + snapshot.storyFlags().size()
                    + " — revision " + snapshot.revision()), false);
            return 1;
        } catch (IllegalArgumentException error) {
            source.sendError(Text.literal("Unknown server-authored rival: " + rivalId));
            return 0;
        }
    }

    private static Path canonicalStateRoot(ServerPlayerEntity player) {
        return player.getServer().getSavePath(WorldSavePath.ROOT)
                .resolve("autoptu")
                .resolve("canonical-state")
                .normalize();
    }
}
