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
    public static final String AUTOPTU_PYTHON_MAIN_SHA = "0a2b4e924e11567fa0e3cc0e4f4045ab141f7163";
    public static final int AUTOPTU_JAVA_MERGED_POST_DAMAGE_TIMING_PR = 189;
    public static final int AUTOPTU_JAVA_MERGED_REACTION_HANDOFF_PR = 190;
    public static final int AUTOPTU_JAVA_MERGED_LIVE_POST_DAMAGE_PR = 191;

    private CurrentUpstreamDevelopmentWatch() {}

    public static boolean postDamageRuntimeMayBePromoted() {
        return true;
    }

    public static String postDamageRuntimeBoundary() {
        return "AutoPTU-Java PR #191 is merged on main and invokes RuntimeMoveSpecialPostDamageApplication from BattleRuntime only after the authoritative move outcome commits HP/history state. The shared PRE result is handed through defender reactions and the final pre-HP adjustment before POST_DAMAGE. This promotes Java-owned POST_DAMAGE execution only; END_ACTION and complete move-special coverage remain upstream-owned and incomplete.";
    }

    public static String pythonMainObservation() {
        return "AutoPTU Python main 0a2b4e9 contains Career narrative-cache hardening; current battle-oracle behavior still executes move-special phases PRE_DAMAGE, POST_DAMAGE and END_ACTION with POST_DAMAGE observing already-applied damage_dealt and shared mutable result state.";
    }
}
