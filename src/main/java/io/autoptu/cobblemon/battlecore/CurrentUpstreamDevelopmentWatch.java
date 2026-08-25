package io.autoptu.cobblemon.battlecore;

/**
 * Records upstream movement observed after the last compatibility-matrix promotion.
 *
 * <p>This watch is informational. Open or reference-only upstream work must never promote adapter
 * authority. Capability support changes only when the corresponding contract is merged, inspected,
 * and reflected in {@link CurrentUpstreamCompatibilityInspection}.</p>
 */
public final class CurrentUpstreamDevelopmentWatch {
    public static final String AUTOPTU_JAVA_MAIN_SHA = "4148255b038f85902feb781413f163c7b7cf3799";
    public static final String AUTOPTU_PYTHON_MAIN_SHA = "c9b9b372b6a86546679188df97aa5bde27ab066c";
    public static final int AUTOPTU_JAVA_MERGED_POST_DAMAGE_TIMING_PR = 189;
    public static final int AUTOPTU_JAVA_MERGED_REACTION_HANDOFF_PR = 190;
    public static final int AUTOPTU_JAVA_MERGED_LIVE_POST_DAMAGE_PR = 191;
    public static final int AUTOPTU_JAVA_MERGED_END_ACTION_BRIDGE_PR = 192;
    public static final int AUTOPTU_JAVA_MERGED_END_ACTION_ACCUMULATOR_PR = 193;
    public static final int AUTOPTU_JAVA_MERGED_END_ACTION_FINALIZATION_PR = 194;
    public static final int AUTOPTU_JAVA_MERGED_TARGET_RESULT_TRANSPORT_PR = 195;
    public static final int AUTOPTU_JAVA_OPEN_END_ACTION_ORACLE_PR = 196;
    public static final String AUTOPTU_JAVA_OPEN_END_ACTION_ORACLE_HEAD_SHA =
            "0cbf8bb3082372cb1ae487b136657ad17421bdd7";

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
        return "AutoPTU-Java PRs #192, #193, #194 and #195 are merged on main and freeze END_ACTION bridge, declaration accumulation, finalization and package-private per-target result transport contracts. MoveSpecialActionFinalization composes ordered per-target results, keeps the last target result, sums applied damage and dispatches END_ACTION exactly once, including Python-compatible empty-target defaults. PR #195 deliberately keeps MoveSpecialTargetResult package-private and leaves AppliedActionResult as the public Minecraft-facing result. No authoritative BattleRuntime call site was added by #195, so Minecraft/Cobblemon must not aggregate target results, dispatch END_ACTION or infer move effects from this transport.";
    }

    public static String openEndActionOracleBoundary() {
        return "AutoPTU-Java draft PR #196 strengthens only the read-only Python END_ACTION oracle: it asserts that last_result replacement and total_damage_dealt accumulation occur in the same target loop and finish before the single END_ACTION dispatch. It activates no Java runtime behavior and therefore cannot promote Minecraft/Cobblemon authority.";
    }

    public static String pythonMainObservation() {
        return "AutoPTU Python main c9b9b37 is one Career Web rival-identity presentation commit ahead of the prior watch pin and does not change the frozen battle-oracle files. The battle-oracle contract remains PRE_DAMAGE, POST_DAMAGE and END_ACTION, with END_ACTION occurring once after target processing using the declaration-level final result and accumulated applied damage.";
    }
}
