package io.autoptu.cobblemon.battlecore;

/**
 * Records upstream movement observed after the last compatibility-matrix promotion.
 *
 * <p>This watch is informational. Open or reference-only upstream work must never promote adapter
 * authority. Capability support changes only when the corresponding contract is merged, inspected,
 * and reflected in {@link CurrentUpstreamCompatibilityInspection}.</p>
 */
public final class CurrentUpstreamDevelopmentWatch {
    public static final String AUTOPTU_JAVA_MAIN_SHA = "0566685bf0d84b2d41bbf0bb75185c6723dd0c44";
    public static final String AUTOPTU_PYTHON_MAIN_SHA = "bae915ff074e1c39d05dd2fa7ab88655bf92ab60";
    public static final int AUTOPTU_JAVA_MERGED_POST_DAMAGE_TIMING_PR = 189;
    public static final int AUTOPTU_JAVA_MERGED_REACTION_HANDOFF_PR = 190;
    public static final int AUTOPTU_JAVA_MERGED_LIVE_POST_DAMAGE_PR = 191;
    public static final int AUTOPTU_JAVA_MERGED_END_ACTION_BRIDGE_PR = 192;
    public static final int AUTOPTU_JAVA_MERGED_END_ACTION_ACCUMULATOR_PR = 193;
    public static final int AUTOPTU_JAVA_MERGED_END_ACTION_FINALIZATION_PR = 194;
    public static final int AUTOPTU_JAVA_MERGED_TARGET_RESULT_TRANSPORT_PR = 195;
    public static final int AUTOPTU_JAVA_MERGED_END_ACTION_ORACLE_PR = 196;
    public static final int AUTOPTU_JAVA_MERGED_EFFECT_ROLL_RESOLVER_PR = 197;
    public static final int AUTOPTU_JAVA_OPEN_EFFECT_ROLL_TEMP_STATE_PR = 198;
    public static final String AUTOPTU_JAVA_OPEN_EFFECT_ROLL_TEMP_STATE_HEAD_SHA =
            "e512dfbb9161f10afc82132b61ea21f0bdb3dbce";

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
        return "AutoPTU-Java PRs #192, #193, #194, #195 and #196 are merged on main and freeze END_ACTION bridge, declaration accumulation, finalization, package-private per-target result transport and the Python target-loop aggregation oracle. MoveSpecialActionFinalization composes ordered per-target results, keeps the last target result, sums applied damage and dispatches END_ACTION exactly once, including Python-compatible empty-target defaults. PR #196 proves last_result replacement and total_damage_dealt accumulation share the target loop and complete before END_ACTION. No authoritative BattleRuntime call site invokes the finalization path, so Minecraft/Cobblemon must not aggregate target results, dispatch END_ACTION or infer move effects from these contracts.";
    }

    public static boolean effectRollRuntimeMayBePromoted() {
        return false;
    }

    public static String effectRollResolverBoundary() {
        return "AutoPTU-Java PR #197 is merged on main and ports a deterministic move-special secondary-effect roll resolver against pinned Python oracle 16d228efa63aabecb67fa788959a359aac7f8f03. It covers modifier inputs including immutable-mind and effect-range blocks, Serene Grace, Stench, Firebrand, battle roll penalties, Mindbreak, Polished Shine, Brutal Training, effect-range bonuses, Stat Stratagem and Hardened. The merged contract is resolver-only: authoritative runtime state derivation and concrete move-special consumers remain absent, so Minecraft/Cobblemon must not calculate or supply final effect rolls.";
    }

    public static String openEffectRollTemporaryStateBoundary() {
        return "AutoPTU-Java draft PR #198 at e512dfbb9161f10afc82132b61ea21f0bdb3dbce freezes Python _effect_roll temporary-state cleanup and short-circuit ordering for immutable_mind_block, effect_range_block and effect_range_bonus against oracle 16d228efa63aabecb67fa788959a359aac7f8f03. It is open reference work, not adapter authority. Full runtime derivation of ability, Trainer Feature, penalty and Hardened inputs remains upstream follow-up.";
    }

    public static String pythonMainObservation() {
        return "AutoPTU Python main bae915ff074e1c39d05dd2fa7ab88655bf92ab60 remains the current read-only head. The inspected _effect_roll implementation still checks defender immutable_mind_block first, then attacker effect_range_block, then applies server-owned ability, Trainer Feature, penalty and temporary-effect modifiers while removing only expired entries reached by that control flow. The Java effect-roll parity work remains pinned to Python oracle 16d228efa63aabecb67fa788959a359aac7f8f03; unrelated later Career commits do not promote Minecraft battle authority.";
    }
}
