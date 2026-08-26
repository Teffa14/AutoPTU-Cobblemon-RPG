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
        MeridianCanopyGymBuilder.BuildResult result = MeridianCanopyGymBuilder.build(world, origin);
        MeridianCanopyGymDetailPass.apply(world, origin);

        player.sendMessage(Text.literal("Meridian Canopy Gym GAMEPLAY PROTOTYPE v2 built 46 blocks ahead."), false);
        player.sendMessage(Text.literal(
                "Footprint " + result.width() + "x" + result.depth() + ", height " + result.height()
                        + ". The v2 pass adds a civic forecourt, deep portico, clerestory, conservatory roof, authored wing facades, arena buttresses, roof garden pergola, service yard and night lighting."), false);
        player.sendMessage(Text.literal(
                "Inspect the approach silhouette, atrium section, both challenge wings, upper bridge, backstage shortcut, arena roof and rear service elevation."), false);
        player.sendMessage(Text.literal(
                "This build remains a GAMEPLAY PROTOTYPE until exact browser parity, screenshots and traversal playtest pass the Ouros quality bar."), false);
        return 1;
    }
}
