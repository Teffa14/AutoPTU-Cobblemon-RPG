package io.autoptu.cobblemon.fabric.rpg;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.Comparator;
import java.util.List;

/**
 * Provisions the authored Cedar Ranger as a normal persistent world actor.
 *
 * The villager owns presentation and physical interaction only. Its canonical NPC identity is
 * bound one-way through {@link FabricNpcDialogueRuntime}; Trainer, quest, wallet, challenge and
 * battle truth remain in server-owned AutoPTU repositories/services.
 */
public final class FabricCedarRangerProvisioningRuntime {
    static final String NPC_ID = "cedar-ranger";
    private static final int SPAWN_OFFSET_X = 6;
    private static final int SPAWN_OFFSET_Z = 6;
    private static final double SEARCH_RADIUS = 48.0D;

    private FabricCedarRangerProvisioningRuntime() {}

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(FabricCedarRangerProvisioningRuntime::ensureProvisioned);
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

    private static BlockPos findStandingPosition(ServerWorld world, BlockPos preferred) {
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
