package io.autoptu.cobblemon.authority;

public record BattleOutcomeDecision(
        boolean accepted,
        boolean idempotent,
        String reason,
        BattleOutcomeCommit outcome
) {
    public BattleOutcomeDecision {
        reason = reason == null ? "" : reason;
    }

    public static BattleOutcomeDecision committed(BattleOutcomeCommit outcome) {
        return new BattleOutcomeDecision(true, false, "", outcome);
    }

    public static BattleOutcomeDecision idempotent(BattleOutcomeCommit outcome) {
        return new BattleOutcomeDecision(true, true, "", outcome);
    }

    public static BattleOutcomeDecision deny(String reason) {
        return new BattleOutcomeDecision(false, false, reason, null);
    }
}
