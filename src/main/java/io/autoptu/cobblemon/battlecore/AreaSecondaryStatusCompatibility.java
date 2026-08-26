package io.autoptu.cobblemon.battlecore;

import java.util.Set;

/**
 * Bounded compatibility gate for authoritative secondary-effect paths in AutoPTU-Java. This class
 * records integration authority only; it does not implement PTU rules in Minecraft.
 */
public final class AreaSecondaryStatusCompatibility {
    public static final String AUTOPTU_JAVA_MAIN_SHA =
            "6b7ceb94001bb4228db81500bedba378ba2b6335";
    public static final String AUTOPTU_PYTHON_MAIN_SHA =
            "b4f3d895bca569e69bf50756694eb65c74e07de7";
    public static final String PINNED_PYTHON_BATTLE_ORACLE_SHA =
            "16d228efa63aabecb67fa788959a359aac7f8f03";
    public static final int MERGED_AREA_SECONDARY_STATUS_PR = 210;
    public static final int MERGED_SECONDARY_COMBAT_STAGE_PARSER_PR = 211;
    public static final int MERGED_SECONDARY_COMBAT_STAGE_APPLICATION_PR = 212;
    public static final int MERGED_ACCURACY_EVASION_COMBAT_STAGE_CONTRACT_PR = 213;
    public static final int MERGED_SEVEN_COMBAT_STAGE_STATE_PR = 214;
    public static final int OPEN_SEVEN_COMBAT_STAGE_HOOKS_PR = 215;
    public static final String OPEN_SEVEN_COMBAT_STAGE_HOOKS_HEAD_SHA =
            "a977d4e55e0f4e84cb3e86afda401be5425ab97b";

    private static final Set<UpstreamCompatibilityMatrix.Capability> DEPENDENCIES = Set.of(
            UpstreamCompatibilityMatrix.Capability.CORE_TARGETING,
            UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE,
            UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE,
            UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE,
            UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR,
            UpstreamCompatibilityMatrix.Capability.ABILITIES,
            UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS,
            UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK);

    private AreaSecondaryStatusCompatibility() {}

    public static Set<UpstreamCompatibilityMatrix.Capability> dependencies() {
        return DEPENDENCIES;
    }

    public static boolean authoritativeAreaSecondaryStatusMayBeProjected() {
        return DEPENDENCIES.stream().allMatch(UpstreamCompatibilityMatrix::mayProjectAuthoritativeBehavior);
    }

    public static boolean secondaryCombatStageMayBeProjected() {
        return false;
    }

    public static boolean accuracyEvasionCombatStageMayBeProjected() {
        return false;
    }

    public static boolean delayedSecondaryStatusMayBeProjected() {
        return false;
    }

    public static String mergedAreaBoundary() {
        return "AutoPTU-Java PR #210 is merged on main. BattleRuntime supplies RuntimeMoveSpecialHooks.standardRegistry for each authoritative area target while the outer declaration retains one action/frequency spend. "
                + "RuntimeMultiTargetSecondaryStatusIntegrationTest verifies two Burst targets, Poison application to one target, Immunity status_block on the other, stable authoritative target ids, and exactly one declaration-level STANDARD/frequency consumption. "
                + "Projection is permitted only while every declared upstream dependency remains non-BLOCKING. Minecraft/Cobblemon must not calculate per-target effect rolls, parse effects text, choose statuses, run prevention hooks, or mutate status state.";
    }

    public static String combatStageBoundary() {
        return "AutoPTU-Java PR #211 is merged and freezes Python-compatible generic secondary Combat Stage parsing into ordered semantic stage-change requests. "
                + "PR #212 is merged and composes ATK/DEF/SPATK/SPDEF/SPD requests with authoritative CombatStageMutationService, preserving prevention, reflection, clamping and post-apply reactions, but still does not wire that application boundary into live BattleRuntime move execution. "
                + "PR #213 is merged and freezes the pinned Python contract that Accuracy and Evasion use the same dynamic Combat Stage read/write path, -6..+6 clamp and hook-context stat forwarding as other stages. "
                + "PR #214 is merged on main at 6b7ceb94001bb4228db81500bedba378ba2b6335 and introduces canonical seven-stage server-owned state while retaining five-stat CombatStat compatibility for deterministic combat arithmetic. "
                + "Draft PR #215 at a977d4e55e0f4e84cb3e86afda401be5425ab97b migrates CombatStageMutationService and prevention/post-apply hook contexts to CombatStageStat, including Accuracy/Evasion mutation, prevention and reflection, but it remains open and still does not establish live BattleRuntime secondary Combat Stage execution. "
                + "Minecraft/Cobblemon therefore must not parse stage text, apply any secondary stage changes, evaluate prevention/reflection, or synthesize semantic stage events until the seven-stage hook migration and live runtime path are merged and verified.";
    }

    public static String pythonOracleObservation() {
        return "AutoPTU Python main b4f3d895bca569e69bf50756694eb65c74e07de7 changes Career local-save validation and does not replace the battle oracle. "
                + "Battle parity remains pinned to AutoPTU 16d228efa63aabecb67fa788959a359aac7f8f03. The frozen oracle contract reads and writes combat_stages by the requested stat key, clamps stages to -6..+6, forwards that stat through the hook context, and lets generic move-special parsing identify Accuracy and Evasion without a five-stat allowlist.";
    }
}
