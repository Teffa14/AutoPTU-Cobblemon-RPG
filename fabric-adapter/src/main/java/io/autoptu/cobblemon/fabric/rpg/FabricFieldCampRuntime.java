package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalPlayerState;
import io.autoptu.cobblemon.authority.WorldTaskCatalogue;
import io.autoptu.cobblemon.authority.WorldTaskCompetenceService;
import io.autoptu.cobblemon.authority.WorldTaskDefinition;
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

/** Physical Ouros field-camp surface backed only by canonical Trainer capability state. */
public final class FabricFieldCampRuntime {
    private static final double MAX_INTERACTION_DISTANCE_SQUARED = 25.0D;
    private static final WorldTaskCatalogue CATALOGUE = new WorldTaskCatalogue();
    private static final WorldTaskCompetenceService COMPETENCE = new WorldTaskCompetenceService();

    private FabricFieldCampRuntime() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient()
                    || hand != Hand.MAIN_HAND
                    || !(player instanceof ServerPlayerEntity serverPlayer)
                    || serverPlayer.isSneaking()) {
                return ActionResult.PASS;
            }

            BlockPos campfire = hitResult.getBlockPos();
            if (!isFieldCamp(world, campfire)) return ActionResult.PASS;
            if (!withinInteractionDistance(serverPlayer, campfire)) {
                serverPlayer.sendMessage(Text.literal("You are too far away to assess this AutoPTU field camp."), false);
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

            WorldTaskDefinition task = CATALOGUE.find(WorldTaskCatalogue.FIELD_CAMP_SETUP).orElseThrow();
            WorldTaskCompetenceService.Assessment assessment = COMPETENCE.assess(canonicalPlayer, task);
            serverPlayer.sendMessage(Text.literal(assessmentMessage(assessment)), false);
            serverPlayer.sendMessage(Text.literal(
                    "Assessment only: no PTU action cost, frequency, roll, healing, reward, encounter result, or battle state was created."), false);
            return ActionResult.SUCCESS;
        });
    }

    static String assessmentMessage(WorldTaskCompetenceService.Assessment assessment) {
        WorldTaskDefinition.QualityDistribution distribution = assessment.distribution();
        return assessment.displayName()
                + " | " + assessment.canonicalSkillId() + " rank " + assessment.canonicalSkillRank()
                + " | quality readiness: improvised " + distribution.improvisedPercent() + "%"
                + ", standard " + distribution.standardPercent() + "%"
                + ", excellent " + distribution.excellentPercent() + "%";
    }

    static boolean isFieldCamp(World world, BlockPos campfire) {
        return world.getBlockState(campfire).isOf(Blocks.CAMPFIRE)
                && world.getBlockState(campfire.down()).isOf(Blocks.BARREL);
    }

    static boolean withinInteractionDistance(ServerPlayerEntity player, BlockPos campfire) {
        double x = campfire.getX() + 0.5D;
        double y = campfire.getY() + 0.5D;
        double z = campfire.getZ() + 0.5D;
        return player.squaredDistanceTo(x, y, z) <= MAX_INTERACTION_DISTANCE_SQUARED;
    }
}
