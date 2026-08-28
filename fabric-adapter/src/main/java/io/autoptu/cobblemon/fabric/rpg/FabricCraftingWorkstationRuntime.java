package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalPlayerState;
import io.autoptu.cobblemon.authority.WorldTaskCatalogue;
import io.autoptu.cobblemon.authority.WorldTaskCompetenceService;
import io.autoptu.cobblemon.authority.WorldTaskCraftMaterialAssessmentService;
import io.autoptu.cobblemon.authority.WorldTaskDefinition;
import io.autoptu.cobblemon.authority.WorldTaskRecipeDefinition;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Normal-world crafting workstation preview backed by canonical Trainer and item state. */
public final class FabricCraftingWorkstationRuntime {
    private static final double MAX_INTERACTION_DISTANCE_SQUARED = 25.0D;
    private static final String WORKSTATION_ID = WorldTaskCatalogue.GENERAL_CRAFTING_WORKSTATION;
    private static final WorldTaskCatalogue CATALOGUE = new WorldTaskCatalogue();
    private static final WorldTaskCompetenceService COMPETENCE = new WorldTaskCompetenceService();

    private FabricCraftingWorkstationRuntime() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient() || hand != Hand.MAIN_HAND || !(player instanceof ServerPlayerEntity serverPlayer)) {
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

            serverPlayer.sendMessage(Text.literal("AutoPTU crafting workstation | " + WORKSTATION_ID), false);
            boolean anyUnderstood = false;
            for (WorldTaskRecipeDefinition recipe : CATALOGUE.allRecipes()) {
                if (!WORKSTATION_ID.equals(recipe.workstationId())) continue;
                WorldTaskDefinition task = recipe.task();
                WorldTaskCompetenceService.Assessment assessment = COMPETENCE.assess(canonicalPlayer, task);
                if (!assessment.understood()) {
                    serverPlayer.sendMessage(Text.literal(
                            "- " + task.taskId() + ": locked (" + assessment.detail() + ")"), false);
                    continue;
                }

                anyUnderstood = true;
                WorldTaskCraftMaterialAssessmentService.Assessment materials =
                        materialAssessment.assess(playerId, recipe, 1);
                WorldTaskDefinition.QualityDistribution distribution = assessment.distribution();
                serverPlayer.sendMessage(Text.literal(
                        "- " + task.taskId() + " | " + task.displayName()
                                + " | materials " + FabricCraftingAssessmentRuntime.materialSummary(materials)
                                + " | ready=" + materials.ready()
                                + " | " + assessment.canonicalSkillId() + " rank " + assessment.canonicalSkillRank()
                                + " | quality " + distribution.improvisedPercent() + "/"
                                + distribution.standardPercent() + "/" + distribution.excellentPercent() + "%"), false);
                serverPlayer.sendMessage(Text.literal(
                        "  outputs I/S/E: "
                                + FabricCraftingAssessmentRuntime.outputSummary(
                                        recipe, WorldTaskRecipeDefinition.CraftQuality.IMPROVISED)
                                + " | "
                                + FabricCraftingAssessmentRuntime.outputSummary(
                                        recipe, WorldTaskRecipeDefinition.CraftQuality.STANDARD)
                                + " | "
                                + FabricCraftingAssessmentRuntime.outputSummary(
                                        recipe, WorldTaskRecipeDefinition.CraftQuality.EXCELLENT)), false);
            }
            serverPlayer.sendMessage(Text.literal(
                    anyUnderstood
                            ? "Preview only. Canonical inventory was read; no reservation, craft roll, or material consumption occurred."
                            : "No authored recipes are currently understood by this Trainer."), false);
            return ActionResult.SUCCESS;
        });
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
}
