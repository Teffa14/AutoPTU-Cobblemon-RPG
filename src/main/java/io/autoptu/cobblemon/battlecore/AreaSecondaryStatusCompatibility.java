package io.autoptu.cobblemon.battlecore;

import java.util.Set;

public final class AreaSecondaryStatusCompatibility {
    public static final String AUTOPTU_JAVA_MAIN_SHA =
            "8670b4bf2b423c5d9e43cc9e8d6c979e6c832909";
    public static final String AUTOPTU_PYTHON_MAIN_SHA =
            "c4aff1eb04e7bb27f72b6aaeb55937e7f6c71563";
    public static final String PINNED_PYTHON_BATTLE_ORACLE_SHA =
            "16d228efa63aabecb67fa788959a359aac7f8f03";
    public static final String DRAFT_EFFECTIVE_ACCURACY_LIVE_HEAD_SHA =
            "a84f924212d6890b3fa92df4d26438a3f54a365a";
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
    public static final int MERGED_TEMPORARY_ACCURACY_RUNTIME_INPUTS_PR = 220;
    public static final int MERGED_FOCUSED_TRAINING_CHRONICLER_CONTRACT_PR = 221;
    public static final int MERGED_FOCUSED_TRAINING_RUNTIME_PR = 222;
    public static final int MERGED_CHRONICLER_METADATA_PR = 223;
    public static final int MERGED_CHRONICLER_PROFILE_MATCH_PR = 225;
    public static final int MERGED_CHRONICLER_ACCURACY_BONUS_PR = 226;
    public static final int MERGED_CHRONICLER_RUNTIME_IDENTITY_PR = 227;
    public static final int MERGED_CHRONICLER_RUNTIME_ACCURACY_PR = 228;
    public static final int DRAFT_EFFECTIVE_ACCURACY_LIVE_PR = 230;

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
        return "AutoPTU-Java PR #211 through PR #228 establish increasingly complete server-owned Combat Stage and Accuracy inputs. "
                + "PR #227 owns canonical Pokemon name/species identity plus Trainer name and Chronicler metadata in runtime state, with legacy identity failing closed. "
                + "PR #228 derives Chronicler targeted_profiling Accuracy from authoritative BattleRuntimeState and rejects spoofed prepared Chronicler bonuses when canonical Trainer ownership exists. "
                + "Draft PR #230 at a84f924212d6890b3fa92df4d26438a3f54a365a proposes the remaining live effective Accuracy composition: mutable Accuracy stage plus intrinsic accuracy_cs plus resolved temporary Accuracy, clamped through EffectiveAccuracyStageProjection and written into MoveResolutionInput. "
                + "PR #230 is not merged, so it grants no adapter authority even though its observed CI is green. Effective Evasion remains separately incomplete under the pinned oracle contract. "
                + "Minecraft/Cobblemon therefore must not calculate temporary Accuracy, Chronicler or Focused Training bonuses, compose effective Accuracy/Evasion, apply secondary Combat Stages, or synthesize hit outcomes.";
    }

    public static String pythonOracleObservation() {
        return "AutoPTU Python main c4aff1eb04e7bb27f72b6aaeb55937e7f6c71563 changes Career rollback decision counters and does not replace the pinned battle oracle. "
                + "Battle parity remains pinned to AutoPTU 16d228efa63aabecb67fa788959a359aac7f8f03. In that oracle, calculations.py owns stage clamping and temporary Accuracy composition; Java PR #230 explicitly reuses those server-owned contributions instead of trusting the legacy MoveResolutionInput accuracyStage.";
    }
}
