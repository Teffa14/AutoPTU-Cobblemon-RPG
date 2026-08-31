package io.autoptu.cobblemon.authority;

import java.util.Map;
import java.util.Optional;

/** Server-authored progression gates. Requirements are RPG state references, never client claims. */
public final class CanonicalProgressionGateCatalogue {
    public static final String CEDAR_BADGE_GATE_ID = "cedar_badge_gate";
    public static final String CEDAR_TRIAL_BADGE_ID = "cedar_trial_badge";

    private static final Map<String, Gate> GATES = Map.of(
            CEDAR_BADGE_GATE_ID,
            new Gate(CEDAR_BADGE_GATE_ID, "Cedar League Gate", CEDAR_TRIAL_BADGE_ID)
    );

    private CanonicalProgressionGateCatalogue() {}

    public static Optional<Gate> find(String gateId) {
        return Optional.ofNullable(GATES.get(gateId));
    }

    public record Gate(String id, String displayName, String requiredBadgeId) {}
}
