package io.autoptu.cobblemon.authority;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Server-side projection of durable authored-location discoveries onto the world hierarchy.
 *
 * <p>This service does not grant XP, rewards, battle state or PTU effects. It only summarizes
 * already-persisted server-observed discoveries so Minecraft can present MMORPG exploration progress.
 * A hierarchy node counts as explored when every discoverable descendant site bound beneath it has
 * been discovered. Nodes with no discoverable descendants never auto-complete.</p>
 */
public final class CanonicalWorldExplorationProgressService {
    private final CanonicalWorldHierarchyCatalogue hierarchy;

    public CanonicalWorldExplorationProgressService(CanonicalWorldHierarchyCatalogue hierarchy) {
        this.hierarchy = Objects.requireNonNull(hierarchy, "hierarchy");
    }

    public Progress progress(String nodeId, Set<String> discoveredLocationIds) {
        var node = hierarchy.node(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("unknown world hierarchy node: " + nodeId));
        Set<String> discovered = normalize(discoveredLocationIds);
        LinkedHashSet<String> discoverable = new LinkedHashSet<>();
        collectDiscoverableSites(node, discoverable);
        int discoveredCount = 0;
        for (String locationId : discoverable) {
            if (discovered.contains(locationId)) discoveredCount++;
        }
        return new Progress(
                node.nodeId(),
                node.displayName(),
                node.kind(),
                discoveredCount,
                discoverable.size(),
                !discoverable.isEmpty() && discoveredCount == discoverable.size()
        );
    }

    public Progress nearestProgressForLocation(String locationId, Set<String> discoveredLocationIds) {
        if (locationId == null || locationId.isBlank()) {
            throw new IllegalArgumentException("locationId is required");
        }
        String normalized = locationId.strip();
        var siteNode = hierarchy.nodes().stream()
                .filter(node -> normalized.equals(node.canonicalSiteId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("location is not represented in world hierarchy: " + normalized));
        String scopeId = siteNode.parentNodeId() == null ? siteNode.nodeId() : siteNode.parentNodeId();
        return progress(scopeId, discoveredLocationIds);
    }

    private void collectDiscoverableSites(
            CanonicalWorldHierarchyCatalogue.Node node,
            LinkedHashSet<String> output
    ) {
        if (node.canonicalSiteId() != null) output.add(node.canonicalSiteId());
        for (var child : hierarchy.childrenOf(node.nodeId())) collectDiscoverableSites(child, output);
    }

    private static Set<String> normalize(Set<String> values) {
        if (values == null || values.isEmpty()) return Set.of();
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) normalized.add(value.strip());
        }
        return Set.copyOf(normalized);
    }

    public record Progress(
            String nodeId,
            String displayName,
            CanonicalWorldHierarchyCatalogue.NodeKind kind,
            int discoveredCount,
            int discoverableCount,
            boolean complete
    ) {
        public Progress {
            if (nodeId == null || nodeId.isBlank()) throw new IllegalArgumentException("nodeId is required");
            if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("displayName is required");
            Objects.requireNonNull(kind, "kind");
            if (discoveredCount < 0 || discoverableCount < 0 || discoveredCount > discoverableCount) {
                throw new IllegalArgumentException("invalid exploration counts");
            }
        }

        public String compactLabel() {
            return displayName + " " + discoveredCount + "/" + discoverableCount;
        }
    }
}
