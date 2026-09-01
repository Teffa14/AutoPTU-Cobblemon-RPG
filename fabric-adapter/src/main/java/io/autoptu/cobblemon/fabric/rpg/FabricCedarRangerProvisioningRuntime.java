package io.autoptu.cobblemon.fabric.rpg;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.Comparator;
import java.util.List;

/**
 * Provisions the authored Cedar Ranger and field-notes quest object as normal persistent world actors.
 *
 * The villager and lectern own presentation and physical interaction only. Their canonical RPG
 * effects remain in server-owned AutoPTU repositories/services.
 */
public final class FabricCedarRangerProvisioningRuntime {
    static final String NPC_ID = "cedar-ranger";
    private static final int SPAWN_OFFSET_X = 6;
    private static final int SPAWN_OFFSET_Z = 6;
    private static final int FIELD_NOTES_OFFSET_X = 2;
    private static final double SEARCH_RADIUS = 48.0D;

    private FabricCedarRangerProvisioningRuntime() {}

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ensureProvisioned(server);
            ensureFieldNotesProvisioned(server);
        });
    }

    static VillagerEntity ensureProvisioned(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        BlockPos spawn = world.getSpawnPos();
        BlockPos target = findStandingPosition(world, spawn.add(SPAWN_OFFSET_X, 0, SPAWN_OFFSET_Z));
        Box search = new Box(
                spawn.getX() - SEARCH_RADIUS,
                spawn.getY() - 32.0D,
                spawn.getZ() - SEARCH_RADIUS,
                spawn.getX() + SEARCH_RADIUS,
                spawn.getY() + 32.0D,
                spawn.getZ() + SEARCH_RADIUS
        );
        List<VillagerEntity> existing = world.getEntitiesByClass(
                VillagerEntity.class,
                search,
                villager -> FabricNpcDialogueRuntime.npcId(villager).filter(NPC_ID::equals).isPresent()
        );
        if (!existing.isEmpty()) {
            return existing.stream()
                    .min(Comparator.comparingDouble(villager -> villager.squaredDistanceTo(
                            target.getX() + 0.5D,
                            target.getY(),
                            target.getZ() + 0.5D
                    )))
                    .orElseThrow();
        }

        VillagerEntity ranger = new VillagerEntity(EntityType.VILLAGER, world);
        ranger.refreshPositionAndAngles(
                target.getX() + 0.5D,
                target.getY(),
                target.getZ() + 0.5D,
                180.0F,
                0.0F
        );
        FabricNpcDialogueRuntime.bind(ranger, NPC_ID);
        if (!world.spawnEntity(ranger)) {
            ranger.discard();
            throw new IllegalStateException("Could not provision the canonical Cedar Ranger presentation actor");
        }
        return ranger;
    }

    static BlockPos ensureFieldNotesProvisioned(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        BlockPos anchor = fieldNotesAnchor(world);
        BlockPos marker = anchor.down();
        if (!world.getBlockState(marker).isOf(Blocks.GOLD_BLOCK)) {
            world.setBlockState(marker, Blocks.GOLD_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
        }
        if (!world.getBlockState(anchor).isOf(Blocks.LECTERN)) {
            world.setBlockState(anchor, Blocks.LECTERN.getDefaultState(), Block.NOTIFY_ALL);
        }
        return anchor;
    }

    static boolean isCanonicalFieldNotes(World world, BlockPos anchor) {
        if (world == null || anchor == null || !world.getRegistryKey().equals(World.OVERWORLD)) return false;
        BlockPos expected = fieldNotesAnchor(world);
        return anchor.equals(expected)
                && world.getBlockState(anchor).isOf(Blocks.LECTERN)
                && world.getBlockState(anchor.down()).isOf(Blocks.GOLD_BLOCK);
    }

    static BlockPos fieldNotesAnchor(World world) {
        BlockPos spawn = world.getSpawnPos();
        BlockPos rangerTarget = findStandingPosition(world, spawn.add(SPAWN_OFFSET_X, 0, SPAWN_OFFSET_Z));
        return rangerTarget.add(FIELD_NOTES_OFFSET_X, 0, 0);
    }

    private static BlockPos findStandingPosition(World world, BlockPos preferred) {
        for (int dy = 4; dy >= -4; dy--) {
            BlockPos candidate = preferred.add(0, dy, 0);
            if (!world.getBlockState(candidate).isAir()) continue;
            if (!world.getBlockState(candidate.up()).isAir()) continue;
            if (world.getBlockState(candidate.down()).isAir()) continue;
            return candidate;
        }
        return world.getSpawnPos();
    }
}
