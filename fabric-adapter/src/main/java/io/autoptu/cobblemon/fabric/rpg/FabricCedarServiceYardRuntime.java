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
 * Existing non-air world blocks are never overwritten. Each facility has a small authored fallback
 * set so normal terrain or another server provisioner cannot silently remove part of the service yard.
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
                facility("healing_machine", CobblemonBlocks.HEALING_MACHINE.getDefaultState(), -8, 6),
                facility("pc", CobblemonBlocks.PC.getDefaultState(), -6, 6),
                facility("recovery_bed", FabricRpgContent.PTU_RECOVERY_BED.getDefaultState(), -4, 6),
                facility("cedar_mart", FabricRpgContent.CEDAR_MART_COUNTER.getDefaultState(), -2, 6),
                facility("item_storage", FabricRpgContent.ITEM_STORAGE_TERMINAL.getDefaultState(), -8, 9),
                facility("crafting", FabricRpgContent.CRAFTING_WORKSTATION.getDefaultState(), -6, 9),
                facility("field_camp", FabricRpgContent.FIELD_CAMP.getDefaultState(), -4, 9),
                facility("league_desk", FabricRpgContent.GYM_LEAGUE_REGISTRATION_DESK.getDefaultState(), -2, 9),
                facility("mailbox", FabricRpgContent.OUROS_MAILBOX.getDefaultState(), -8, 12),
                facility("fast_travel", Blocks.LODESTONE.getDefaultState(), -6, 12)
        );

        int placed = 0;
        int present = 0;
        int blocked = 0;
        for (Facility facility : facilities) {
            Placement placement = ensureFacility(world, anchor, facility);
            switch (placement) {
                case PLACED -> placed++;
                case PRESENT -> present++;
                case BLOCKED -> blocked++;
            }
        }

        LOGGER.info("AutoPTU Cedar service yard ready: placed={}, present={}, blocked={}", placed, present, blocked);
        return new ProvisioningResult(anchor, facilities.size(), placed, present, blocked);
    }

    public static BlockPos viewingPosition(MinecraftServer server) {
        BlockPos anchor = findStandingPosition(server.getOverworld(), server.getOverworld().getSpawnPos());
        return anchor.add(-5, 1, 1);
    }

    private static Placement ensureFacility(ServerWorld world, BlockPos anchor, Facility facility) {
        for (Offset candidate : facility.candidates()) {
            BlockPos pos = anchor.add(candidate.x(), 0, candidate.z());
            if (world.getBlockState(pos).isOf(facility.state().getBlock())) {
                return Placement.PRESENT;
            }
        }
        for (Offset candidate : facility.candidates()) {
            BlockPos pos = anchor.add(candidate.x(), 0, candidate.z());
            if (!world.getBlockState(pos).isAir()) continue;
            if (world.setBlockState(pos, facility.state(), Block.NOTIFY_ALL)) {
                if (!candidate.equals(facility.candidates().getFirst())) {
                    LOGGER.info("Cedar service yard used authored fallback for {} at {}", facility.id(), pos);
                }
                return Placement.PLACED;
            }
        }
        LOGGER.warn("Cedar service yard found no free authored slot for {}", facility.id());
        return Placement.BLOCKED;
    }

    private static Facility facility(String id, BlockState state, int preferredX, int preferredZ) {
        return new Facility(id, state, List.of(
                new Offset(preferredX, preferredZ),
                new Offset(preferredX - 1, preferredZ),
                new Offset(preferredX + 1, preferredZ),
                new Offset(preferredX, preferredZ + 1),
                new Offset(preferredX, preferredZ - 1)
        ));
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

    private enum Placement { PLACED, PRESENT, BLOCKED }

    private record Offset(int x, int z) {}

    private record Facility(String id, BlockState state, List<Offset> candidates) {}

    public record ProvisioningResult(BlockPos anchor, int expected, int placed, int present, int blocked) {
        public boolean complete() {
            return blocked == 0 && placed + present == expected;
        }
    }
}
