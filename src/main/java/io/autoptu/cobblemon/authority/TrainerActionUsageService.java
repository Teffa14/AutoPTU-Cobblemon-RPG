package io.autoptu.cobblemon.authority;

import java.time.Clock;
import java.util.Objects;

/**
 * Authority boundary for overworld Trainer Feature frequency usage.
 *
 * <p>Normal Minecraft actions do not pass through this service. Only a server-resolved PTU
 * Feature/action reaches this boundary. PTU turn/round action economy remains owned by
 * AutoPTU-Java and is rejected here rather than recreated in Minecraft.</p>
 */
public final class TrainerActionUsageService {
    private final TrainerActionUsageLedger ledger;
    private final Clock clock;

    public TrainerActionUsageService(TrainerActionUsageLedger ledger) {
        this(ledger, Clock.systemUTC());
    }

    TrainerActionUsageService(TrainerActionUsageLedger ledger, Clock clock) {
        this.ledger = Objects.requireNonNull(ledger, "ledger");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public TrainerActionUsageDecision reserveFeatureUse(
            CanonicalPlayerState player,
            CanonicalTrainerActionRule rule,
            String operationId,
            String canonicalContextId,
            long observedOverworldDay
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(rule, "rule");
        requireId("operationId", operationId);
        if (observedOverworldDay < 0) throw new IllegalArgumentException("observedOverworldDay must not be negative");

        ledger.observeOverworldDay(observedOverworldDay);

        if (!ownsFeature(player, rule.actionId())) {
            return TrainerActionUsageDecision.withoutReservation(
                    TrainerActionUsageDecision.Status.FEATURE_NOT_OWNED,
                    0,
                    rule.maxUses(),
                    ""
            );
        }
        if (rule.frequency() == TrainerActionFrequency.AT_WILL) {
            return TrainerActionUsageDecision.withoutReservation(
                    TrainerActionUsageDecision.Status.ALLOWED_AT_WILL,
                    0,
                    rule.maxUses(),
                    "at-will"
            );
        }
        if (rule.frequency().battleCoreOwned()) {
            return TrainerActionUsageDecision.withoutReservation(
                    TrainerActionUsageDecision.Status.BATTLE_CORE_OWNED,
                    0,
                    rule.maxUses(),
                    ""
            );
        }
        if (rule.frequency().requiresCanonicalContext()
                && (canonicalContextId == null || canonicalContextId.isBlank())) {
            return TrainerActionUsageDecision.withoutReservation(
                    TrainerActionUsageDecision.Status.CONTEXT_REQUIRED,
                    0,
                    rule.maxUses(),
                    ""
            );
        }

        return ledger.reserve(new TrainerActionUsageAttempt(
                operationId,
                player.playerId(),
                rule,
                canonicalContextId,
                observedOverworldDay,
                clock.millis()
        ));
    }

    public boolean commitFeatureUse(CanonicalPlayerState player, String reservationId) {
        Objects.requireNonNull(player, "player");
        return ledger.commit(requireId("reservationId", reservationId), player.playerId());
    }

    public boolean releaseFeatureUse(CanonicalPlayerState player, String reservationId) {
        Objects.requireNonNull(player, "player");
        return ledger.release(requireId("reservationId", reservationId), player.playerId());
    }

    private static boolean ownsFeature(CanonicalPlayerState player, String actionId) {
        return player.trainerFeatures().stream().anyMatch(feature -> feature.equalsIgnoreCase(actionId));
    }

    private static String requireId(String field, String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }
}
