package io.autoptu.cobblemon.fabric.rpg;

import com.cobblemon.mod.common.CobblemonBlocks;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Provisions a compact set of already-authoritative RPG facilities near normal Overworld spawn.
 *
 * The yard owns only Minecraft placement/presentation. Every facility keeps using its existing
 * server-owned RPG service boundary, while Cobblemon blocks remain presentation/identity surfaces.
 * Existing non-air world blocks are never overwritten.
 */
public final class FabricCedarServiceYardRuntime {
    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cedar-service-yard");

    private FabricCedarServiceYardRuntime() {}

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(FabricCedarServiceYardRuntime::ensureProvisioned);
    }

    public static ProvisioningResult ensureProvisioned(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        BlockPos spawn = world.getSpawnPos();
        BlockPos anchor = findStandingPosition(world, spawn);

        List<Facility> facilities = List.of(
                new Facility("healing_machine", -8, 6, CobblemonBlocks.HEALING_MACHINE.getDefaultState()),
                new Facility("pc", -6, 6, CobblemonBlocks.PC.getDefaultState()),
                new Facility("recovery_bed", -4, 6, FabricRpgContent.PTU_RECOVERY_BED.getDefaultState()),
                new Facility("cedar_mart", -2, 6, FabricRpgContent.CEDAR_MART_COUNTER.getDefaultState()),
                new Facility("item_storage", -8, 9, FabricRpgContent.ITEM_STORAGE_TERMINAL.getDefaultState()),
                new Facility("crafting", -6, 9, FabricRpgContent.CRAFTING_WORKSTATION.getDefaultState()),
                new Facility("field_camp", -4, 9, FabricRpgContent.FIELD_CAMP.getDefaultState()),
                new Facility("league_desk", -2, 9, FabricRpgContent.GYM_LEAGUE_REGISTRATION_DESK.getDefaultState()),
                new Facility("mailbox", -8, 12, FabricRpgContent.OUROS_MAILBOX.getDefaultState()),
                new Facility("fast_travel", -6, 12, Blocks.LODESTONE.getDefaultState())
        );

        int placed = 0;
        int present = 0;
        int blocked = 0;
        for (Facility facility : facilities) {
            BlockPos pos = anchor.add(facility.offsetX(), 0, facility.offsetZ());
            BlockState current = world.getBlockState(pos);
            if (current.isOf(facility.state().getBlock())) {
                present++;
                continue;
            }
            if (!current.isAir()) {
                blocked++;
                LOGGER.warn("Cedar service yard left occupied block unchanged for {} at {}", facility.id(), pos);
                continue;
            }
            if (world.setBlockState(pos, facility.state(), Block.NOTIFY_ALL)) {
                placed++;
            } else {
                blocked++;
            }
        }

        LOGGER.info("AutoPTU Cedar service yard ready: placed={}, present={}, blocked={}", placed, present, blocked);
        return new ProvisioningResult(anchor, facilities.size(), placed, present, blocked);
    }

    public static BlockPos viewingPosition(MinecraftServer server) {
        BlockPos anchor = findStandingPosition(server.getOverworld(), server.getOverworld().getSpawnPos());
        return anchor.add(-5, 1, 1);
    }

    private static BlockPos findStandingPosition(World world, BlockPos preferred) {
        for (int dy = 4; dy >= -4; dy--) {
            BlockPos candidate = preferred.add(0, dy, 0);
            if (!world.getBlockState(candidate).isAir()) continue;
            if (!world.getBlockState(candidate.up()).isAir()) continue;
            if (world.getBlockState(candidate.down()).isAir()) continue;
            return candidate;
        }
        return preferred;
    }

    private record Facility(String id, int offsetX, int offsetZ, BlockState state) {}

    public record ProvisioningResult(BlockPos anchor, int expected, int placed, int present, int blocked) {
        public boolean complete() {
            return blocked == 0 && placed + present == expected;
        }
    }
}
