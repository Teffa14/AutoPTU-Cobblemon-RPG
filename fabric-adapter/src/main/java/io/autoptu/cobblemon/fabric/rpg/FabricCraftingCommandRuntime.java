package io.autoptu.cobblemon.fabric.rpg;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.authority.CanonicalPlayerState;
import io.autoptu.cobblemon.authority.WorldTaskCatalogue;
import io.autoptu.cobblemon.authority.WorldTaskCraftService;
import io.autoptu.cobblemon.authority.WorldTaskRecipeDefinition;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;
import java.util.stream.Collectors;

/** Player fallback command for the same canonical crafting transaction used by the workstation. */
public final class FabricCraftingCommandRuntime {
    private static final WorldTaskCatalogue CATALOGUE = new WorldTaskCatalogue();
    private static final int MAX_BATCH_QUANTITY = 64;

    private FabricCraftingCommandRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("craft")
                                .then(CommandManager.argument("recipe", StringArgumentType.word())
                                        .executes(context -> craft(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "recipe"),
                                                1
                                        ))
                                        .then(CommandManager.argument("quantity", IntegerArgumentType.integer(1, MAX_BATCH_QUANTITY))
                                                .executes(context -> craft(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "recipe"),
                                                        IntegerArgumentType.getInteger(context, "quantity")
                                                )))))));
    }

    private static int craft(ServerCommandSource source, String requestedRecipe, int quantity) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Crafting must be requested by an authenticated player."));
            return 0;
        }

        WorldTaskRecipeDefinition recipe = CATALOGUE.findRecipe(requestedRecipe).orElse(null);
        if (recipe == null) {
            source.sendError(Text.literal("Unknown AutoPTU recipe. Available: " + knownRecipes()));
            return 0;
        }

        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        CanonicalPlayerState canonicalPlayer = FabricCanonicalPlayerStoreRuntime
                .requireRepository(player.getServer())
                .findPlayer(playerId)
                .orElse(null);
        if (canonicalPlayer == null) {
            source.sendError(Text.literal("Canonical Trainer state is unavailable for this player."));
            return 0;
        }

        String attemptId = "minecraft-craft-command:" + player.getUuid() + ":" + UUID.randomUUID();
        WorldTaskCraftService.CraftResult result = new WorldTaskCraftService(
                FabricCanonicalPlayerStoreRuntime.requireAssetRepository(player.getServer()),
                FabricCanonicalPlayerStoreRuntime.requireCraftAttemptRepository(player.getServer())
        ).craft(attemptId, canonicalPlayer, recipe, quantity);

        if (!result.committed() || result.attempt() == null) {
            source.sendError(Text.literal(
                    "AutoPTU craft did not commit (" + result.status().name().toLowerCase() + "): "
                            + result.detail() + ". Attempt: " + attemptId));
            return 0;
        }

        player.sendMessage(Text.literal(
                "Crafted " + result.attempt().outputQuantity() + "x "
                        + result.attempt().outputTemplateId()
                        + " from recipe " + result.attempt().recipeId()
                        + " at " + result.attempt().quality().name().toLowerCase()
                        + " quality. Result is stored in canonical AutoPTU inventory."), false);
        if (result.status() == WorldTaskCraftService.Status.COMMITTED_CLEANUP_PENDING) {
            player.sendMessage(Text.literal(
                    "Craft committed safely; reservation cleanup remains pending for attempt "
                            + attemptId + "."), false);
        }
        return 1;
    }

    static String knownRecipes() {
        return CATALOGUE.allRecipes().stream()
                .map(WorldTaskRecipeDefinition::taskId)
                .collect(Collectors.joining(", "));
    }
}
