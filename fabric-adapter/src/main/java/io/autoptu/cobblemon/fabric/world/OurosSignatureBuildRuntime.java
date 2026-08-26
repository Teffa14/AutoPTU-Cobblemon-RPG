package io.autoptu.cobblemon.fabric.world;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/** Registers manually-invoked large Ouros build prototypes for visual and traversal review. */
public final class OurosSignatureBuildRuntime {
    private OurosSignatureBuildRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("ouros")
                        .then(CommandManager.literal("build")
                                .then(CommandManager.literal("meridian_canopy_gym")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(context -> buildCanopyGym(context.getSource()))))));
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

        player.sendMessage(Text.literal("Meridian Canopy Gym ZERO-BASE REBUILD v1 built 46 blocks ahead."), false);
        player.sendMessage(Text.literal(
                "Footprint " + result.width() + "x" + result.depth() + ", height envelope " + result.height()
                        + ". The legacy box/detail-pass stack is no longer used by this command."), false);
        player.sendMessage(Text.literal(
                "Review the monumental gatehouse, supported compound roofs, conservatory vault, staggered botanical wing, hydro sawtooth wing, elliptical battle sanctum, backstage service bar, custom trees, joinery, drainage and human-scale props."), false);
        player.sendMessage(Text.literal(
                "This remains under OI-106 visual review. Exact browser geometry is the acceptance artifact; disconnected floating components fail CI."), false);
        return 1;
    }
}
