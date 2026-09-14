package io.autoptu.cobblemon.fabric.admin;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.fabric.rpg.FabricItemGrantService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Operator-only grant of server-authored canonical item templates. */
public final class FabricItemGrantAdminRuntime implements ModInitializer {
    private static final FabricItemGrantService SERVICE = new FabricItemGrantService();

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var item = CommandManager.literal("item")
                    .then(CommandManager.argument("player", StringArgumentType.word())
                            .then(CommandManager.argument("item", StringArgumentType.word())
                                    .executes(context -> grant(
                                            context.getSource(),
                                            StringArgumentType.getString(context, "player"),
                                            StringArgumentType.getString(context, "item"),
                                            1))
                                    .then(CommandManager.argument("qty", IntegerArgumentType.integer(1, FabricItemGrantService.MAX_GRANT_QUANTITY))
                                            .executes(context -> grant(
                                                    context.getSource(),
                                                    StringArgumentType.getString(context, "player"),
                                                    StringArgumentType.getString(context, "item"),
                                                    IntegerArgumentType.getInteger(context, "qty"))))));
            var grant = CommandManager.literal("grant").then(item);
            var admin = CommandManager.literal("admin")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(grant);
            dispatcher.register(CommandManager.literal("autoptu").then(admin));
        });
    }

    private static int grant(ServerCommandSource source, String playerName, String itemTemplateId, int quantity) {
        ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(playerName);
        if (target == null) {
            source.sendError(Text.literal("That Minecraft player must be online for canonical identity resolution."));
            return 0;
        }

        FabricItemGrantService.Result result = SERVICE.grant(target, itemTemplateId, quantity);
        return switch (result.outcome()) {
            case GRANTED -> {
                source.sendFeedback(() -> Text.literal("Granted " + result.quantity() + " " + result.templateId()
                        + " to " + target.getGameProfile().getName()
                        + " | canonical instance " + result.itemInstanceId()), true);
                target.sendMessage(Text.literal("Received " + result.quantity() + " " + result.templateId()
                        + " in persistent AutoPTU inventory."), false);
                yield 1;
            }
            case INVALID_ITEM -> {
                source.sendError(Text.literal("Unknown server-authored canonical item template."));
                yield 0;
            }
            case INVALID_QUANTITY -> {
                source.sendError(Text.literal("Grant quantity must be between 1 and " + FabricItemGrantService.MAX_GRANT_QUANTITY + "."));
                yield 0;
            }
            case MISSING_TRAINER -> {
                source.sendError(Text.literal("No canonical AutoPTU Trainer state exists for "
                        + target.getGameProfile().getName() + "."));
                yield 0;
            }
            case CONFLICT, REJECTED -> {
                source.sendError(Text.literal("Canonical item grant could not be committed safely; no Minecraft inventory fallback was used."));
                yield 0;
            }
        };
    }
}
