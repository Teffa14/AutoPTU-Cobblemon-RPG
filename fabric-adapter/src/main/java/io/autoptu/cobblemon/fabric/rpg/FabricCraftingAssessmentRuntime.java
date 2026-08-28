package io.autoptu.cobblemon.fabric.rpg;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.authority.CanonicalPlayerState;
import io.autoptu.cobblemon.authority.WorldTaskCatalogue;
import io.autoptu.cobblemon.authority.WorldTaskCompetenceService;
import io.autoptu.cobblemon.authority.WorldTaskCraftMaterialAssessmentService;
import io.autoptu.cobblemon.authority.WorldTaskDefinition;
import io.autoptu.cobblemon.authority.WorldTaskRecipeDefinition;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.stream.Collectors;

/** Minecraft-facing capability and canonical-material crafting assessment. */
public final class FabricCraftingAssessmentRuntime {
    private static final WorldTaskCatalogue CATALOGUE = new WorldTaskCatalogue();
    private static final WorldTaskCompetenceService COMPETENCE = new WorldTaskCompetenceService();

    private FabricCraftingAssessmentRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("cancraft")
                                .then(CommandManager.argument("recipe", StringArgumentType.word())
                                        .executes(context -> assess(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "recipe")
                                        ))))));
    }

    private static int assess(ServerCommandSource source, String requestedRecipe) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Crafting assessment must be requested by an authenticated player."));
            return 0;
        }

        WorldTaskRecipeDefinition recipe = CATALOGUE.findRecipe(requestedRecipe).orElse(null);
        if (recipe == null) {
            String known = CATALOGUE.allRecipes().stream()
                    .map(WorldTaskRecipeDefinition::taskId)
                    .collect(Collectors.joining(", "));
            source.sendError(Text.literal("Unknown AutoPTU recipe. Available: " + known));
            return 0;
        }
        WorldTaskDefinition task = recipe.task();

        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        CanonicalPlayerState canonicalPlayer = FabricCanonicalPlayerStoreRuntime
                .requireRepository(player.getServer())
                .findPlayer(playerId)
                .orElse(null);
        if (canonicalPlayer == null) {
            source.sendError(Text.literal("Canonical Trainer state is unavailable for this player."));
            return 0;
        }

        WorldTaskCompetenceService.Assessment assessment = COMPETENCE.assess(canonicalPlayer, task);
        if (!assessment.understood()) {
            source.sendError(Text.literal(task.displayName() + " cannot be attempted yet. " + assessment.detail()));
            return 0;
        }

        WorldTaskCraftMaterialAssessmentService.Assessment materials =
                new WorldTaskCraftMaterialAssessmentService(
                        FabricCanonicalPlayerStoreRuntime.requireAssetRepository(player.getServer()))
                        .assess(playerId, recipe, 1);
        WorldTaskDefinition.QualityDistribution distribution = assessment.distribution();
        player.sendMessage(Text.literal(
                task.displayName()
                        + " | " + assessment.canonicalSkillId() + " rank " + assessment.canonicalSkillRank()
                        + " | workstation " + recipe.workstationId()
                        + " | canonical materials " + materialSummary(materials)
                        + " | ready=" + materials.ready()
                        + " | quality chances: improvised " + distribution.improvisedPercent() + "%"
                        + ", standard " + distribution.standardPercent() + "%"
                        + ", excellent " + distribution.excellentPercent() + "%"), false);
        player.sendMessage(Text.literal(
                "Outputs: improvised=" + outputSummary(recipe, WorldTaskRecipeDefinition.CraftQuality.IMPROVISED)
                        + ", standard=" + outputSummary(recipe, WorldTaskRecipeDefinition.CraftQuality.STANDARD)
                        + ", excellent=" + outputSummary(recipe, WorldTaskRecipeDefinition.CraftQuality.EXCELLENT)
                        + ". Preview only: canonical inventory was read, but no reservation, roll, or consumption occurred."), false);
        return materials.ready() ? 1 : 0;
    }

    static String materialSummary(WorldTaskCraftMaterialAssessmentService.Assessment assessment) {
        return assessment.ingredients().stream()
                .map(ingredient -> ingredient.available() + "/" + ingredient.required() + " "
                        + ingredient.itemTemplateId()
                        + (ingredient.missing() == 0 ? "" : " (missing " + ingredient.missing() + ")"))
                .collect(Collectors.joining(", "));
    }

    static String outputSummary(
            WorldTaskRecipeDefinition recipe,
            WorldTaskRecipeDefinition.CraftQuality quality
    ) {
        WorldTaskRecipeDefinition.CraftOutput output = recipe.outputFor(quality);
        return output.quantity() + "x " + output.itemTemplateId();
    }
}
