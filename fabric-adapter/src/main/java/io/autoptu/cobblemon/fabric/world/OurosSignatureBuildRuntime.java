package io.autoptu.cobblemon.fabric.world;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/** Registers manually-invoked large Ouros builds for visual and traversal review. */
public final class OurosSignatureBuildRuntime {
    private OurosSignatureBuildRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("ouros")
                        .then(CommandManager.literal("build")
                                .then(CommandManager.literal("meridian_canopy_gym")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(context -> buildCanopyGym(context.getSource())))
                                .then(CommandManager.literal("grand_palace")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(context -> buildGrandPalace(context.getSource()))))));
    }

    private static int buildCanopyGym(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("The Meridian Canopy Gym prototype must be placed by a player."));
            return 0;
        }

        ServerWorld world = player.getServerWorld();
        BlockPos origin = player.getBlockPos().add(0, -1, 46);
        MeridianCanopyGymRebuild.BuildResult result = MeridianCanopyGymRebuild.build(world, origin);
        MeridianCanopyGymRebuildStructuralPass.apply(world, origin);
        MeridianCanopyGymRebuildDetailPass.apply(world, origin);
        MeridianCanopyGymRebuildAnchoringPass.apply(world, origin);
        MeridianCanopyGymVerticalLandmarkPass.apply(world, origin);

        player.sendMessage(Text.literal("Meridian Canopy Gym VERTICAL LANDMARK REBUILD v2 built 46 blocks ahead."), false);
        player.sendMessage(Text.literal(
                "Footprint " + result.width() + "x" + result.depth() + ", authored height envelope 46. The vertical limit is intentionally unlocked for silhouette review."), false);
        player.sendMessage(Text.literal(
                "Review the monumental gatehouse, high conservatory lantern, supported arena canopy ribs, distinct challenge wings, backstage service bar, custom trees, joinery, drainage and human-scale props."), false);
        player.sendMessage(Text.literal(
                "OI-106 remains under visual review. Exact browser geometry is the acceptance artifact; disconnected floating components or geometry outside the expanded capture envelope fail CI."), false);
        return 1;
    }

    private static int buildGrandPalace(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("The Ouros Grand Palace must be placed by a player."));
            return 0;
        }

        ServerWorld world = player.getServerWorld();
        BlockPos origin = player.getBlockPos().add(0, -1, 74);
        OurosGrandPalace.BuildResult result = OurosGrandPalace.build(world, origin);

        player.sendMessage(Text.literal("Ouros Grand Palace OI-107 built 74 blocks ahead."), false);
        player.sendMessage(Text.literal(
                "Footprint " + result.width() + "x" + result.depth() + ", capture height " + result.height()
                        + ", authored spaces " + result.authoredSpaces() + "."), false);
        player.sendMessage(Text.literal(
                "The palace includes the 19-space reference program: four double-height ceremonial halls, side salons/cabinets, two upper galleries, a glazed mansard roof and seven distinct upper rooms."), false);
        player.sendMessage(Text.literal(
                "OI-107 is a SIGNATURE STRUCTURE candidate under docs/ouros-build-doctrine.md. Exact live-server BlockState export and browser visual review remain mandatory."), false);
        return 1;
    }
}