package io.autoptu.cobblemon.battlecore;

import java.util.Set;

/**
 * Bounded compatibility gate for the authoritative area-target secondary-status path merged in
 * AutoPTU-Java PR #210. This class records integration authority only; it does not implement PTU
 * rules in Minecraft.
 */
public final class AreaSecondaryStatusCompatibility {
    public static final String AUTOPTU_JAVA_MAIN_SHA =
            "f85c2271e56b2c903cf53d124140d5a6dd562c9b";
    public static final String AUTOPTU_PYTHON_MAIN_SHA =
            "0444ff670a53b83499f360d70ff0428a45faa914";
    public static final String PINNED_PYTHON_BATTLE_ORACLE_SHA =
            "16d228efa63aabecb67fa788959a359aac7f8f03";
    public static final int MERGED_AREA_SECONDARY_STATUS_PR = 210;
    public static final int MERGED_SECONDARY_COMBAT_STAGE_PARSER_PR = 211;
    public static final int MERGED_SECONDARY_COMBAT_STAGE_APPLICATION_PR = 212;

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
                + "AutoPTU-Java PR #212 is merged on main at f85c2271e56b2c903cf53d124140d5a6dd562c9b and composes supported ATK/DEF/SPATK/SPDEF/SPD requests with authoritative CombatStageMutationService, preserving prevention, reflection, clamping and post-apply reactions. "
                + "Accuracy and Evasion still fail closed before mutation. PR #212 adds an authoritative application boundary and regression coverage, but does not wire that boundary into live BattleRuntime move execution. "
                + "Minecraft/Cobblemon therefore must not parse stage text, apply stage changes, evaluate prevention/reflection, or synthesize semantic stage events until a live runtime contract is merged and verified.";
    }

    public static String pythonOracleObservation() {
        return "AutoPTU Python main 0444ff670a53b83499f360d70ff0428a45faa914 changes Career GitHub Pages ranked-auth handling and does not replace the battle oracle. "
                + "Battle parity for this feature remains pinned to AutoPTU 16d228efa63aabecb67fa788959a359aac7f8f03, including _generic_post_damage_from_text semantics used by the Java secondary-effect contracts.";
    }
}
