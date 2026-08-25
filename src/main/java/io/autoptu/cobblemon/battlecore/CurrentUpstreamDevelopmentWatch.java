package io.autoptu.cobblemon.battlecore;

/**
 * Records upstream movement observed after the last compatibility-matrix promotion.
 *
 * <p>This watch is informational. Open or reference-only upstream work must never promote adapter
 * authority. Capability support changes only when the corresponding contract is merged, inspected,
 * and reflected in {@link CurrentUpstreamCompatibilityInspection}.</p>
 */
public final class CurrentUpstreamDevelopmentWatch {
    public static final String AUTOPTU_JAVA_MAIN_SHA = "3dbfc85605c03e4f8e6aeb1f4195e0fdb412556a";
    public static final String AUTOPTU_PYTHON_MAIN_SHA = "d5f2833837f6096673ad64920f239691b4481464";
    public static final int AUTOPTU_JAVA_MERGED_POST_DAMAGE_TIMING_PR = 189;
    public static final int AUTOPTU_JAVA_MERGED_REACTION_HANDOFF_PR = 190;
    public static final int AUTOPTU_JAVA_MERGED_LIVE_POST_DAMAGE_PR = 191;
    public static final int AUTOPTU_JAVA_MERGED_END_ACTION_BRIDGE_PR = 192;
    public static final int AUTOPTU_JAVA_MERGED_END_ACTION_ACCUMULATOR_PR = 193;
    public static final int AUTOPTU_JAVA_MERGED_END_ACTION_FINALIZATION_PR = 194;
    public static final int AUTOPTU_JAVA_MERGED_TARGET_RESULT_TRANSPORT_PR = 195;
    public static final int AUTOPTU_JAVA_MERGED_END_ACTION_ORACLE_PR = 196;
    public static final int AUTOPTU_JAVA_MERGED_EFFECT_ROLL_RESOLVER_PR = 197;
    public static final int AUTOPTU_JAVA_MERGED_EFFECT_ROLL_TEMP_STATE_PR = 198;
    public static final int AUTOPTU_JAVA_MERGED_MOVE_EFFECTS_TEXT_PR = 199;
    public static final int AUTOPTU_JAVA_MERGED_EFFECT_ROLL_STAT_STRATAGEM_STACKS_PR = 200;
    public static final int AUTOPTU_JAVA_OPEN_EFFECT_ROLL_PENALTY_STATE_PR = 201;
    public static final String AUTOPTU_JAVA_OPEN_EFFECT_ROLL_PENALTY_STATE_HEAD_SHA =
            "5a7a34aa0cf072c48cdd9e3e7a9a83be6f9b9b03";

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

    public static String mergedEffectRollTemporaryStateBoundary() {
        return "AutoPTU-Java PR #198 is merged on main at 3d9be13bfd3c89361e58c35e2df6a3265b57f93b and freezes Python _effect_roll temporary-state cleanup and short-circuit ordering for immutable_mind_block, effect_range_block and effect_range_bonus against oracle 16d228efa63aabecb67fa788959a359aac7f8f03. The contract adds exact-entry temporary-effect removal and preserves the Python rule that early blocks can prevent later cleanup. It does not wire effect-roll resolution into BattleRuntime or derive ability, Trainer Feature, penalty, Hardened, move-content or other runtime inputs. Minecraft/Cobblemon must continue to fail closed and must not perform those mutations or effect-roll calculations independently.";
    }

    public static String mergedMoveEffectsTextBoundary() {
        return "AutoPTU-Java PR #199 is merged on main at 3825f32490c405a3d541c5eddf4b04097b4d1e69 and carries canonical move effects text in server-owned MoveSpec while preserving Python _effects_text_for precedence against oracle 16d228efa63aabecb67fa788959a359aac7f8f03. Direct MoveSpec effectsText wins exactly when non-empty; otherwise resolution may use a server-owned canonical fallback. This closes a content dependency for generic move-special logic but does not provide live effect-roll runtime authority. Minecraft/Cobblemon must not send rules text or infer Stench, Firebrand or secondary-effect behavior from client/world data.";
    }

    public static String mergedEffectRollStatStratagemStacksBoundary() {
        return "AutoPTU-Java PR #200 is merged on main at 3dbfc85605c03e4f8e6aeb1f4195e0fdb412556a and preserves the Python _effect_roll rule that every matching stat_stratagem temporary effect with stat=spatk for a non-Status Ranged move adds the capped current SpAtk stage once. This closes the prior single-boolean parity gap in the resolver contract. It still does not derive live Stat Stratagem state or invoke secondary-effect resolution from BattleRuntime, so the adapter must not count Stat Stratagem effects or calculate this bonus itself.";
    }

    public static String openEffectRollPenaltyStateBoundary() {
        return "AutoPTU-Java draft PR #201 at 5a7a34aa0cf072c48cdd9e3e7a9a83be6f9b9b03 ports the pinned Python BattleState._roll_penalty contract: all_roll_penalty entries stack in insertion order, expire only when currentRound is greater than expires_round, same-round entries remain, invalid amounts are ignored, int-like numeric values are accepted and the final penalty is clamped at zero. The PR is open reference work for a later authoritative _effect_roll runtime input factory and does not wire live move-special consumers. Minecraft/Cobblemon must not read, expire, sum or clamp roll-penalty state independently.";
    }

    public static String pythonMainObservation() {
        return "AutoPTU Python main d5f2833837f6096673ad64920f239691b4481464 is the current read-only head inspected for this integration refresh. Java effect-roll parity remains explicitly pinned to Python oracle 16d228efa63aabecb67fa788959a359aac7f8f03, including _effect_roll modifier semantics and the _roll_penalty behavior exercised by Java PR #201. The later Python head is reference-only and does not promote Minecraft battle authority without a merged Java runtime contract.";
    }
}
