package io.autoptu.cobblemon.battlecore;

import java.util.Set;

public final class AreaSecondaryStatusCompatibility {
    public static final String AUTOPTU_JAVA_MAIN_SHA =
            "b66fcb4dac909c2f44bf6caf54a15f8da82e3e0a";
    public static final String AUTOPTU_PYTHON_MAIN_SHA =
            "231c50e4f2e7c4c0442123b1ba2221b7d07384eb";
    public static final String PINNED_PYTHON_BATTLE_ORACLE_SHA =
            "16d228efa63aabecb67fa788959a359aac7f8f03";
    public static final int MERGED_AREA_SECONDARY_STATUS_PR = 210;
    public static final int MERGED_SECONDARY_COMBAT_STAGE_PARSER_PR = 211;
    public static final int MERGED_SECONDARY_COMBAT_STAGE_APPLICATION_PR = 212;
    public static final int MERGED_ACCURACY_EVASION_COMBAT_STAGE_CONTRACT_PR = 213;
    public static final int MERGED_SEVEN_COMBAT_STAGE_STATE_PR = 214;
    public static final int MERGED_SEVEN_COMBAT_STAGE_HOOKS_PR = 215;
    public static final int MERGED_EFFECTIVE_ACCURACY_EVASION_PROJECTION_CONTRACT_PR = 216;
    public static final int MERGED_EFFECTIVE_ACCURACY_PROJECTION_PR = 217;

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

    public static boolean effectiveAccuracyEvasionArithmeticMayBeProjected() {
        return false;
    }

    public static boolean delayedSecondaryStatusMayBeProjected() {
        return false;
    }

    public static String mergedAreaBoundary() {
        return "AutoPTU-Java PR #210 is merged on main. Projection is permitted only while every declared upstream dependency remains non-BLOCKING. Minecraft/Cobblemon must not calculate secondary-effect outcomes or mutate status state.";
    }

    public static String combatStageBoundary() {
        return "AutoPTU-Java PR #211 through PR #215 establish parsing, application, seven-stage state and authoritative mutation hooks. "
                + "PR #216 freezes the effective Accuracy/Evasion arithmetic contract against the pinned Python oracle. "
                + "PR #217 is merged on main at b66fcb4dac909c2f44bf6caf54a15f8da82e3e0a and adds only a package-private EffectiveAccuracyStageProjection primitive that combines dynamic stage, intrinsic Accuracy CS and runtime bonus before clamping. "
                + "PR #217 does not add intrinsic Accuracy ownership to RuntimeCombatantState and does not wire the primitive into live hit resolution, so it grants no adapter authority. "
                + "The pinned oracle reads combat_stages['accuracy'] for effective Accuracy while current evasion_value does not read combat_stages['evasion']. Minecraft/Cobblemon must not apply secondary stage changes, calculate effective Accuracy/Evasion, reinterpret that asymmetry, or synthesize stage outcomes until merged live runtime contracts prove those behaviors.";
    }

    public static String pythonOracleObservation() {
        return "AutoPTU Python main 231c50e4f2e7c4c0442123b1ba2221b7d07384eb changes Career rival-timeline validation and does not replace the pinned battle oracle. "
                + "Battle parity remains pinned to AutoPTU 16d228efa63aabecb67fa788959a359aac7f8f03. Current calculations still separate temporary accuracy modifiers from the stage contract; Java PR #216 records that Accuracy consumes combat_stages['accuracy'] while current evasion_value does not consume combat_stages['evasion'].";
    }
}
