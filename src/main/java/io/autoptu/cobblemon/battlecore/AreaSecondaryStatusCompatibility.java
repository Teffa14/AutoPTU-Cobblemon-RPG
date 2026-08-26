package io.autoptu.cobblemon.battlecore;

import java.util.Set;

public final class AreaSecondaryStatusCompatibility {
    public static final String AUTOPTU_JAVA_MAIN_SHA =
            "38eb8966ecdc2295cabff932ad1f09d3e82ed6f5";
    public static final String AUTOPTU_PYTHON_MAIN_SHA =
            "ad9c202ec9e3982c6797bd38b14df8f647852fc9";
    public static final String PINNED_PYTHON_BATTLE_ORACLE_SHA =
            "16d228efa63aabecb67fa788959a359aac7f8f03";
    public static final int MERGED_AREA_SECONDARY_STATUS_PR = 210;
    public static final int MERGED_SECONDARY_COMBAT_STAGE_PARSER_PR = 211;
    public static final int MERGED_SECONDARY_COMBAT_STAGE_APPLICATION_PR = 212;
    public static final int MERGED_ACCURACY_EVASION_COMBAT_STAGE_CONTRACT_PR = 213;
    public static final int MERGED_SEVEN_COMBAT_STAGE_STATE_PR = 214;
    public static final int MERGED_SEVEN_COMBAT_STAGE_HOOKS_PR = 215;
    public static final int MERGED_EFFECTIVE_ACCURACY_EVASION_PROJECTION_CONTRACT_PR = 216;
    public static final int OPEN_EFFECTIVE_ACCURACY_PROJECTION_PR = 217;

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
                + "PR #216 is merged on main at 38eb8966ecdc2295cabff932ad1f09d3e82ed6f5 and freezes the effective Accuracy/Evasion arithmetic contract against the pinned Python oracle. "
                + "Accuracy reads combat_stages['accuracy']; current evasion_value does not read combat_stages['evasion']. "
                + "Draft PR #217 adds a package-private effective Accuracy projection primitive but does not add intrinsic Accuracy ownership to RuntimeCombatantState and does not change live hit resolution, so it grants no adapter authority. "
                + "Minecraft/Cobblemon must not apply secondary stage changes, calculate effective Accuracy/Evasion, reinterpret the oracle asymmetry, or synthesize stage outcomes until merged live runtime contracts prove those behaviors.";
    }

    public static String pythonOracleObservation() {
        return "AutoPTU Python main ad9c202ec9e3982c6797bd38b14df8f647852fc9 changes Career club-transition validation and does not replace the pinned battle oracle. "
                + "Battle parity remains pinned to AutoPTU 16d228efa63aabecb67fa788959a359aac7f8f03. The merged Java PR #216 contract observes that Accuracy arithmetic reads combat_stages['accuracy'], while current evasion_value does not read combat_stages['evasion'].";
    }
}
