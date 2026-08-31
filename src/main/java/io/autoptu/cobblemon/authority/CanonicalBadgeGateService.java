package io.autoptu.cobblemon.authority;

import java.util.Objects;
import java.util.Set;

/** Server-owned progression gate that consumes durable Trainer badge records read-only. */
public final class CanonicalBadgeGateService {
    public static final String CEDAR_LEAGUE_GATE_ID = "cedar-league-badge-gate";
    public static final String CEDAR_GYM_BADGE_ID = "cedar-gym-badge";

    private final CanonicalTrainerRecordQueryService trainerRecords;

    public CanonicalBadgeGateService(CanonicalTrainerRecordQueryService trainerRecords) {
        this.trainerRecords = Objects.requireNonNull(trainerRecords, "trainerRecords");
    }

    public Decision canPass(String playerId, String gateId) {
        if (playerId == null || playerId.isBlank()) {
            return Decision.denied("authenticated Trainer identity is required");
        }
        if (!CEDAR_LEAGUE_GATE_ID.equals(gateId)) {
            return Decision.denied("unknown authored gate");
        }
        var snapshot = trainerRecords.inspect(playerId.trim());
        Set<String> required = Set.of(CEDAR_GYM_BADGE_ID);
        if (!snapshot.badgeIds().containsAll(required)) {
            return new Decision(false, "Cedar Gym Badge required", required, snapshot.badgeIds(), snapshot.revision());
        }
        return new Decision(true, "badge requirement satisfied", required, snapshot.badgeIds(), snapshot.revision());
    }

    public record Decision(
            boolean allowed,
            String reason,
            Set<String> requiredBadgeIds,
            Set<String> ownedBadgeIds,
            long trainerRecordRevision
    ) {
        public Decision {
            reason = Objects.requireNonNull(reason, "reason");
            requiredBadgeIds = Set.copyOf(requiredBadgeIds == null ? Set.of() : requiredBadgeIds);
            ownedBadgeIds = Set.copyOf(ownedBadgeIds == null ? Set.of() : ownedBadgeIds);
        }

        static Decision denied(String reason) {
            return new Decision(false, reason, Set.of(), Set.of(), -1L);
        }
    }
}
