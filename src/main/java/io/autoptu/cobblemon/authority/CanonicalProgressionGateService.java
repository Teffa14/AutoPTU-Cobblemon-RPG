package io.autoptu.cobblemon.authority;

import java.util.Objects;

/** Read-only authorization boundary for server-authored RPG progression gates. */
public final class CanonicalProgressionGateService {
    private final CanonicalTrainerRecordQueryService records;

    public CanonicalProgressionGateService(CanonicalTrainerRecordQueryService records) {
        this.records = Objects.requireNonNull(records, "records");
    }

    public Decision canPass(String playerId, String gateId) {
        var gate = CanonicalProgressionGateCatalogue.find(gateId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown progression gate: " + gateId));
        var snapshot = records.inspect(playerId);
        boolean allowed = snapshot.badgeIds().contains(gate.requiredBadgeId());
        return new Decision(gate, allowed, allowed ? "authorized" : "missing badge " + gate.requiredBadgeId(), snapshot.revision());
    }

    public record Decision(
            CanonicalProgressionGateCatalogue.Gate gate,
            boolean allowed,
            String reason,
            long trainerRecordRevision
    ) {}
}
