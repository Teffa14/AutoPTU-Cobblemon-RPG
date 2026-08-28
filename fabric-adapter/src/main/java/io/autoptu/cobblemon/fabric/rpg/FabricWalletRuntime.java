package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalWalletQueryService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

/** Read-only wallet fallback backed exclusively by durable canonical server state. */
public final class FabricWalletRuntime {
    private FabricWalletRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("money")
                                .executes(context -> show(context.getSource())))));
    }

    private static int show(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("AutoPTU money must be requested by an authenticated player."));
            return 0;
        }
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        if (FabricCanonicalPlayerStoreRuntime.requireRepository(player.getServer()).findPlayer(playerId).isEmpty()) {
            source.sendError(Text.literal("Canonical Trainer state is not loaded."));
            return 0;
        }

        CanonicalWalletQueryService.WalletSnapshot wallet = new CanonicalWalletQueryService(
                FabricCanonicalPlayerStoreRuntime.requireWalletRepository(player.getServer())).inspect(playerId);
        for (String line : formatLines(wallet)) player.sendMessage(Text.literal(line), false);
        return 1;
    }

    static List<String> formatLines(CanonicalWalletQueryService.WalletSnapshot wallet) {
        return List.of(
                "AutoPTU money",
                wallet.currencyId() + ": " + wallet.balance(),
                "Wallet revision: " + wallet.revision()
        );
    }
}
