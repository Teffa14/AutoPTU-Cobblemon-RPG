package io.autoptu.cobblemon.fabric.world;

import net.minecraft.world.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WildPopulationGlowProjectionRuntimeTest {
    @AfterEach
    void clearSnapshots() {
        WildPopulationGlowProjectionRuntime.clearSnapshotsForTest();
    }

    @Test
    void dormantActorsSuppressGlowAndRestorePriorTruePolicy() {
        var key = new WildPopulationGlowProjectionRuntime.ActorKey(World.OVERWORLD, UUID.randomUUID());
        AtomicBoolean glowing = new AtomicBoolean(true);

        WildPopulationGlowProjectionRuntime.synchronizeGlow(key, glowing.get(), true, glowing::set);
        assertFalse(glowing.get());

        WildPopulationGlowProjectionRuntime.synchronizeGlow(key, glowing.get(), false, glowing::set);
        assertTrue(glowing.get());
    }

    @Test
    void dormantActorsRestorePriorFalsePolicy() {
        var key = new WildPopulationGlowProjectionRuntime.ActorKey(World.OVERWORLD, UUID.randomUUID());
        AtomicBoolean glowing = new AtomicBoolean(false);

        WildPopulationGlowProjectionRuntime.synchronizeGlow(key, glowing.get(), true, glowing::set);
        assertFalse(glowing.get());

        WildPopulationGlowProjectionRuntime.synchronizeGlow(key, glowing.get(), false, glowing::set);
        assertFalse(glowing.get());
    }

    @Test
    void activeVisibleActorsAreNotSuppressed() {
        assertFalse(WildPopulationGlowProjectionRuntime.shouldSuppressGlow(true, false));
        assertTrue(WildPopulationGlowProjectionRuntime.shouldSuppressGlow(false, false));
        assertTrue(WildPopulationGlowProjectionRuntime.shouldSuppressGlow(true, true));
    }
}
