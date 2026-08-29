package io.autoptu.cobblemon.authority;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Owner-scoped item quantities that are deliberately outside the active canonical bag. */
public record CanonicalItemStorageState(
        String playerId,
        Map<String, Integer> quantities,
        Set<String> appliedTransferIds,
        long revision
) {
    public CanonicalItemStorageState {
        if (playerId == null || playerId.isBlank()) throw new IllegalArgumentException("playerId must not be blank");
        LinkedHashMap<String, Integer> normalized = new LinkedHashMap<>();
        if (quantities != null) {
            quantities.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                if (entry.getKey() == null || entry.getKey().isBlank()) throw new IllegalArgumentException("templateId must not be blank");
                if (entry.getValue() == null || entry.getValue() <= 0) throw new IllegalArgumentException("stored quantity must be > 0");
                normalized.put(entry.getKey().strip(), entry.getValue());
            });
        }
        quantities = Map.copyOf(normalized);
        LinkedHashSet<String> receipts = new LinkedHashSet<>();
        if (appliedTransferIds != null) {
            for (String id : appliedTransferIds) {
                if (id == null || id.isBlank()) throw new IllegalArgumentException("applied transfer id must not be blank");
                receipts.add(id.strip());
            }
        }
        appliedTransferIds = Set.copyOf(receipts);
        if (revision < 0) throw new IllegalArgumentException("revision must be >= 0");
    }

    public int quantity(String templateId) {
        if (templateId == null || templateId.isBlank()) return 0;
        return quantities.getOrDefault(templateId.strip(), 0);
    }
}
