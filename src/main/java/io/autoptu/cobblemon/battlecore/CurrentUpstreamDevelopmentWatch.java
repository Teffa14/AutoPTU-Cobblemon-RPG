package io.autoptu.cobblemon.battlecore;

/**
 * Records upstream movement observed after the last compatibility-matrix promotion.
 *
 * <p>This watch is informational. Open or reference-only upstream work must never promote adapter
 * authority. Capability support changes only when the corresponding contract is merged, inspected,
 * and reflected in {@link CurrentUpstreamCompatibilityInspection}.</p>
 */
public final class CurrentUpstreamDevelopmentWatch {
    public static final String AUTOPTU_JAVA_MAIN_SHA = "fb93d3a4e6633d17a5a79f3095b141f887d4f258";
    public static final String AUTOPTU_PYTHON_MAIN_SHA = "ef0143b900ab671b1f0e061318278058b87fe403";
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
    public static final int AUTOPTU_JAVA_MERGED_RUNTIME_SECONDARY_STATUS_BRIDGE_PR = 207;
    public static final int AUTOPTU_JAVA_MERGED_LIVE_ACCURACY_ROLL_PR = 208;
    public static final int AUTOPTU_JAVA_MERGED_LIVE_DIRECT_SECONDARY_STATUS_PR = 209;
    public static final int AUTOPTU_JAVA_OPEN_AREA_SECONDARY_STATUS_PR = 210;
    public static final String AUTOPTU_JAVA_OPEN_AREA_SECONDARY_STATUS_HEAD_SHA =
            "ec977ede1f506e2c95278de2711bc2c1a4e68f99";

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

    public static boolean liveDirectSecondaryStatusMayBePromoted() {
        return true;
    }

    public static boolean areaSecondaryStatusMayBePromoted() {
        return false;
    }

    public static String effectRollResolverBoundary() {
        return "AutoPTU-Java PR #197 is merged on main and ports a deterministic move-special secondary-effect roll resolver against pinned Python oracle 16d228efa63aabecb67fa788959a359aac7f8f03. Later merged contracts derive those inputs from BattleRuntimeState, preserve the authoritative accuracy d20, compose generic secondary-status requests with canonical prevention, and PR #209 now consumes that chain in the preferred direct combatant-target runtime path. Overall effect-roll runtime remains PARTIAL because AoE and delayed paths are not covered by merged live wiring. Minecraft/Cobblemon must not calculate effect rolls or secondary outcomes for paths Java has not verified on main.";
    }

    public static String mergedEffectRollTemporaryStateBoundary() {
        return "AutoPTU-Java PR #198 is merged on main at 3d9be13bfd3c89361e58c35e2df6a3265b57f93b and freezes Python _effect_roll temporary-state cleanup and short-circuit ordering for immutable_mind_block, effect_range_block and effect_range_bonus against oracle 16d228efa63aabecb67fa788959a359aac7f8f03. The contract does not give Minecraft authority to mutate those effects or calculate secondary outcomes independently.";
    }

    public static String mergedMoveEffectsTextBoundary() {
        return "AutoPTU-Java PR #199 is merged on main at 3825f32490c405a3d541c5eddf4b04097b4d1e69 and carries canonical move effects text in server-owned MoveSpec while preserving Python _effects_text_for precedence against oracle 16d228efa63aabecb67fa788959a359aac7f8f03. Minecraft/Cobblemon must not send rules text or infer secondary-effect behavior from client or world data.";
    }

    public static String mergedEffectRollStatStratagemStacksBoundary() {
        return "AutoPTU-Java PR #200 is merged on main at 3dbfc85605c03e4f8e6aeb1f4195e0fdb412556a and preserves the Python _effect_roll rule that every matching stat_stratagem temporary effect with stat=spatk for a non-Status Ranged move adds the capped current SpAtk stage once. The adapter must not count Stat Stratagem effects or calculate this bonus itself.";
    }

    public static String mergedEffectRollPenaltyStateBoundary() {
        return "AutoPTU-Java PR #201 is merged on main at eb34ad6b3e2691e6192e8f489611bec0bb144f0d and ports the pinned Python BattleState._roll_penalty contract. Minecraft/Cobblemon must not read, expire, sum or clamp roll-penalty state independently.";
    }

    public static String mergedHardenedCritEffectBonusBoundary() {
        return "AutoPTU-Java PR #202 is merged on main at 215967c224e3dcd73e06d47e9e4bad3153a96d8c and ports the pinned Python Hardened critical/effect-range bonus contract from server-owned semantic state. Minecraft/Cobblemon must not infer injuries, Press On!, Intimidate rank, Hardened expiry or effect-roll bonuses from world/client state.";
    }

    public static String mergedEffectRollRuntimeInputsBoundary() {
        return "AutoPTU-Java PR #203 is merged on main at f1fce54336f1a6a540e90eb1d3c5049a16e69336 and derives move-special effect-roll inputs from canonical BattleRuntimeState. Ability, Trainer Feature, battle penalty, temporary-effect, Combat Stage and Hardened inputs remain server-owned. Minecraft/Cobblemon must not reconstruct these modifiers or calculate effect rolls.";
    }

    public static String mergedSecondaryStatusContractBoundary() {
        return "AutoPTU-Java PR #204 is merged on main at 7cd765e87fa4254789eb40e8d14f91e1251631ad and freezes generic text-driven secondary-status request semantics against pinned Python _generic_post_damage_from_text. The contract returns ordered status requests without applying them. Minecraft/Cobblemon must not parse effects text, choose requested statuses or treat this parser contract as a complete status lifecycle.";
    }

    public static String mergedSecondaryStatusApplicationBoundary() {
        return "AutoPTU-Java PR #205 is merged on main at d64d6417dc89c1aca878d0a8fd6b526921b8e193 and composes PR #204 requests with server-authoritative StatusApplicationResolution and BuiltinStatusApplicationHooks. Minecraft/Cobblemon must not apply requested statuses, run prevention hooks, attach payloads or reconstruct those outcomes locally.";
    }

    public static String mergedAccuracyRollTransportBoundary() {
        return "AutoPTU-Java PR #206 is merged on main at f6115543da34bae91353c302a635913906656c2a and freezes the Python contract that the original server-owned accuracy d20 is carried in the shared move-special result as roll and later read by _effect_roll. Minecraft/Cobblemon must not generate, inject or infer that roll.";
    }

    public static String mergedRuntimeSecondaryStatusBridgeBoundary() {
        return "AutoPTU-Java PR #207 is merged on main at d365642c74b43592073a7cc07bb3e011aaa503a9 and composes the authoritative shared accuracy roll, runtime-derived effect-roll modifiers, generic effects-text status parsing and canonical status application/prevention behind a package-private runtime bridge. Its regressions cover Serene Grace effect-roll modification, Immunity prevention and fail-closed behavior when the shared roll is absent. PR #209 now invokes this composition for the preferred direct combatant-target path. Minecraft/Cobblemon must still not call an equivalent bridge, parse status text, provide modifiers or apply/prevent statuses locally.";
    }

    public static String mergedLiveAccuracyRollBoundary() {
        return "AutoPTU-Java PR #208 is merged on main at 412ec8f82c7dd4cb89e58e4db80b3e9d957b5bb4 and passes the already-consumed authoritative accuracy d20 from BattleRuntime into MoveSpecialPreDamageResolution. The live PRE_DAMAGE shared result carries the same roll consumed by the pinned Python move-special effect-roll path. PR #208 closes roll transport only; PR #209 is the separate contract that consumes it for direct-target secondary statuses.";
    }

    public static String mergedLiveDirectSecondaryStatusBoundary() {
        return "AutoPTU-Java PR #209 is merged on main at fb93d3a4e6633d17a5a79f3095b141f887d4f258 and wires the generic secondary-status chain into the preferred authoritative single combatant-target runtime path. RuntimeMoveResolution builds a server-owned MoveSpecialHookRegistry from canonical move identity plus effective type/category, BattleRuntime supplies the real accuracy d20, POST_DAMAGE derives effect-roll inputs from BattleRuntimeState, parses canonical effects text, and delegates final application/prevention to StatusApplicationResolution. End-to-end regressions cover Serene Grace plus Burn and Immunity blocking Poison. This promotes only that direct live path. AoE and delayed paths remain explicitly outside PR #209, and complete status lifecycle remains PARTIAL. Minecraft/Cobblemon must consume authoritative events/state and must not reproduce the parser, modifier derivation, prevention, or status mutation locally.";
    }

    public static String openAreaSecondaryStatusBoundary() {
        return "AutoPTU-Java draft PR #210 at ec977ede1f506e2c95278de2711bc2c1a4e68f99 proposes live generic secondary-status execution for authoritative area targets by reusing RuntimeMoveSpecialHooks.standardRegistry per resolved target while the outer declaration retains single action/frequency spending. Its end-to-end regression covers guaranteed Poison across two Burst targets with Immunity blocking one target, and the observed Java Core Parity, Multi-Target Move Execution Contract, Move Special Secondary Status Parity, Move Special Execution Order Contract, Move Special POST Damage Contract and Battle RNG Ownership Contract are green on that draft head. The PR is still open and draft, so this is reference evidence only. Minecraft/Cobblemon must not promote AoE secondary outcomes, apply area statuses, reconstruct per-target RNG/status prevention, or infer that the contract will merge unchanged.";
    }

    public static String pythonMainObservation() {
        return "AutoPTU Python main ef0143b900ab671b1f0e061318278058b87fe403 is the current read-only head inspected for this integration refresh. Its latest change hardens Career ranked leaderboard rendering against malformed entries and does not replace the frozen battle oracle. Java battle parity remains explicitly pinned to Python oracle 16d228efa63aabecb67fa788959a359aac7f8f03, where generic POST_DAMAGE status requests delegate through battle._apply_status and _effect_roll reads the shared move-result roll. The later Python head is reference-only and cannot promote Minecraft battle authority without a merged Java runtime contract.";
    }
}
