package io.autoptu.cobblemon.battlecore;

/**
 * Records upstream movement observed after the last compatibility-matrix promotion.
 *
 * <p>This watch is informational. Open or reference-only upstream work must never promote adapter
 * authority. Capability support changes only when the corresponding contract is merged, inspected,
 * and reflected in {@link CurrentUpstreamCompatibilityInspection}.</p>
 */
public final class CurrentUpstreamDevelopmentWatch {
    public static final String AUTOPTU_JAVA_MAIN_SHA = "f6115543da34bae91353c302a635913906656c2a";
    public static final String AUTOPTU_PYTHON_MAIN_SHA = "8951f4a174a1f7e644ce08f09acb5f486c27da57";
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
    public static final int AUTOPTU_JAVA_MERGED_EFFECT_ROLL_PENALTY_STATE_PR = 201;
    public static final int AUTOPTU_JAVA_MERGED_HARDENED_CRIT_EFFECT_BONUS_PR = 202;
    public static final int AUTOPTU_JAVA_MERGED_EFFECT_ROLL_RUNTIME_INPUTS_PR = 203;
    public static final int AUTOPTU_JAVA_MERGED_SECONDARY_STATUS_CONTRACT_PR = 204;
    public static final int AUTOPTU_JAVA_MERGED_SECONDARY_STATUS_APPLICATION_PR = 205;
    public static final int AUTOPTU_JAVA_MERGED_ACCURACY_ROLL_TRANSPORT_PR = 206;
    public static final int AUTOPTU_JAVA_OPEN_RUNTIME_SECONDARY_STATUS_BRIDGE_PR = 207;
    public static final String AUTOPTU_JAVA_OPEN_RUNTIME_SECONDARY_STATUS_BRIDGE_HEAD_SHA =
            "7ca7c0321d0f9d10f6df202b0634cfd2d22eab81";

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
        return "AutoPTU-Java PR #197 is merged on main and ports a deterministic move-special secondary-effect roll resolver against pinned Python oracle 16d228efa63aabecb67fa788959a359aac7f8f03. It covers modifier inputs including immutable-mind and effect-range blocks, Serene Grace, Stench, Firebrand, battle roll penalties, Mindbreak, Polished Shine, Brutal Training, effect-range bonuses, Stat Stratagem and Hardened. Later merged contracts derive those inputs from BattleRuntimeState and preserve the authoritative accuracy d20 in the shared move result, but no inspected live BattleRuntime consumer proves complete secondary-effect execution. Minecraft/Cobblemon must not calculate or supply final effect rolls.";
    }

    public static String mergedEffectRollTemporaryStateBoundary() {
        return "AutoPTU-Java PR #198 is merged on main at 3d9be13bfd3c89361e58c35e2df6a3265b57f93b and freezes Python _effect_roll temporary-state cleanup and short-circuit ordering for immutable_mind_block, effect_range_block and effect_range_bonus against oracle 16d228efa63aabecb67fa788959a359aac7f8f03. The contract adds exact-entry temporary-effect removal and preserves the Python rule that early blocks can prevent later cleanup. It does not give Minecraft authority to mutate those effects or calculate secondary outcomes independently.";
    }

    public static String mergedMoveEffectsTextBoundary() {
        return "AutoPTU-Java PR #199 is merged on main at 3825f32490c405a3d541c5eddf4b04097b4d1e69 and carries canonical move effects text in server-owned MoveSpec while preserving Python _effects_text_for precedence against oracle 16d228efa63aabecb67fa788959a359aac7f8f03. Direct MoveSpec effectsText wins exactly when non-empty; otherwise resolution may use a server-owned canonical fallback. Minecraft/Cobblemon must not send rules text or infer secondary-effect behavior from client or world data.";
    }

    public static String mergedEffectRollStatStratagemStacksBoundary() {
        return "AutoPTU-Java PR #200 is merged on main at 3dbfc85605c03e4f8e6aeb1f4195e0fdb412556a and preserves the Python _effect_roll rule that every matching stat_stratagem temporary effect with stat=spatk for a non-Status Ranged move adds the capped current SpAtk stage once. The adapter must not count Stat Stratagem effects or calculate this bonus itself.";
    }

    public static String mergedEffectRollPenaltyStateBoundary() {
        return "AutoPTU-Java PR #201 is merged on main at eb34ad6b3e2691e6192e8f489611bec0bb144f0d and ports the pinned Python BattleState._roll_penalty contract: all_roll_penalty entries stack in insertion order, expire only when currentRound is greater than expires_round, same-round entries remain, invalid amounts are ignored, int-like numeric values are accepted and the final penalty is clamped at zero. Minecraft/Cobblemon must not read, expire, sum or clamp roll-penalty state independently.";
    }

    public static String mergedHardenedCritEffectBonusBoundary() {
        return "AutoPTU-Java PR #202 is merged on main at 215967c224e3dcd73e06d47e9e4bad3153a96d8c and ports the pinned Python Hardened critical/effect-range bonus contract from server-owned semantic state. Hardened grants the bounded bonus only with the verified injury threshold and active Hardened state; Press On! may double that bonus only when its temporary state, Trainer Feature ownership and Intimidate rank contract are all satisfied. Minecraft/Cobblemon must not infer injuries, Press On!, Intimidate rank, Hardened expiry or effect-roll bonuses from world/client state.";
    }

    public static String mergedEffectRollRuntimeInputsBoundary() {
        return "AutoPTU-Java PR #203 is merged on main at f1fce54336f1a6a540e90eb1d3c5049a16e69336 and derives the already-ported move-special effect-roll inputs from canonical BattleRuntimeState. Ability suppression, Serene Grace, Stench, Firebrand, Polished Shine, Press On!, Intimidate rank, battle roll penalties, Mindbreak, Brutal Training, stacked Stat Stratagem, SpAtk Combat Stage, temporary effect blocks/bonuses and Hardened are read from server-owned state, including Python-compatible blocked-roll short-circuit ordering. This removes caller-supplied modifier authority, but Minecraft/Cobblemon still must not calculate effect rolls or reconstruct these modifiers from client, entity or world state.";
    }

    public static String mergedSecondaryStatusContractBoundary() {
        return "AutoPTU-Java PR #204 is merged on main at 7cd765e87fa4254789eb40e8d14f91e1251631ad and freezes generic text-driven secondary-status request semantics against pinned Python _generic_post_damage_from_text. It covers Burn, Poison, Paralysis, Freeze, Confusion, Flinch and Sleep request selection from canonical move effects text and an authoritative effect roll, preserving current Python normalization and verb-mapping quirks. The contract deliberately returns ordered status requests without applying them. Minecraft/Cobblemon must not parse effects text, choose requested statuses or treat this parser contract as a complete status lifecycle.";
    }

    public static String mergedSecondaryStatusApplicationBoundary() {
        return "AutoPTU-Java PR #205 is merged on main at d64d6417dc89c1aca878d0a8fd6b526921b8e193 and composes PR #204 requests with server-authoritative StatusApplicationResolution and BuiltinStatusApplicationHooks. The merged regression covers canonical Burn mutation, Immunity, Safeguard and Inner Focus prevention, ordered blocker events, and Flinch remaining/applied_round payload semantics. It adds no live POST_DAMAGE wiring. Minecraft/Cobblemon must not apply requested statuses, run prevention hooks, attach payloads, or reconstruct those outcomes locally.";
    }

    public static String mergedAccuracyRollTransportBoundary() {
        return "AutoPTU-Java PR #206 is merged on main at f6115543da34bae91353c302a635913906656c2a and freezes the Python contract that the original server-owned accuracy d20 is carried in the shared move-special result as roll and later read by _effect_roll. The merged overload preserves callers that do not yet transport the roll. BattleRuntime still does not supply accuracy.roll() through the live PRE_DAMAGE call, so Minecraft/Cobblemon must not generate, inject, infer or locally consume that roll for authoritative secondary-effect resolution.";
    }

    public static String openRuntimeSecondaryStatusBridgeBoundary() {
        return "AutoPTU-Java draft PR #207 at 7ca7c0321d0f9d10f6df202b0634cfd2d22eab81 composes the merged accuracy-roll transport, runtime-derived effect-roll modifiers, generic effects-text status parser and canonical status application/prevention behind a package-private runtime bridge. Its focused regressions prove Serene Grace can modify the server-owned d20 before a Burn threshold, Immunity still owns Poison prevention, and missing shared roll fails closed. The draft explicitly does not wire this bridge into BattleRuntime yet. Minecraft/Cobblemon must not call an equivalent bridge, parse status text, provide rolls or modifiers, or apply/prevent statuses locally.";
    }

    public static String pythonMainObservation() {
        return "AutoPTU Python main 8951f4a174a1f7e644ce08f09acb5f486c27da57 is the current read-only head inspected for this integration refresh. Its newest commits harden Career presentation and legacy-save rendering without replacing the frozen battle oracle. Java move-special parity remains explicitly pinned to Python oracle 16d228efa63aabecb67fa788959a359aac7f8f03. The oracle's generic POST_DAMAGE behavior delegates matching status requests through battle._apply_status rather than directly mutating status state, and _effect_roll reads the shared move-result roll. The later Python head is reference-only and does not promote Minecraft battle authority without a merged and live Java runtime contract.";
    }
}
