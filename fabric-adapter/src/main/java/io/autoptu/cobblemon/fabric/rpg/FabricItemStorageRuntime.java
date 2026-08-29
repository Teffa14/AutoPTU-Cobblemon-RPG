package io.autoptu.cobblemon.fabric.rpg;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.authority.CanonicalItemStorageState;
import io.autoptu.cobblemon.authority.CanonicalItemStorageTransferService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

/** Player fallback for server-owned bag <-> storage transfers. */
public final class FabricItemStorageRuntime {
    private FabricItemStorageRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("storage")
                                .executes(context -> show(context.getSource()))
                                .then(CommandManager.literal("deposit")
                                        .then(CommandManager.argument("item", StringArgumentType.word())
                                                .executes(context -> deposit(context.getSource(),
                                                        StringArgumentType.getString(context, "item"), 1))
                                                .then(CommandManager.argument("quantity", IntegerArgumentType.integer(1))
                                                        .executes(context -> deposit(context.getSource(),
                                                                StringArgumentType.getString(context, "item"),
                                                                IntegerArgumentType.getInteger(context, "quantity"))))))
                                .then(CommandManager.literal("withdraw")
                                        .then(CommandManager.argument("item", StringArgumentType.word())
                                                .executes(context -> withdraw(context.getSource(),
                                                        StringArgumentType.getString(context, "item"), 1))
                                                .then(CommandManager.argument("quantity", IntegerArgumentType.integer(1))
                                                        .executes(context -> withdraw(context.getSource(),
                                                                StringArgumentType.getString(context, "item"),
                                                                IntegerArgumentType.getInteger(context, "quantity"))))))))));
    }

    private static int show(ServerCommandSource source) {
        ServerPlayerEntity player = requireCanonicalPlayer(source);
        if (player == null) return 0;
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        CanonicalItemStorageState state = service(player).inspect(playerId);
        player.sendMessage(Text.literal("AutoPTU item storage | revision " + state.revision()), false);
        if (state.quantities().isEmpty()) {
            player.sendMessage(Text.literal("Storage is empty."), false);
        } else {
            state.quantities().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey())
                    .forEach(entry -> player.sendMessage(Text.literal(entry.getKey() + " x" + entry.getValue()), false));
        }
        return 1;
    }

    private static int deposit(ServerCommandSource source, String itemKey, int quantity) {
        ServerPlayerEntity player = requireCanonicalPlayer(source);
        if (player == null) return 0;
        try {
            String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
            CanonicalItemStorageTransferService.TransferResult result = service(player).deposit(
                    "minecraft-storage-" + UUID.randomUUID(), playerId, itemKey, quantity);
            player.sendMessage(Text.literal("Stored " + result.templateId() + " x" + result.quantity()
                    + " | storage revision " + result.storageRevision()), false);
            return 1;
        } catch (IllegalArgumentException | IllegalStateException error) {
            source.sendError(Text.literal(error.getMessage()));
            return 0;
        }
    }

    private static int withdraw(ServerCommandSource source, String templateId, int quantity) {
        ServerPlayerEntity player = requireCanonicalPlayer(source);
        if (player == null) return 0;
        try {
            String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
            CanonicalItemStorageTransferService.TransferResult result = service(player).withdraw(
                    "minecraft-storage-" + UUID.randomUUID(), playerId, templateId, quantity);
            player.sendMessage(Text.literal("Withdrew " + result.templateId() + " x" + result.quantity()
                    + " | bag stack " + result.bagItemInstanceId()), false);
            return 1;
        } catch (IllegalArgumentException | IllegalStateException error) {
            source.sendError(Text.literal(error.getMessage()));
            return 0;
        }
    }

    private static ServerPlayerEntity requireCanonicalPlayer(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("AutoPTU storage must be requested by an authenticated player."));
            return null;
        }
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        if (FabricCanonicalPlayerStoreRuntime.requireRepository(player.getServer()).findPlayer(playerId).isEmpty()) {
            source.sendError(Text.literal("Canonical Trainer state is not loaded."));
            return null;
        }
        return player;
    }

    private static CanonicalItemStorageTransferService service(ServerPlayerEntity player) {
        return new CanonicalItemStorageTransferService(
                FabricCanonicalPlayerStoreRuntime.requireAssetRepository(player.getServer()),
                FabricCanonicalPlayerStoreRuntime.requireItemStorageRepository(player.getServer()),
                FabricCanonicalPlayerStoreRuntime.requireItemStorageTransferRepository(player.getServer()));
    }
}
