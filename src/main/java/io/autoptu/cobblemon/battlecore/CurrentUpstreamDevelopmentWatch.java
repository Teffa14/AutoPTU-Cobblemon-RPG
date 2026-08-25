package io.autoptu.cobblemon.battlecore;

/**
 * Records upstream movement observed after the last compatibility-matrix promotion.
 *
 * <p>This watch is informational. Open or reference-only upstream work must never promote adapter
 * authority. Capability support changes only when the corresponding contract is merged, inspected,
 * and reflected in {@link CurrentUpstreamCompatibilityInspection}.</p>
 */
public final class CurrentUpstreamDevelopmentWatch {
    public static final String AUTOPTU_JAVA_MAIN_SHA = "3d9be13bfd3c89361e58c35e2df6a3265b57f93b";
    public static final String AUTOPTU_PYTHON_MAIN_SHA = "e6aa730a77e25142f5308eaa3a738dc66ba34bbb";
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
    public static final int AUTOPTU_JAVA_OPEN_MOVE_EFFECTS_TEXT_PR = 199;
    public static final String AUTOPTU_JAVA_OPEN_MOVE_EFFECTS_TEXT_HEAD_SHA =
            "2e6efccc9dc5ac373a0945fcf60c1dddbc025833";

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

    public static String openMoveEffectsTextBoundary() {
        return "AutoPTU-Java draft PR #199 at 2e6efccc9dc5ac373a0945fcf60c1dddbc025833 carries canonical move effects text in server-owned MoveSpec and ports Python _effects_text_for precedence against oracle 16d228efa63aabecb67fa788959a359aac7f8f03. The fallback is explicitly server-owned canonical content, not client or Minecraft-supplied rule text. The PR is open reference work and does not provide live effect-roll runtime authority, so the adapter must not send rules text or infer Stench, Firebrand or secondary-effect behavior from Minecraft data.";
    }

    public static String pythonMainObservation() {
        return "AutoPTU Python main e6aa730a77e25142f5308eaa3a738dc66ba34bbb remains the current read-only head. The inspected _effect_roll implementation still checks defender immutable_mind_block first, then attacker effect_range_block, then applies server-owned ability, Trainer Feature, penalty and temporary-effect modifiers while removing only expired entries reached by that control flow. _effects_text_for continues to prefer move.effects_text and otherwise uses canonical move content by move name. Java parity remains pinned to Python oracle 16d228efa63aabecb67fa788959a359aac7f8f03; later Career-only commits do not promote Minecraft battle authority.";
    }
}
