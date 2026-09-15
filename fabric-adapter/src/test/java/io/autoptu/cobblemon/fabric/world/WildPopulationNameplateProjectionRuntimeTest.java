package io.autoptu.cobblemon.fabric.world;

import net.minecraft.world.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WildPopulationNameplateProjectionRuntimeTest {
    @AfterEach
    void clearSnapshots() {
        WildPopulationNameplateProjectionRuntime.clearSnapshotsForTest();
    }

    @Test
    void dormantActorsSuppressNameplateAndRestorePriorVisiblePolicy() {
        var key = new WildPopulationNameplateProjectionRuntime.ActorKey(World.OVERWORLD, UUID.randomUUID());
        AtomicBoolean visible = new AtomicBoolean(true);

        WildPopulationNameplateProjectionRuntime.synchronizeNameplate(key, visible.get(), true, visible::set);
        assertFalse(visible.get());

        WildPopulationNameplateProjectionRuntime.synchronizeNameplate(key, visible.get(), false, visible::set);
        assertTrue(visible.get());
    }

    @Test
    void dormantActorsPreservePriorHiddenPolicy() {
        var key = new WildPopulationNameplateProjectionRuntime.ActorKey(World.OVERWORLD, UUID.randomUUID());
        AtomicBoolean visible = new AtomicBoolean(false);

        WildPopulationNameplateProjectionRuntime.synchronizeNameplate(key, visible.get(), true, visible::set);
        assertFalse(visible.get());

        WildPopulationNameplateProjectionRuntime.synchronizeNameplate(key, visible.get(), false, visible::set);
        assertFalse(visible.get());
    }

    @Test
    void activeVisibleActorsAreNotSuppressed() {
        assertFalse(WildPopulationNameplateProjectionRuntime.shouldSuppressNameplate(true, false));
        assertTrue(WildPopulationNameplateProjectionRuntime.shouldSuppressNameplate(false, false));
        assertTrue(WildPopulationNameplateProjectionRuntime.shouldSuppressNameplate(true, true));
    }
}
