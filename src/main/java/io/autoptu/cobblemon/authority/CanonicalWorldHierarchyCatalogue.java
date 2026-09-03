package io.autoptu.cobblemon.authority;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Server-authored hierarchy for the Ouros overworld.
 *
 * <p>This is world/narrative topology only. It does not own PTU rules, battles, stats, capture,
 * progression math, rewards or Cobblemon gameplay state. The hierarchy is deliberately broader
 * than any one city or local region so authored content can scale from the whole world down to
 * settlements, routes and sites without treating a vertical-slice location as the world model.</p>
 */
public final class CanonicalWorldHierarchyCatalogue {
    public static final String OUROS_WORLD_ID = "ouros.world";
    public static final String MAREA_TERRITORY_ID = "ouros.territory.marea";

    public static final CanonicalWorldHierarchyCatalogue DEFAULT = new CanonicalWorldHierarchyCatalogue(List.of(
            new Node(
                    OUROS_WORLD_ID,
                    "Ouros",
                    NodeKind.WORLD,
                    null,
                    "minecraft:overworld",
                    null
            ),
            new Node(
                    MAREA_TERRITORY_ID,
                    "Marea",
                    NodeKind.TERRITORY,
                    OUROS_WORLD_ID,
                    "minecraft:overworld",
                    null
            ),
            new Node(
                    "ouros.locality.puerto_bruma",
                    "Puerto Bruma",
                    NodeKind.LOCALITY,
                    MAREA_TERRITORY_ID,
                    "minecraft:overworld",
                    "ouros.marea.puerto_bruma"
            ),
            new Node(
                    "ouros.route.sendero_vidrio",
                    "Sendero del Vidrio",
                    NodeKind.ROUTE,
                    MAREA_TERRITORY_ID,
                    "minecraft:overworld",
                    "ouros.marea.sendero_vidrio"
            ),
            new Node(
                    "ouros.locality.loma_clara",
                    "Loma Clara",
                    NodeKind.LOCALITY,
                    MAREA_TERRITORY_ID,
                    "minecraft:overworld",
                    "ouros.marea.loma_clara"
            ),
            new Node(
                    "ouros.locality.estacion_mirador",
                    "Estacion Mirador",
                    NodeKind.LOCALITY,
                    MAREA_TERRITORY_ID,
                    "minecraft:overworld",
                    "ouros.marea.estacion_mirador"
            )
    ));

    private final Map<String, Node> nodes;

    public CanonicalWorldHierarchyCatalogue(List<Node> nodes) {
        Objects.requireNonNull(nodes, "nodes");
        LinkedHashMap<String, Node> indexed = new LinkedHashMap<>();
        for (Node node : nodes) {
            Objects.requireNonNull(node, "node");
            if (indexed.putIfAbsent(node.nodeId(), node) != null) {
                throw new IllegalArgumentException("duplicate world hierarchy node id: " + node.nodeId());
            }
        }
        for (Node node : indexed.values()) {
            if (node.parentNodeId() != null && !indexed.containsKey(node.parentNodeId())) {
                throw new IllegalArgumentException("unknown parent world hierarchy node: " + node.parentNodeId());
            }
            if (node.canonicalSiteId() != null && CanonicalWorldMapCatalogue.DEFAULT.site(node.canonicalSiteId()).isEmpty()) {
                throw new IllegalArgumentException("unknown canonical site for world hierarchy node: " + node.canonicalSiteId());
            }
        }
        this.nodes = Map.copyOf(indexed);
    }

    public Optional<Node> node(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) return Optional.empty();
        return Optional.ofNullable(nodes.get(nodeId.strip()));
    }

    public List<Node> childrenOf(String parentNodeId) {
        if (parentNodeId == null || parentNodeId.isBlank()) return List.of();
        String normalized = parentNodeId.strip();
        return nodes.values().stream()
                .filter(node -> normalized.equals(node.parentNodeId()))
                .toList();
    }

    public List<Node> nodes() {
        return List.copyOf(nodes.values());
    }

    public enum NodeKind {
        WORLD,
        COUNTRY,
        MACRO_REGION,
        TERRITORY,
        LOCALITY,
        ROUTE,
        SITE
    }

    public record Node(
            String nodeId,
            String displayName,
            NodeKind kind,
            String parentNodeId,
            String dimensionId,
            String canonicalSiteId
    ) {
        public Node {
            nodeId = requireText(nodeId, "nodeId");
            displayName = requireText(displayName, "displayName");
            kind = Objects.requireNonNull(kind, "kind");
            dimensionId = requireText(dimensionId, "dimensionId");
            if (parentNodeId != null) parentNodeId = requireText(parentNodeId, "parentNodeId");
            if (canonicalSiteId != null) canonicalSiteId = requireText(canonicalSiteId, "canonicalSiteId");
            if (kind == NodeKind.WORLD && parentNodeId != null) {
                throw new IllegalArgumentException("world hierarchy root cannot have a parent");
            }
            if (kind != NodeKind.WORLD && parentNodeId == null) {
                throw new IllegalArgumentException("non-world hierarchy node requires a parent");
            }
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}
