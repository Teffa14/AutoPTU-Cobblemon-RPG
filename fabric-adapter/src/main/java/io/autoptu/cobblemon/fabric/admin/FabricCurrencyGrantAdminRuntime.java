package io.autoptu.cobblemon.fabric.admin;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.fabric.rpg.FabricCurrencyGrantService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Operator-only command surface for the reusable canonical currency grant service. */
public final class FabricCurrencyGrantAdminRuntime implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("admin")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.literal("grant")
                                        .then(CommandManager.literal("currency")
                                                .then(CommandManager.argument("player", StringArgumentType.word())
                                                        .then(CommandManager.argument(
                                                                        "amount",
                                                                        LongArgumentType.longArg(1L, FabricCurrencyGrantService.MAX_GRANT))
                                                                .executes(context -> grant(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(context, "player"),
                                                                        LongArgumentType.getLong(context, "amount"))))))))));
    }

    private static int grant(ServerCommandSource source, String playerName, long amount) {
        ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(playerName);
        if (target == null) {
            source.sendError(Text.literal("That Minecraft player must be online for canonical identity resolution."));
            return 0;
        }

        FabricCurrencyGrantService.GrantResult result;
        try {
            result = FabricCurrencyGrantService.grant(source.getServer(), target, amount);
        } catch (IllegalArgumentException invalidRequest) {
            source.sendError(Text.literal("Canonical currency grant rejected the requested amount."));
            return 0;
        } catch (RuntimeException failed) {
            source.sendError(Text.literal("Canonical currency grant could not be committed safely; no Minecraft balance fallback was used."));
            return 0;
        }

        if (result.status() == FabricCurrencyGrantService.GrantResult.Status.NO_CANONICAL_TRAINER) {
            source.sendError(Text.literal("No canonical AutoPTU Trainer state exists for " + target.getGameProfile().getName() + "."));
            return 0;
        }
        if (result.status() != FabricCurrencyGrantService.GrantResult.Status.COMMITTED) {
            source.sendError(Text.literal("Canonical currency grant did not commit: " + result.transactionStatus()));
            return 0;
        }

        source.sendFeedback(() -> Text.literal(
                "Granted " + amount + " " + result.currencyId()
                        + " to " + target.getGameProfile().getName()
                        + " | balance " + result.balance()
                        + " | wallet revision " + result.revision()
                        + " | transaction " + result.transactionId()), true);
        return 1;
    }
}
