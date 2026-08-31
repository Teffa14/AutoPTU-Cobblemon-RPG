package io.autoptu.cobblemon.fabric.rpg;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.authority.CanonicalFactionCatalogue;
import io.autoptu.cobblemon.authority.CanonicalFactionReputationQueryService;
import io.autoptu.cobblemon.authority.FileCanonicalFactionReputationRepository;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Path;

/** Read-only Minecraft surface for server-owned RPG faction reputation. */
public final class FabricFactionReputationRuntime {
    private FabricFactionReputationRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("faction")
                                .then(CommandManager.literal("reputation")
                                        .then(CommandManager.argument("factionId", StringArgumentType.word())
                                                .executes(context -> show(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "factionId"))))))));
    }

    private static int show(ServerCommandSource source, String factionId) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("AutoPTU faction reputation must be requested by an authenticated player."));
            return 0;
        }
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        if (FabricCanonicalPlayerStoreRuntime.requireRepository(player.getServer()).findPlayer(playerId).isEmpty()) {
            source.sendError(Text.literal("Canonical Trainer state is not loaded."));
            return 0;
        }
        try {
            var snapshot = new CanonicalFactionReputationQueryService(
                    CanonicalFactionCatalogue.DEFAULT,
                    new FileCanonicalFactionReputationRepository(canonicalStateRoot(player))
            ).inspect(playerId, factionId);
            player.sendMessage(Text.literal(snapshot.displayName() + " reputation — " + snapshot.reputation()
                    + " — revision " + snapshot.revision()), false);
            return 1;
        } catch (IllegalArgumentException error) {
            source.sendError(Text.literal("Unknown server-authored faction: " + factionId));
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
