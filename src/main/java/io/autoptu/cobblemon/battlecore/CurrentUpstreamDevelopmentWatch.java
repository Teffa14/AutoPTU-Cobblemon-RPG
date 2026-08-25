package io.autoptu.cobblemon.battlecore;

/**
 * Records upstream movement observed after the last compatibility-matrix promotion.
 *
 * <p>This watch is informational. Open or reference-only upstream work must never promote adapter
 * authority. Capability support changes only when the corresponding contract is merged, inspected,
 * and reflected in {@link CurrentUpstreamCompatibilityInspection}.</p>
 */
public final class CurrentUpstreamDevelopmentWatch {
    public static final String AUTOPTU_JAVA_MAIN_SHA = "2c83099de0f558a6e387f39174c0223f8e1668e6";
    public static final String AUTOPTU_PYTHON_MAIN_SHA = "05363c11b0a174ef8ffee89e94ceb6273766f3d9";
    public static final int AUTOPTU_JAVA_MERGED_POST_DAMAGE_TIMING_PR = 189;
    public static final int AUTOPTU_JAVA_MERGED_REACTION_HANDOFF_PR = 190;
    public static final int AUTOPTU_JAVA_MERGED_LIVE_POST_DAMAGE_PR = 191;
    public static final int AUTOPTU_JAVA_MERGED_END_ACTION_BRIDGE_PR = 192;
    public static final int AUTOPTU_JAVA_OPEN_END_ACTION_ACCUMULATOR_PR = 193;
    public static final String AUTOPTU_JAVA_OPEN_END_ACTION_ACCUMULATOR_HEAD_SHA =
            "1f8b2d677f6c257aed3c1821199adeb549dc7fbb";

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
        return "AutoPTU-Java PR #192 is merged on main and adds the core-owned MoveSpecialEndActionResolution bridge. The bridge freezes Python's action-wide contract: defender is absent, the shared result starts from the last per-target result, hit is snapshotted before handler mutation, damage_dealt is total action damage, and END_ACTION preserves global-before-move-specific handler order. Draft PR #193 adds a package-private action accumulator with the Python no-target defaults and total-damage aggregation, and its current CI is green, but it explicitly does not wire END_ACTION into the live action/multi-target BattleRuntime layer. Minecraft must therefore keep END_ACTION fail-closed and must not dispatch it once per defender, aggregate damage itself, or synthesize the shared result.";
    }

    public static String pythonMainObservation() {
        return "AutoPTU Python main 05363c1 contains newer Career leaderboard hardening. The Java parity gate for move-special END_ACTION remains pinned to battle oracle 16d228efa63aabecb67fa788959a359aac7f8f03 and freezes defender=None, result=last_result, damage_dealt=total_damage_dealt, initial last_result={hit:false, immutable_mind:true}, and initial total_damage_dealt=0. Unrelated Python main movement does not widen adapter authority.";
    }
}
