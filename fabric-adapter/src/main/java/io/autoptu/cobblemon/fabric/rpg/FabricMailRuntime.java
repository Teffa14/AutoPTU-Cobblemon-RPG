package io.autoptu.cobblemon.fabric.rpg;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.authority.CanonicalMailCatalogue;
import io.autoptu.cobblemon.authority.CanonicalMailService;
import io.autoptu.cobblemon.authority.FileCanonicalMailRepository;
import io.autoptu.cobblemon.authority.FileCanonicalWalletRepository;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Path;

/** Authenticated Minecraft fallback surface for durable server-authored RPG mail. */
public final class FabricMailRuntime {
    private FabricMailRuntime() { }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("mail")
                                .executes(context -> inspect(context.getSource()))
                                .then(CommandManager.literal("read")
                                        .then(CommandManager.argument("mailId", StringArgumentType.word())
                                                .executes(context -> read(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "mailId")))))
                                .then(CommandManager.literal("claim")
                                        .then(CommandManager.argument("mailId", StringArgumentType.word())
                                                .executes(context -> claim(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "mailId"))))))));
    }

    private static int inspect(ServerCommandSource source) {
        ServerPlayerEntity player = requirePlayer(source);
        if (player == null) return 0;
        var inbox = service(player).inspect(playerId(player));
        player.sendMessage(Text.literal("Mail — " + inbox.messages().size() + " message(s) — revision " + inbox.revision()), false);
        for (var message : inbox.messages()) {
            String state = message.read() ? "read" : "unread";
            String reward = message.hasReward()
                    ? (message.rewardClaimed() ? "reward claimed" : "reward available")
                    : "no reward";
            player.sendMessage(Text.literal("  " + message.mailId() + " — " + message.subject() + " — " + state + " — " + reward), false);
        }
        return 1;
    }

    private static int read(ServerCommandSource source, String mailId) {
        ServerPlayerEntity player = requirePlayer(source);
        if (player == null) return 0;
        try {
            var result = service(player).read(playerId(player), mailId);
            var message = result.message();
            player.sendMessage(Text.literal(message.subject() + " — from " + message.sender()), false);
            player.sendMessage(Text.literal(message.body()), false);
            if (message.hasReward() && !message.rewardClaimed()) {
                player.sendMessage(Text.literal("Reward available: " + message.rewardAmount() + " " + message.rewardCurrencyId()
                        + ". Claim with /autoptu mail claim " + message.mailId()), false);
            }
            return 1;
        } catch (IllegalArgumentException | IllegalStateException error) {
            source.sendError(Text.literal(error.getMessage()));
            return 0;
        }
    }

    private static int claim(ServerCommandSource source, String mailId) {
        ServerPlayerEntity player = requirePlayer(source);
        if (player == null) return 0;
        try {
            var result = service(player).claimReward(playerId(player), mailId);
            switch (result.status()) {
                case APPLIED, RECOVERED_AFTER_WALLET_COMMIT -> player.sendMessage(Text.literal(
                        "Mail reward claimed: " + result.message().rewardAmount() + " " + result.message().rewardCurrencyId()), false);
                case ALREADY_CLAIMED -> player.sendMessage(Text.literal("Mail reward already claimed."), false);
                case NO_REWARD -> player.sendMessage(Text.literal("This mail has no reward."), false);
                case TRANSACTION_CONFLICT, RETRY_EXHAUSTED -> source.sendError(Text.literal(
                        "Mail reward could not be committed safely: " + result.status()));
            }
            return result.committed() || result.status() == CanonicalMailService.ClaimStatus.NO_REWARD ? 1 : 0;
        } catch (IllegalArgumentException | IllegalStateException error) {
            source.sendError(Text.literal(error.getMessage()));
            return 0;
        }
    }

    private static ServerPlayerEntity requirePlayer(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("AutoPTU mail must be requested by an authenticated player."));
            return null;
        }
        if (FabricCanonicalPlayerStoreRuntime.requireRepository(player.getServer()).findPlayer(playerId(player)).isEmpty()) {
            source.sendError(Text.literal("Canonical Trainer state is not loaded."));
            return null;
        }
        return player;
    }

    private static CanonicalMailService service(ServerPlayerEntity player) {
        Path root = canonicalStateRoot(player);
        return new CanonicalMailService(
                CanonicalMailCatalogue.DEFAULT,
                new FileCanonicalMailRepository(root),
                new FileCanonicalWalletRepository(root));
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
