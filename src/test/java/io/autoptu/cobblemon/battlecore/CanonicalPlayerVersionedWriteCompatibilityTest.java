package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalPlayerVersionedWriteCompatibilityTest {
    @Test
    void durableVersionedCanonicalPlayerWritesDependOnlyOnServerAuthorityAdapterBoundary() {
        EnumSet<UpstreamCompatibilityMatrix.Capability> dependencies = EnumSet.of(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK);
        assertEquals(1, dependencies.size());
        assertFalse(dependencies.stream().map(UpstreamCompatibilityMatrix::entry)
                .anyMatch(entry -> entry.support() == UpstreamCompatibilityMatrix.Support.BLOCKING));

        UpstreamCompatibilityMatrix.Entry adapter = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK);
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, adapter.support());
        assertTrue(adapter.contracts().contains("World-scoped durable player/profile/Pokemon/item stores"));
        assertTrue(adapter.contracts().contains("create-only WILD blueprint registry"));
        assertTrue(adapter.contracts().contains("WILD publication and preparation"));
        assertTrue(adapter.adapterPolicy().contains("cross-aggregate transaction recovery"));
        assertTrue(adapter.adapterPolicy().contains("no PTU values may be derived from Cobblemon"));
    }
}
