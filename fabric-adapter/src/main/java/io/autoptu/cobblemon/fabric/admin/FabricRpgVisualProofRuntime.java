package io.autoptu.cobblemon.fabric.admin;

import io.autoptu.cobblemon.fabric.rpg.FabricCedarServiceYardRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Operator-only camera hook for visually inspecting the normal server-provisioned RPG service yard.
 *
 * This command creates no RPG or PTU facts and no longer builds a synthetic showcase. It reuses the
 * production world provisioning path and only moves the QA player to a deterministic viewing point.
 */
public final class FabricRpgVisualProofRuntime {
    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-rpg-visual-proof");
    public static final String QA_COMMAND = "autoptuvisualproof";

    private FabricRpgVisualProofRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal(QA_COMMAND)
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> inspect(context.getSource()))));
    }

    private static int inspect(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Visual proof must be inspected by a player."));
            return 0;
        }

        FabricCedarServiceYardRuntime.ProvisioningResult yard =
                FabricCedarServiceYardRuntime.ensureProvisioned(player.getServer());
        if (!yard.complete()) {
            source.sendError(Text.literal(
                    "Cedar service yard is not complete; occupied world blocks were preserved. "
                            + "placed=" + yard.placed() + ", present=" + yard.present()
                            + ", blocked=" + yard.blocked()));
            return 0;
        }

        ServerWorld world = player.getServerWorld();
        BlockPos view = FabricCedarServiceYardRuntime.viewingPosition(player.getServer());
        player.teleport(world,
                view.getX() + 0.5D,
                view.getY(),
                view.getZ() + 0.5D,
                0.0F,
                8.0F);
        player.sendMessage(Text.literal(
                "AutoPTU RPG visual proof ready: inspecting the normal Cedar service yard."), false);
        LOGGER.info("AutoPTU RPG visual proof scene built for {} from normal Cedar service yard",
                player.getGameProfile().getName());
        return 1;
    }
}
