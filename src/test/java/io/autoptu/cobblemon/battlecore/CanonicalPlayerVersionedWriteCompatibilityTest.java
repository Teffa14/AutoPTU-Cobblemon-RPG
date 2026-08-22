package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalPlayerVersionedWriteCompatibilityTest {
    @Test
    void versionedCanonicalPlayerWritesDependOnlyOnServerAuthorityAdapterBoundary() {
        EnumSet<UpstreamCompatibilityMatrix.Capability> dependencies = EnumSet.of(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK
        );

        assertEquals(1, dependencies.size());
        assertFalse(dependencies.stream()
                .map(UpstreamCompatibilityMatrix::entry)
                .anyMatch(entry -> entry.support() == UpstreamCompatibilityMatrix.Support.BLOCKING));

        UpstreamCompatibilityMatrix.Entry adapter = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK);
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, adapter.support());
        assertTrue(adapter.contracts().contains("CanonicalPlayerMutationService"));
        assertTrue(adapter.contracts().contains("VersionedCanonicalStateRepository"));
        assertTrue(adapter.contracts().contains("compare-and-set"));
        assertTrue(adapter.adapterPolicy().contains("durable production canonical-state backend"));
        assertTrue(adapter.adapterPolicy().contains("cross-aggregate Pokemon/item transaction recovery"));
        assertTrue(adapter.adapterPolicy().contains("client replacement aggregates"));
    }
}
