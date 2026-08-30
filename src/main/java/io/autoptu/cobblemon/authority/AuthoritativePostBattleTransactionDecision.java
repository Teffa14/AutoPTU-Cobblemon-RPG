package io.autoptu.cobblemon.authority;

public record AuthoritativePostBattleTransactionDecision(
        boolean accepted,
        boolean idempotent,
        boolean recoveryRequired,
        String reason,
        AuthoritativePostBattleTransaction transaction
) {
    public AuthoritativePostBattleTransactionDecision {
        reason = reason == null ? "" : reason;
    }

    public static AuthoritativePostBattleTransactionDecision committed(
            AuthoritativePostBattleTransaction transaction,
            boolean idempotent
    ) {
        return new AuthoritativePostBattleTransactionDecision(true, idempotent, false, "", transaction);
    }

    public static AuthoritativePostBattleTransactionDecision recoveryRequired(
            String reason,
            AuthoritativePostBattleTransaction transaction
    ) {
        return new AuthoritativePostBattleTransactionDecision(false, false, true, reason, transaction);
    }

    public static AuthoritativePostBattleTransactionDecision rejected(String reason) {
        return new AuthoritativePostBattleTransactionDecision(false, false, false, reason, null);
    }
}
