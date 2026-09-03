package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalWorldHierarchyCatalogueTest {
    @Test
    void defaultHierarchyKeepsMareaBelowWorldScope() {
        var catalogue = CanonicalWorldHierarchyCatalogue.DEFAULT;
        var world = catalogue.node(CanonicalWorldHierarchyCatalogue.OUROS_WORLD_ID).orElseThrow();
        var marea = catalogue.node(CanonicalWorldHierarchyCatalogue.MAREA_TERRITORY_ID).orElseThrow();

        assertEquals(CanonicalWorldHierarchyCatalogue.NodeKind.WORLD, world.kind());
        assertEquals(world.nodeId(), marea.parentNodeId());
        assertEquals(CanonicalWorldHierarchyCatalogue.NodeKind.TERRITORY, marea.kind());
        assertTrue(catalogue.childrenOf(marea.nodeId()).stream()
                .anyMatch(node -> node.nodeId().equals("ouros.locality.puerto_bruma")));
    }

    @Test
    void rejectsOrphanedNonWorldNodes() {
        assertThrows(IllegalArgumentException.class, () -> new CanonicalWorldHierarchyCatalogue(List.of(
                new CanonicalWorldHierarchyCatalogue.Node(
                        "ouros.locality.bad",
                        "Bad",
                        CanonicalWorldHierarchyCatalogue.NodeKind.LOCALITY,
                        "ouros.missing",
                        "minecraft:overworld",
                        null
                )
        )));
    }

    @Test
    void rejectsUnknownCanonicalSiteBindings() {
        assertThrows(IllegalArgumentException.class, () -> new CanonicalWorldHierarchyCatalogue(List.of(
                new CanonicalWorldHierarchyCatalogue.Node(
                        "ouros.world.test",
                        "Test",
                        CanonicalWorldHierarchyCatalogue.NodeKind.WORLD,
                        null,
                        "minecraft:overworld",
                        null
                ),
                new CanonicalWorldHierarchyCatalogue.Node(
                        "ouros.locality.test",
                        "Test Locality",
                        CanonicalWorldHierarchyCatalogue.NodeKind.LOCALITY,
                        "ouros.world.test",
                        "minecraft:overworld",
                        "ouros.missing.site"
                )
        )));
    }
}
