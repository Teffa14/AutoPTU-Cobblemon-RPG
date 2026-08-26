package io.autoptu.cobblemon.battlecore;

import java.util.Set;

public final class AreaSecondaryStatusCompatibility {
    public static final String AUTOPTU_JAVA_MAIN_SHA =
            "b35f09bbcc4246b1846e57c5c4f9bb5771d474e8";
    public static final String AUTOPTU_PYTHON_MAIN_SHA =
            "011ba46379255dc2175c08a73c08a7b7e6200176";
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
    public static final int MERGED_INTRINSIC_ACCURACY_PROFILE_PR = 218;
    public static final int MERGED_TEMPORARY_ACCURACY_BONUS_CONTRACT_PR = 219;
    public static final int MERGED_RUNTIME_TEMPORARY_ACCURACY_INPUTS_PR = 220;

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
                + "PR #217 adds the package-private EffectiveAccuracyStageProjection primitive and PR #218 stores intrinsic PokemonSpec.accuracy_cs as trusted immutable profile content. "
                + "PR #219 freezes temporary Accuracy bonus semantics and PR #220 materializes ordinary ability, item, position, move metadata, temporary-effect and lower-AV inputs from BattleRuntimeState. "
                + "PR #220 still keeps Focused Training and Chronicler helper contributions behind a package-private core seam and explicitly does not wire the resolved temporary bonus or effective Accuracy projection into live hit resolution. "
                + "The pinned oracle reads combat_stages['accuracy'] for effective Accuracy while current evasion_value does not read combat_stages['evasion']. Minecraft/Cobblemon must not calculate effective Accuracy/Evasion, supply helper contributions, derive temporary Accuracy bonuses independently, apply secondary stage changes, reinterpret that asymmetry, or synthesize hit/stage outcomes until merged live runtime contracts prove those behaviors.";
    }

    public static String pythonOracleObservation() {
        return "AutoPTU Python main 011ba46379255dc2175c08a73c08a7b7e6200176 changes Career leaderboard display-name validation and explicitly leaves score, ranking and battle rules unchanged. "
                + "Battle parity remains pinned to AutoPTU 16d228efa63aabecb67fa788959a359aac7f8f03. The pinned calculations keep temporary Accuracy modifiers server-side and separate from the mutable Accuracy stage plus intrinsic accuracy_cs projection; current evasion_value still does not consume combat_stages['evasion'].";
    }
}
