package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WorldScopedWildBlueprintRegistryCompatibilityTest {
    @Test
    void consumesOnlyThePartialMinecraftAdapterBoundary() {
        assertEquals(
                Set.of(UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK),
                WorldScopedWildBlueprintRegistryCompatibility.requirement().capabilities()
        );
        assertFalse(WorldScopedWildBlueprintRegistryCompatibility.hasBlockingDependency());
    }
}
