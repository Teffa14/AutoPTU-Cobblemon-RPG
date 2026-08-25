package io.autoptu.cobblemon.battlecore;

/**
 * Records upstream movement observed after the last compatibility-matrix promotion.
 *
 * <p>This watch is informational. Open or reference-only upstream work must never promote adapter
 * authority. Capability support changes only when the corresponding contract is merged, inspected,
 * and reflected in {@link CurrentUpstreamCompatibilityInspection}.</p>
 */
public final class CurrentUpstreamDevelopmentWatch {
    public static final String AUTOPTU_JAVA_MAIN_SHA = "b0dc8cc2fc6fba5d1fa3799485545d0c48b6f18a";
    public static final String AUTOPTU_PYTHON_MAIN_SHA = "b7f8fbba1221222a61af2fe6a23d047d8b61bcbb";
    public static final int AUTOPTU_JAVA_MERGED_POST_DAMAGE_TIMING_PR = 189;
    public static final int AUTOPTU_JAVA_MERGED_REACTION_HANDOFF_PR = 190;
    public static final int AUTOPTU_JAVA_MERGED_LIVE_POST_DAMAGE_PR = 191;
    public static final int AUTOPTU_JAVA_OPEN_END_ACTION_BRIDGE_PR = 192;

    private CurrentUpstreamDevelopmentWatch() {}

    public static boolean postDamageRuntimeMayBePromoted() {
        return true;
    }

    public static boolean endActionRuntimeMayBePromoted() {
        return false;
    }

    public static String postDamageRuntimeBoundary() {
        return "AutoPTU-Java PR #191 is merged on main and invokes RuntimeMoveSpecialPostDamageApplication from BattleRuntime only after the authoritative move outcome commits HP/history state. The shared PRE result is handed through defender reactions and the final pre-HP adjustment before POST_DAMAGE. This promotes Java-owned POST_DAMAGE execution only; END_ACTION and complete move-special coverage remain upstream-owned and incomplete.";
    }

    public static String endActionDevelopmentBoundary() {
        return "AutoPTU-Java PR #192 is open and draft. It adds a runtime-facing END_ACTION bridge preserving Python's action-wide contract: defender is absent, the shared result is the last per-target result, hit is snapshotted before handler mutation, and damage_dealt is total action damage. The PR explicitly does not wire END_ACTION into the action/multi-target runtime, and its head currently has failing workflow checks. Minecraft must therefore keep END_ACTION fail-closed and must not dispatch it once per defender or approximate action-wide aggregation.";
    }

    public static String pythonMainObservation() {
        return "AutoPTU Python main b7f8fbba contains Career save-isolation hardening; current battle-oracle behavior still exposes the action-wide move-special END_ACTION contract used by the Java parity gate: defender=None, result=last_result and damage_dealt=total_damage_dealt. The adapter continues to rely on the frozen battle-oracle contract rather than unrelated Career changes on Python main.";
    }
}
