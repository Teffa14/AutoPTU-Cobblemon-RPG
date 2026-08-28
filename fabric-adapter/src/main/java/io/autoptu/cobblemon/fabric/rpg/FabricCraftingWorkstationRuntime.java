package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalPlayerState;
import io.autoptu.cobblemon.authority.WorldTaskCatalogue;
import io.autoptu.cobblemon.authority.WorldTaskCompetenceService;
import io.autoptu.cobblemon.authority.WorldTaskCraftMaterialAssessmentService;
import io.autoptu.cobblemon.authority.WorldTaskRecipeDefinition;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;
import java.util.stream.Collectors;

/** Normal-world crafting workstation backed by canonical Trainer, item and craft-attempt state. */
public final class FabricCraftingWorkstationRuntime {
    private static final double MAX_INTERACTION_DISTANCE_SQUARED = 25.0D;
    private static final String WORKSTATION_ID = WorldTaskCatalogue.GENERAL_CRAFTING_WORKSTATION;
    private static final WorldTaskCatalogue CATALOGUE = new WorldTaskCatalogue();
    private static final WorldTaskCompetenceService COMPETENCE = new WorldTaskCompetenceService();

    private FabricCraftingWorkstationRuntime() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient()
                    || hand != Hand.MAIN_HAND
                    || !(player instanceof ServerPlayerEntity serverPlayer)
                    || serverPlayer.isSneaking()) {
                return ActionResult.PASS;
            }

            BlockPos stationHead = hitResult.getBlockPos();
            if (!isCraftingWorkstation(world, stationHead)) return ActionResult.PASS;
            if (!withinInteractionDistance(serverPlayer, stationHead)) {
                serverPlayer.sendMessage(Text.literal("You are too far away to use this AutoPTU crafting workstation."), false);
                return ActionResult.FAIL;
            }

            String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(serverPlayer.getUuid());
            CanonicalPlayerState canonicalPlayer = FabricCanonicalPlayerStoreRuntime
                    .requireRepository(serverPlayer.getServer())
                    .findPlayer(playerId)
                    .orElse(null);
            if (canonicalPlayer == null) {
                serverPlayer.sendMessage(Text.literal("Canonical Trainer state is unavailable for this player."), false);
                return ActionResult.FAIL;
            }

            WorldTaskCraftMaterialAssessmentService materialAssessment =
                    new WorldTaskCraftMaterialAssessmentService(
                            FabricCanonicalPlayerStoreRuntime.requireAssetRepository(serverPlayer.getServer()));
            List<RecipeOption> options = recipeOptions(canonicalPlayer, playerId, materialAssessment);
            if (options.isEmpty()) {
                serverPlayer.sendMessage(Text.literal(
                        "This Trainer does not currently understand any recipe authored for this workstation."), false);
                return ActionResult.FAIL;
            }

            serverPlayer.sendMessage(Text.literal("AutoPTU crafting workstation — choose a recipe:"), false);
            for (RecipeOption option : options) {
                WorldTaskRecipeDefinition recipe = option.recipe();
                Text line = Text.literal(recipe.task().displayName() + " — ");
                if (option.materials().ready()) {
                    line = line.copy().append(Text.literal("[CRAFT]").styled(style -> style.withClickEvent(
                            new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/autoptu craft " + recipe.taskId())
                    )));
                } else {
                    line = line.copy().append(Text.literal("missing " + missingMaterials(option.materials())));
                }
                serverPlayer.sendMessage(line, false);
            }
            serverPlayer.sendMessage(Text.literal(
                    "Sneak-use while holding an authored ingredient to deposit one item."), false);
            return ActionResult.SUCCESS;
        });
    }

    static List<RecipeOption> recipeOptions(
            CanonicalPlayerState player,
            String playerId,
            WorldTaskCraftMaterialAssessmentService materialAssessment
    ) {
        return CATALOGUE.allRecipes().stream()
                .filter(recipe -> WORKSTATION_ID.equals(recipe.workstationId()))
                .filter(recipe -> COMPETENCE.assess(player, recipe.task()).understood())
                .map(recipe -> new RecipeOption(recipe, materialAssessment.assess(playerId, recipe, 1)))
                .toList();
    }

    static String missingMaterials(WorldTaskCraftMaterialAssessmentService.Assessment assessment) {
        return assessment.ingredients().stream()
                .filter(ingredient -> ingredient.missing() > 0)
                .map(ingredient -> ingredient.missing() + "x " + ingredient.itemTemplateId())
                .collect(Collectors.joining(", "));
    }

    static boolean isCraftingWorkstation(World world, BlockPos head) {
        return world.getBlockState(head).isOf(Blocks.SMITHING_TABLE)
                && world.getBlockState(head.down()).isOf(Blocks.CRAFTING_TABLE)
                && world.getBlockState(head.down(2)).isOf(Blocks.BARREL);
    }

    static boolean withinInteractionDistance(ServerPlayerEntity player, BlockPos stationHead) {
        double x = stationHead.getX() + 0.5D;
        double y = stationHead.getY() + 0.5D;
        double z = stationHead.getZ() + 0.5D;
        return player.squaredDistanceTo(x, y, z) <= MAX_INTERACTION_DISTANCE_SQUARED;
    }

    record RecipeOption(
            WorldTaskRecipeDefinition recipe,
            WorldTaskCraftMaterialAssessmentService.Assessment materials
    ) {}
}
