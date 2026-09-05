package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildEcologyProjectionRegistryTest {
    @Test
    void multipleIndependentPopulationSourcesShareTheGenericRuntimeRegistry() {
        int before = WildEcologyProjectionRegistry.sourcesSnapshot().size();
        String suffix = UUID.randomUUID().toString();
        WildEcologyProjectionRegistry.ProjectionSource grassland = world -> List.of();
        WildEcologyProjectionRegistry.ProjectionSource cave = world -> List.of();

        WildEcologyProjectionRegistry.register("test.grassland." + suffix, grassland);
        WildEcologyProjectionRegistry.register("test.cave." + suffix, cave);

        var after = WildEcologyProjectionRegistry.sourcesSnapshot();
        assertEquals(before + 2, after.size());
        assertTrue(after.contains(grassland));
        assertTrue(after.contains(cave));
    }

    @Test
    void duplicateSourceIdentityFailsClosedInsteadOfReplacingAuthority() {
        String id = "test.duplicate." + UUID.randomUUID();
        WildEcologyProjectionRegistry.ProjectionSource first = world -> List.of();
        WildEcologyProjectionRegistry.register(id, first);
        WildEcologyProjectionRegistry.register(id, first);

        assertThrows(IllegalStateException.class,
                () -> WildEcologyProjectionRegistry.register(id, world -> List.of()));
    }
}
