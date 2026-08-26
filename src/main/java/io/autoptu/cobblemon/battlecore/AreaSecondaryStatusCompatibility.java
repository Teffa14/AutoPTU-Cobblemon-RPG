package io.autoptu.cobblemon.battlecore;

import java.util.Set;

/**
 * Bounded compatibility gate for authoritative secondary-effect paths in AutoPTU-Java. This class
 * records integration authority only; it does not implement PTU rules in Minecraft.
 */
public final class AreaSecondaryStatusCompatibility {
    public static final String AUTOPTU_JAVA_MAIN_SHA =
            "a9fb0d81238e69a5263f074b4a8ad8ef1905325d";
    public static final String AUTOPTU_PYTHON_MAIN_SHA =
            "44305a1b3f06a45fbd06392a64573f287ac31555";
    public static final String PINNED_PYTHON_BATTLE_ORACLE_SHA =
            "16d228efa63aabecb67fa788959a359aac7f8f03";
    public static final int MERGED_AREA_SECONDARY_STATUS_PR = 210;
    public static final int MERGED_SECONDARY_COMBAT_STAGE_PARSER_PR = 211;
    public static final int MERGED_SECONDARY_COMBAT_STAGE_APPLICATION_PR = 212;
    public static final int MERGED_ACCURACY_EVASION_COMBAT_STAGE_CONTRACT_PR = 213;
    public static final int MERGED_SEVEN_COMBAT_STAGE_STATE_PR = 214;
    public static final int MERGED_SEVEN_COMBAT_STAGE_HOOKS_PR = 215;

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
                + "PR #212 is merged and composes stage requests with authoritative CombatStageMutationService, preserving prevention, reflection, clamping and post-apply reactions, but does not wire that application boundary into live BattleRuntime move execution. "
                + "PR #213 freezes the pinned Python Accuracy/Evasion Combat Stage contract and PR #214 stores all seven stages canonically on server-owned state. "
                + "PR #215 is merged on main at a9fb0d81238e69a5263f074b4a8ad8ef1905325d and migrates CombatStageMutationService plus prevention/post-apply hooks to CombatStageStat, so ATK/DEF/SPATK/SPDEF/SPD/Accuracy/Evasion share the authoritative mutation seam. "
                + "PR #215 verifies Accuracy mutation, Evasion prevention and Mirror Armor reflection, while explicitly stopping short of effective Evasion calculation parity and without establishing live BattleRuntime secondary Combat Stage execution. "
                + "Minecraft/Cobblemon therefore must not parse stage text, apply any secondary stage changes, evaluate prevention/reflection, calculate effective Evasion from stage state, or synthesize semantic stage events until live runtime execution and downstream arithmetic are merged and verified.";
    }

    public static String pythonOracleObservation() {
        return "AutoPTU Python main 44305a1b3f06a45fbd06392a64573f287ac31555 changes Career sponsor-renewal presentation and explicitly leaves battle behavior unchanged, so it does not replace the battle oracle. "
                + "Battle parity remains pinned to AutoPTU 16d228efa63aabecb67fa788959a359aac7f8f03. The frozen oracle contract reads and writes combat_stages by the requested stat key, clamps stages to -6..+6, forwards that stat through the hook context, and lets generic move-special parsing identify Accuracy and Evasion without a five-stat allowlist.";
    }
}
