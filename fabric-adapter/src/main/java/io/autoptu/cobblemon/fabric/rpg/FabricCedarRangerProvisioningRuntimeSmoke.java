package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRestartSmoke;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/** Dedicated-server proof that normal-world Cedar Ranger provisioning is restart/idempotency safe. */
public final class FabricCedarRangerProvisioningRuntimeSmoke {
    public static final String ENABLE_PROPERTY = "autoptu.liveCedarRangerProvisioningSmoke";
    public static final String SUCCESS_LOG = "AutoPTU live Cedar Ranger normal-world provisioning smoke passed";
    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");

    private FabricCedarRangerProvisioningRuntimeSmoke() {}

    public static void registerIfEnabled() {
        boolean dedicated = Boolean.getBoolean(ENABLE_PROPERTY);
        boolean restartRun = !System.getProperty(FabricCanonicalPlayerStoreRestartSmoke.MODE_PROPERTY, "").isBlank();
        if (!dedicated && !restartRun) return;
        ServerLifecycleEvents.SERVER_STARTED.register(FabricCedarRangerProvisioningRuntimeSmoke::run);
    }

    private static void run(MinecraftServer server) {
        VillagerEntity first = FabricCedarRangerProvisioningRuntime.ensureProvisioned(server);
        VillagerEntity second = FabricCedarRangerProvisioningRuntime.ensureProvisioned(server);
        if (!first.getUuid().equals(second.getUuid())) {
            throw new IllegalStateException("Cedar Ranger provisioning was not idempotent within one server boot");
        }
        if (FabricNpcDialogueRuntime.npcId(first)
                .filter(FabricCedarRangerProvisioningRuntime.NPC_ID::equals)
                .isEmpty()) {
            throw new IllegalStateException("Provisioned Cedar Ranger is missing canonical NPC identity");
        }
        if (!first.isPersistent()) {
            throw new IllegalStateException("Provisioned Cedar Ranger is not persistent");
        }

        ServerWorld world = server.getOverworld();
        BlockPos spawn = world.getSpawnPos();
        Box search = new Box(
                spawn.getX() - 48.0D,
                spawn.getY() - 32.0D,
                spawn.getZ() - 48.0D,
                spawn.getX() + 48.0D,
                spawn.getY() + 32.0D,
                spawn.getZ() + 48.0D
        );
        List<VillagerEntity> canonicalRangers = world.getEntitiesByClass(
                VillagerEntity.class,
                search,
                villager -> FabricNpcDialogueRuntime.npcId(villager)
                        .filter(FabricCedarRangerProvisioningRuntime.NPC_ID::equals)
                        .isPresent()
        );
        if (canonicalRangers.size() != 1) {
            throw new IllegalStateException("Expected exactly one canonical Cedar Ranger near spawn, found " + canonicalRangers.size());
        }
        LOGGER.info(SUCCESS_LOG);
    }
}
