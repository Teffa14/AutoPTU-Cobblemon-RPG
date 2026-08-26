package io.autoptu.cobblemon.battlecore;

import java.util.Set;

public final class AreaSecondaryStatusCompatibility {
    public static final String AUTOPTU_JAVA_MAIN_SHA =
            "b35f09bbcc4246b1846e57c5c4f9bb5771d474e8";
    public static final String AUTOPTU_PYTHON_MAIN_SHA =
            "7c4edba551cc57a51514f7cb43a75745db422837";
    public static final String PINNED_PYTHON_BATTLE_ORACLE_SHA =
            "16d228efa63aabecb67fa788959a359aac7f8f03";
    public static final String OPEN_ACCURACY_HELPER_OWNERSHIP_PR_HEAD_SHA =
            "d317e1ded62752a098513458474b70b2a197f1f9";
    public static final int MERGED_AREA_SECONDARY_STATUS_PR = 210;
    public static final int MERGED_SECONDARY_COMBAT_STAGE_PARSER_PR = 211;
    public static final int MERGED_SECONDARY_COMBAT_STAGE_APPLICATION_PR = 212;
    public static final int MERGED_ACCURACY_EVASION_COMBAT_STAGE_CONTRACT_PR = 213;
    public static final int MERGED_SEVEN_COMBAT_STAGE_STATE_PR = 214;
    public static final int MERGED_SEVEN_COMBAT_STAGE_HOOKS_PR = 215;
    public static final int MERGED_EFFECTIVE_ACCURACY_EVASION_PROJECTION_CONTRACT_PR = 216;
    public static final int MERGED_EFFECTIVE_ACCURACY_PROJECTION_PR = 217;
    public static final int MERGED_INTRINSIC_ACCURACY_OWNERSHIP_PR = 218;
    public static final int MERGED_TEMPORARY_ACCURACY_CONTRACT_PR = 219;
    public static final int MERGED_RUNTIME_TEMPORARY_ACCURACY_INPUTS_PR = 220;
    public static final int OPEN_ACCURACY_HELPER_OWNERSHIP_PR = 221;

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
        return "AutoPTU-Java PR #211 through PR #217 establish parsing, seven-stage state/hooks and the isolated EffectiveAccuracyStageProjection primitive. "
                + "PR #218 makes intrinsic Accuracy CS trusted combatant-profile state, PR #219 freezes temporary Accuracy bonus parity, and PR #220 materializes normal temporary Accuracy inputs from BattleRuntimeState. "
                + "Java main is b35f09bbcc4246b1846e57c5c4f9bb5771d474e8. Those contracts still do not compose mutable Accuracy stage + intrinsic Accuracy CS + resolved temporary Accuracy bonus inside live authoritative hit resolution. "
                + "Draft PR #221 at d317e1ded62752a098513458474b70b2a197f1f9 freezes Focused Training/Chronicler helper ownership and has green parity/contract checks on the inspected head, but it still deliberately does not wire those values into live Accuracy, so it grants no adapter authority. "
                + "The pinned oracle reads combat_stages['accuracy'] for effective Accuracy while current evasion_value does not read combat_stages['evasion']. Minecraft/Cobblemon must not calculate effective Accuracy/Evasion, reinterpret that asymmetry, apply secondary stage rules, or synthesize hit/stage outcomes until merged live runtime contracts prove them.";
    }

    public static String pythonOracleObservation() {
        return "AutoPTU Python main 7c4edba551cc57a51514f7cb43a75745db422837 remains separate from the pinned battle oracle used for Java parity. "
                + "Battle parity remains pinned to AutoPTU 16d228efa63aabecb67fa788959a359aac7f8f03. The pinned calculations contract keeps temporary Accuracy modifiers separate from stage projection; Java PR #216 records that Accuracy consumes combat_stages['accuracy'] while current evasion_value does not consume combat_stages['evasion'].";
    }
}
