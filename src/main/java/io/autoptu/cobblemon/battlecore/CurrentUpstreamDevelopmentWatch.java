package io.autoptu.cobblemon.battlecore;

/**
 * Records upstream movement observed after the last compatibility-matrix promotion.
 *
 * <p>This watch is informational. Open or reference-only upstream work must never promote adapter
 * authority. Capability support changes only when the corresponding contract is merged, inspected,
 * and reflected in {@link CurrentUpstreamCompatibilityInspection}.</p>
 */
public final class CurrentUpstreamDevelopmentWatch {
    public static final String AUTOPTU_JAVA_MAIN_SHA = "10fd20bfd513898a6f8f157a9b469db993444974";
    public static final String AUTOPTU_PYTHON_MAIN_SHA = "d246c3ac15290a3f661e69c75710f46e386ec871";
    public static final int AUTOPTU_JAVA_MERGED_POST_DAMAGE_TIMING_PR = 189;
    public static final int AUTOPTU_JAVA_MERGED_REACTION_HANDOFF_PR = 190;
    public static final int AUTOPTU_JAVA_MERGED_LIVE_POST_DAMAGE_PR = 191;
    public static final int AUTOPTU_JAVA_MERGED_END_ACTION_BRIDGE_PR = 192;
    public static final int AUTOPTU_JAVA_MERGED_END_ACTION_ACCUMULATOR_PR = 193;
    public static final int AUTOPTU_JAVA_MERGED_END_ACTION_FINALIZATION_PR = 194;

    private CurrentUpstreamDevelopmentWatch() {}

    public static boolean postDamageRuntimeMayBePromoted() {
        return true;
    }

    public static String postDamageRuntimeBoundary() {
        return "AutoPTU-Java PR #191 is merged on main and invokes RuntimeMoveSpecialPostDamageApplication from BattleRuntime only after the authoritative move outcome commits HP/history state. The shared PRE result is handed through defender reactions and the final pre-HP adjustment before POST_DAMAGE. This promotes Java-owned POST_DAMAGE execution only; complete move-special coverage remains upstream-owned and incomplete.";
    }

    public static boolean endActionRuntimeMayBePromoted() {
        return false;
    }

    public static String endActionRuntimeBoundary() {
        return "AutoPTU-Java PRs #192, #193 and #194 are merged on main and freeze END_ACTION bridge, declaration accumulator and finalization contracts. MoveSpecialActionFinalization composes ordered per-target POST_DAMAGE results, keeps the last target result, sums applied damage and dispatches END_ACTION exactly once, including Python-compatible empty-target defaults. No authoritative BattleRuntime call site currently invokes that finalization seam, so Minecraft/Cobblemon must not dispatch END_ACTION or infer move effects from it.";
    }

    public static String pythonMainObservation() {
        return "AutoPTU Python main d246c3a contains Career sponsor-history work; the frozen battle-oracle contract remains PRE_DAMAGE, POST_DAMAGE and END_ACTION, with END_ACTION occurring once after target processing using the declaration-level final result and accumulated applied damage.";
    }
}
