package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalTrainerRecordQueryService;
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

/** Read-only Minecraft surface for durable server-owned Trainer records. */
public final class FabricTrainerRecordRuntime {
    private FabricTrainerRecordRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("trainer")
                                .then(CommandManager.literal("records")
                                        .executes(context -> show(context.getSource()))))));
    }

    private static int show(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("AutoPTU Trainer records must be requested by an authenticated player."));
            return 0;
        }
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        if (FabricCanonicalPlayerStoreRuntime.requireRepository(player.getServer()).findPlayer(playerId).isEmpty()) {
            source.sendError(Text.literal("Canonical Trainer state is not loaded."));
            return 0;
        }
        var snapshot = new CanonicalTrainerRecordQueryService(
                new FileCanonicalTrainerRecordRepository(canonicalStateRoot(player))
        ).inspect(playerId);
        player.sendMessage(Text.literal("Trainer records — wins " + snapshot.wins()
                + " — losses " + snapshot.losses()
                + " — badges " + snapshot.badgeIds().size()
                + " — tournaments " + snapshot.tournamentRecordIds().size()
                + " — revision " + snapshot.revision()), false);
        return 1;
    }

    private static Path canonicalStateRoot(ServerPlayerEntity player) {
        return player.getServer().getSavePath(WorldSavePath.ROOT)
                .resolve("autoptu")
                .resolve("canonical-state")
                .normalize();
    }
}
