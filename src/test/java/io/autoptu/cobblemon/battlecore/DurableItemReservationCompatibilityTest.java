package io.autoptu.cobblemon.battlecore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

final class DurableItemReservationCompatibilityTest {
    @Test
    void sliceConsumesOnlyPartialNonBlockingItemAndAdapterBoundaries() {
        assertEquals(Set.of(
                        UpstreamCompatibilityMatrix.Capability.ITEMS,
                        UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK),
                DurableItemReservationCompatibility.CAPABILITIES);
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.ITEMS).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                UpstreamCompatibilityMatrix.entry(
                        UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK).support());
        assertFalse(DurableItemReservationCompatibility.hasBlockingDependency());
        assertTrue(DurableItemReservationCompatibility.CONTRACT.contains("AutoPTU-Java-owned"));
    }
}
