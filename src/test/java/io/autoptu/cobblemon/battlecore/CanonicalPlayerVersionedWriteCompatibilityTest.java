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
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK
        );

        assertEquals(1, dependencies.size());
        assertFalse(dependencies.stream()
                .map(UpstreamCompatibilityMatrix::entry)
                .anyMatch(entry -> entry.support() == UpstreamCompatibilityMatrix.Support.BLOCKING));

        UpstreamCompatibilityMatrix.Entry adapter = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK);
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, adapter.support());
        assertTrue(adapter.contracts().contains("FabricCanonicalPlayerProvisioning"));
        assertTrue(adapter.contracts().contains("FabricCanonicalPlayerStoreRuntime"));
        assertTrue(adapter.contracts().contains("FileCanonicalItemReservationRepository"));
        assertTrue(adapter.contracts().contains("FileCanonicalPokemonRepository"));
        assertTrue(adapter.contracts().contains("two-process restart smoke"));
        assertTrue(adapter.contracts().contains("ServerOwnedWildEncounterIdentityBinder"));
        assertTrue(adapter.adapterPolicy().contains("cross-aggregate transactions"));
        assertTrue(adapter.adapterPolicy().contains("partial-commit recovery"));
        assertTrue(adapter.adapterPolicy().contains("trusted server-owned encounter service"));
        assertTrue(adapter.adapterPolicy().contains("never derives species, level, HP, stats, moves, abilities"));
    }
}
