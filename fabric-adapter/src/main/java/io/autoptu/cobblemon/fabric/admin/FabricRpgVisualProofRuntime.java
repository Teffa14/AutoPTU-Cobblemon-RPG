package io.autoptu.cobblemon.fabric.admin;

import com.cobblemon.mod.common.CobblemonBlocks;
import io.autoptu.cobblemon.fabric.rpg.FabricRpgContent;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Blocks;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Operator-only deterministic showcase for visually inspecting already-live RPG world surfaces.
 *
 * This scene creates no PTU facts. It only places existing Minecraft/Cobblemon presentation blocks
 * in a stable layout near the operator so graphical-client evidence can be captured reproducibly.
 */
public final class FabricRpgVisualProofRuntime {
    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-rpg-visual-proof");
    public static final String QA_COMMAND = "autoptuvisualproof";

    private FabricRpgVisualProofRuntime() {}

    public static void register() {
        // Keep the QA capture command on its own root. The production adapter has multiple
        // independently registered /autoptu branches; a unique operator-only root prevents the
        // evidence hook from depending on Brigadier merge order while remaining unavailable to
        // normal players.
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal(QA_COMMAND)
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> build(context.getSource()))));
    }

    private static int build(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Visual proof scene must be created by a player."));
            return 0;
        }

        ServerWorld world = player.getServerWorld();
        BlockPos origin = player.getBlockPos().add(-6, -1, 8);

        // Neutral inspection floor and backdrop. These blocks have no RPG authority.
        for (int x = 0; x <= 12; x++) {
            for (int z = 0; z <= 8; z++) {
                world.setBlockState(origin.add(x, 0, z), Blocks.SMOOTH_STONE.getDefaultState());
            }
        }
        for (int x = 0; x <= 12; x++) {
            world.setBlockState(origin.add(x, 1, 8), Blocks.QUARTZ_BLOCK.getDefaultState());
            world.setBlockState(origin.add(x, 2, 8), Blocks.QUARTZ_BLOCK.getDefaultState());
            world.setBlockState(origin.add(x, 3, 8), Blocks.QUARTZ_BLOCK.getDefaultState());
        }

        // Cobblemon-native facilities already reused by the RPG adapter.
        place(world, origin.add(1, 1, 2), CobblemonBlocks.HEALING_MACHINE.getDefaultState());
        place(world, origin.add(3, 1, 2), CobblemonBlocks.PC.getDefaultState());

        // Ouros-specific persistent RPG facilities already live on main.
        place(world, origin.add(5, 1, 2), FabricRpgContent.PTU_RECOVERY_BED.getDefaultState());
        place(world, origin.add(7, 1, 2), FabricRpgContent.CEDAR_MART_COUNTER.getDefaultState());
        place(world, origin.add(9, 1, 2), FabricRpgContent.ITEM_STORAGE_TERMINAL.getDefaultState());
        place(world, origin.add(11, 1, 2), FabricRpgContent.CRAFTING_WORKSTATION.getDefaultState());

        place(world, origin.add(2, 1, 5), FabricRpgContent.FIELD_CAMP.getDefaultState());
        place(world, origin.add(4, 1, 5), FabricRpgContent.GYM_LEAGUE_REGISTRATION_DESK.getDefaultState());
        place(world, origin.add(6, 1, 5), FabricRpgContent.CEDAR_BADGE_GATE.getDefaultState());
        place(world, origin.add(8, 1, 5), FabricRpgContent.OUROS_MAILBOX.getDefaultState());
        place(world, origin.add(10, 1, 5), Blocks.LODESTONE.getDefaultState());

        player.teleport(world,
                origin.getX() + 6.5D,
                origin.getY() + 2.0D,
                origin.getZ() - 4.5D,
                0.0F,
                8.0F);
        player.sendMessage(Text.literal("AutoPTU RPG visual proof scene ready. Face forward to inspect live world surfaces."), false);
        LOGGER.info("AutoPTU RPG visual proof scene built for {}", player.getGameProfile().getName());
        return 1;
    }

    private static void place(ServerWorld world, BlockPos pos, net.minecraft.block.BlockState state) {
        world.setBlockState(pos, state);
    }
}
