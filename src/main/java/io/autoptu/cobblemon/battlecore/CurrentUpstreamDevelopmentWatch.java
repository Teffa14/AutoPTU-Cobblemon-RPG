package io.autoptu.cobblemon.battlecore;

/**
 * Records upstream movement observed after the last compatibility-matrix promotion.
 *
 * <p>This watch is informational. Open or reference-only upstream work must never promote adapter
 * authority. Capability support changes only when the corresponding contract is merged, inspected,
 * and reflected in {@link CurrentUpstreamCompatibilityInspection}.</p>
 */
public final class CurrentUpstreamDevelopmentWatch {
    public static final String AUTOPTU_JAVA_MAIN_SHA = "3825f32490c405a3d541c5eddf4b04097b4d1e69";
    public static final String AUTOPTU_PYTHON_MAIN_SHA = "fbeff1a0ba81d2f8dea2e395f21971cd9d756d77";
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
    public static final int AUTOPTU_JAVA_OPEN_EFFECT_ROLL_STAT_STRATAGEM_STACKS_PR = 200;
    public static final String AUTOPTU_JAVA_OPEN_EFFECT_ROLL_STAT_STRATAGEM_STACKS_HEAD_SHA =
            "89f1900e50b3e7c48925105d36083642ef1026b1";

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

    public static String openEffectRollStatStratagemStacksBoundary() {
        return "AutoPTU-Java draft PR #200 at 89f1900e50b3e7c48925105d36083642ef1026b1 fixes a parity gap in the resolver contract: Python _effect_roll iterates every matching stat_stratagem temporary effect with stat=spatk for a non-Status Ranged move and adds the capped current SpAtk stage once per matching entry. Java main at 3825f32490c405a3d541c5eddf4b04097b4d1e69 still models this as a single boolean application. The draft changes that input to an application count but remains open reference work and still excludes live runtime state derivation, so the adapter must not count Stat Stratagem effects or calculate this bonus itself.";
    }

    public static String pythonMainObservation() {
        return "AutoPTU Python main fbeff1a0ba81d2f8dea2e395f21971cd9d756d77 remains the current read-only head. The inspected _effect_roll implementation checks defender immutable_mind_block first, then attacker effect_range_block, then applies server-owned ability, Trainer Feature, penalty and temporary-effect modifiers. For eligible non-Status Ranged moves it loops every stat_stratagem entry whose stat is spatk and adds min(3, max(0, current SpAtk stage)) once per matching entry. _effects_text_for continues to prefer move.effects_text and otherwise uses canonical move content by move name. Java parity remains pinned to Python oracle 16d228efa63aabecb67fa788959a359aac7f8f03; later Career-only Python commits do not promote Minecraft battle authority.";
    }
}
