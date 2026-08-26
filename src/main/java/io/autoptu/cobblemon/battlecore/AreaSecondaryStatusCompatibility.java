package io.autoptu.cobblemon.battlecore;

import java.util.Set;

public final class AreaSecondaryStatusCompatibility {
    public static final String AUTOPTU_JAVA_MAIN_SHA =
            "55bdeb0cb9146054d4d80a0999bcd793275fe140";
    public static final String AUTOPTU_PYTHON_MAIN_SHA =
            "b77644e64596d40b5d712b261802bde19ae9d806";
    public static final String PINNED_PYTHON_BATTLE_ORACLE_SHA =
            "16d228efa63aabecb67fa788959a359aac7f8f03";
    public static final String DRAFT_CHRONICLER_PROFILE_MATCH_PR_HEAD_SHA =
            "0c972201d5105fab5d5abc1c0ddc42e19b6db23b";
    public static final int MERGED_AREA_SECONDARY_STATUS_PR = 210;
    public static final int MERGED_SECONDARY_COMBAT_STAGE_PARSER_PR = 211;
    public static final int MERGED_SECONDARY_COMBAT_STAGE_APPLICATION_PR = 212;
    public static final int MERGED_ACCURACY_EVASION_COMBAT_STAGE_CONTRACT_PR = 213;
    public static final int MERGED_SEVEN_COMBAT_STAGE_STATE_PR = 214;
    public static final int MERGED_SEVEN_COMBAT_STAGE_HOOKS_PR = 215;
    public static final int MERGED_EFFECTIVE_ACCURACY_EVASION_PROJECTION_CONTRACT_PR = 216;
    public static final int MERGED_EFFECTIVE_ACCURACY_PROJECTION_PR = 217;
    public static final int MERGED_INTRINSIC_ACCURACY_OWNERSHIP_PR = 218;
    public static final int MERGED_TEMPORARY_ACCURACY_PARITY_PR = 219;
    public static final int MERGED_TEMPORARY_ACCURACY_INPUTS_PR = 220;
    public static final int MERGED_ACCURACY_HELPER_OWNERSHIP_PR = 221;
    public static final int MERGED_FOCUSED_TRAINING_ACCURACY_PR = 222;
    public static final int MERGED_CHRONICLER_METADATA_PR = 223;
    public static final int SUPERSEDED_CHRONICLER_MATCH_PR = 224;
    public static final int DRAFT_CHRONICLER_PROFILE_MATCH_PR = 225;

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

    public static boolean chroniclerAccuracyMayBeProjected() {
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
                + "PR #217 adds the EffectiveAccuracyStageProjection primitive, PR #218 owns intrinsic Accuracy CS, PR #219 freezes temporary Accuracy parity, and PR #220 materializes normal temporary Accuracy inputs from BattleRuntimeState. "
                + "PR #221 freezes Focused Training and Chronicler helper ownership, PR #222 resolves Focused Training from authoritative controller/Duelist state, and PR #223 owns canonical Chronicler profile metadata. "
                + "Those merged contracts still do not compose mutable Accuracy stage, intrinsic Accuracy CS and all resolved temporary bonuses inside live hit resolution. "
                + "The pinned oracle reads combat_stages['accuracy'] for effective Accuracy while current evasion_value does not read combat_stages['evasion']. Minecraft/Cobblemon must not apply secondary stage changes, calculate effective Accuracy/Evasion, reinterpret that asymmetry, or synthesize stage outcomes until merged live runtime contracts prove those behaviors.";
    }

    public static String chroniclerBoundary() {
        return "AutoPTU-Java PR #224 is superseded by draft PR #225 at " + DRAFT_CHRONICLER_PROFILE_MATCH_PR_HEAD_SHA + ". "
                + "PR #225 ports the pinned Python _chronicler_profile_matches() helper as a pure server-side resolver over canonical Chronicler metadata and live target identity. "
                + "It remains draft and does not bind Chronicler metadata into TrainerRuntimeState, apply targeted_profiling Accuracy, or participate in live hit resolution. "
                + "Minecraft/Cobblemon must not perform profile matching, derive Trainer/species identity for this rule, or add the Chronicler Accuracy bonus.";
    }

    public static String pythonOracleObservation() {
        return "AutoPTU Python main b77644e64596d40b5d712b261802bde19ae9d806 changes Career timeline numeric evidence and does not replace the pinned battle oracle. "
                + "Battle parity remains pinned to AutoPTU 16d228efa63aabecb67fa788959a359aac7f8f03. The pinned _chronicler_profile_matches() helper fails closed without trainer/target data, requires canonical Profile records, normalizes record values, and compares them with live target name/species and optional controller Trainer name. "
                + "Current calculations still separate temporary Accuracy modifiers from Combat Stages; Java PR #216 records that Accuracy consumes combat_stages['accuracy'] while current evasion_value does not consume combat_stages['evasion'].";
    }
}
