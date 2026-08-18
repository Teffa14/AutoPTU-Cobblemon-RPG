package io.autoptu.cobblemon.authority;

public record BattleSnapshotDecision(boolean allowed, String reason, BattleAuthoritySnapshot snapshot) {
    public BattleSnapshotDecision {
        reason = reason == null ? "" : reason;
    }

    public static BattleSnapshotDecision allow(BattleAuthoritySnapshot snapshot) {
        return new BattleSnapshotDecision(true, "", snapshot);
    }

    public static BattleSnapshotDecision deny(String reason) {
        return new BattleSnapshotDecision(false, reason, null);
    }
}
