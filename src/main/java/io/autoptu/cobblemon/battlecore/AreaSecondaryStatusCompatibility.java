package io.autoptu.cobblemon.battlecore;

import java.util.Set;

public final class AreaSecondaryStatusCompatibility {
    public static final String AUTOPTU_JAVA_MAIN_SHA =
            "55bdeb0cb9146054d4d80a0999bcd793275fe140";
    public static final String AUTOPTU_PYTHON_MAIN_SHA =
            "3953a701e8b756fa0f5da7b568cb2fc278d866f7";
    public static final String PINNED_PYTHON_BATTLE_ORACLE_SHA =
            "16d228efa63aabecb67fa788959a359aac7f8f03";
    public static final String OPEN_CHRONICLER_PROFILE_MATCH_PR_HEAD_SHA =
            "42f115e58ec05454f3f5340a9fec02a004527e7e";
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
    public static final int MERGED_ACCURACY_HELPER_OWNERSHIP_PR = 221;
    public static final int MERGED_FOCUSED_TRAINING_ACCURACY_PR = 222;
    public static final int MERGED_CHRONICLER_PROFILE_METADATA_PR = 223;
    public static final int OPEN_CHRONICLER_PROFILE_MATCH_PR = 224;

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
                + "PR #218 makes intrinsic Accuracy CS trusted combatant-profile state, PR #219 freezes temporary Accuracy bonus parity, PR #220 materializes normal temporary Accuracy inputs from BattleRuntimeState, and PR #221 freezes Focused Training/Chronicler helper ownership and behavior. "
                + "Merged PR #222 resolves Focused Training Accuracy from canonical controller bindings plus Duelist tag/momentum state without exposing those inputs through the public Minecraft action boundary. "
                + "Merged PR #223 adds immutable server-owned Chronicler profile metadata and freezes its pinned-oracle shape, but it does not attach that metadata to live TrainerRuntimeState, match live targets, apply targeted_profiling Accuracy, or wire effective Accuracy into hit resolution. "
                + "Java main is 55bdeb0cb9146054d4d80a0999bcd793275fe140. These contracts still do not compose mutable Accuracy stage + intrinsic Accuracy CS + every state-backed temporary Accuracy helper inside live authoritative hit resolution. "
                + "Draft PR #224 at 42f115e58ec05454f3f5340a9fec02a004527e7e is an oracle-inspection slice for _chronicler_profile_matches(); its own scope states that runtime state and Accuracy behavior are unchanged. It grants no adapter authority. "
                + "The pinned oracle reads combat_stages['accuracy'] for effective Accuracy while current evasion_value does not read combat_stages['evasion']. Minecraft/Cobblemon must not calculate effective Accuracy/Evasion, derive Chronicler matching or bonuses, reinterpret that asymmetry, apply secondary stage rules, or synthesize hit/stage outcomes until merged live runtime contracts prove them.";
    }

    public static String pythonOracleObservation() {
        return "AutoPTU Python main 3953a701e8b756fa0f5da7b568cb2fc278d866f7 remains separate from the pinned battle oracle used for Java parity. "
                + "Battle parity remains pinned to AutoPTU 16d228efa63aabecb67fa788959a359aac7f8f03. The pinned calculations contract keeps temporary Accuracy modifiers separate from stage projection; Java PR #216 records that Accuracy consumes combat_stages['accuracy'] while current evasion_value does not consume combat_stages['evasion'].";
    }
}