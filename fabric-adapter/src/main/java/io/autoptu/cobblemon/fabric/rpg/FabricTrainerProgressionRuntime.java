package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalTrainerProgressionQueryService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Read-only fallback for durable server-owned Trainer progression. */
public final class FabricTrainerProgressionRuntime {
    private FabricTrainerProgressionRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("trainer")
                                .then(CommandManager.literal("progression")
                                        .executes(context -> show(context.getSource()))))));
    }

    private static int show(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("AutoPTU Trainer progression must be requested by an authenticated player."));
            return 0;
        }
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        if (FabricCanonicalPlayerStoreRuntime.requireRepository(player.getServer()).findPlayer(playerId).isEmpty()) {
            source.sendError(Text.literal("Canonical Trainer state is not loaded."));
            return 0;
        }
        var snapshot = new CanonicalTrainerProgressionQueryService(
                FabricCanonicalPlayerStoreRuntime.requireTrainerProgressionRepository(player.getServer())).inspect(playerId);
        player.sendMessage(Text.literal("Trainer progression — Level " + snapshot.trainerLevel()
                + " — XP " + snapshot.trainerXp() + " — revision " + snapshot.revision()), false);
        return 1;
    }
}
