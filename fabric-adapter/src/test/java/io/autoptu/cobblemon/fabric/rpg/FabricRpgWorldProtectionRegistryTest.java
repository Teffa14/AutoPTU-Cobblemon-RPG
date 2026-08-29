package io.autoptu.cobblemon.fabric.rpg;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FabricRpgWorldProtectionRegistryTest {
    private static final String SCOPE = "test:battle";

    @AfterEach
    void cleanup() {
        FabricRpgWorldProtectionRegistry.clear(SCOPE);
    }

    @Test
    void leavesOrdinaryMinecraftTerrainUnprotectedByDefault() {
        assertTrue(FabricRpgWorldProtectionRegistry
                .protectionAt(World.OVERWORLD, new BlockPos(100, 64, 100))
                .isEmpty());
    }

    @Test
    void protectsOnlyTheExplicitInteractionFootprintAndClearsAfterward() {
        FabricRpgWorldProtectionRegistry.protect(
                SCOPE,
                World.OVERWORLD,
                new BlockPos(12, 70, 12),
                new BlockPos(8, 66, 8),
                "active battle"
        );

        var inside = FabricRpgWorldProtectionRegistry
                .protectionAt(World.OVERWORLD, new BlockPos(10, 68, 10));
        assertTrue(inside.isPresent());
        assertEquals("active battle", inside.orElseThrow().reason());
        assertTrue(FabricRpgWorldProtectionRegistry
                .protectionAt(World.OVERWORLD, new BlockPos(7, 68, 10))
                .isEmpty());

        FabricRpgWorldProtectionRegistry.clear(SCOPE);
        assertTrue(FabricRpgWorldProtectionRegistry
                .protectionAt(World.OVERWORLD, new BlockPos(10, 68, 10))
                .isEmpty());
    }
}
