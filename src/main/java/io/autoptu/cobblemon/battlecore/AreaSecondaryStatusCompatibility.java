package io.autoptu.cobblemon.battlecore;

import java.util.Set;

public final class AreaSecondaryStatusCompatibility {
    public static final String AUTOPTU_JAVA_MAIN_SHA =
            "c5ef1d72c8a997144d215423e2aab60d706905a9";
    public static final String AUTOPTU_PYTHON_MAIN_SHA =
            "85df7624416c5596a6c047977326b3b60cd733c1";
    public static final String PINNED_PYTHON_BATTLE_ORACLE_SHA =
            "16d228efa63aabecb67fa788959a359aac7f8f03";
    public static final String DRAFT_CHRONICLER_RUNTIME_IDENTITY_HEAD_SHA =
            "0ef1bd1b7d0dcbe72451952aa205b622ef26cab0";
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
    public static final int DRAFT_CHRONICLER_RUNTIME_IDENTITY_PR = 227;

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
        return "AutoPTU-Java PR #211 through PR #226 establish increasingly complete server-owned Combat Stage and Accuracy inputs, including intrinsic Accuracy CS, temporary Accuracy contracts, Focused Training ownership, Chronicler metadata, canonical Chronicler profile matching, and targeted_profiling bonus resolution. "
                + "PR #226 is merged on Java main at c5ef1d72c8a997144d215423e2aab60d706905a9 and owns expiry, stacking, source-controller fallback and +2 matching bonuses over server-owned temporary effects. "
                + "Draft PR #227 at 0ef1bd1b7d0dcbe72451952aa205b622ef26cab0 proposes server-owned Pokemon name/species identity plus Trainer name and Chronicler metadata in runtime state, with legacy snapshots failing closed instead of treating internal ids as Pokemon names. "
                + "PR #227 explicitly does not wire the Chronicler Accuracy bonus into live hit resolution and is not merged, so it grants no adapter authority. "
                + "No merged contract yet composes mutable Accuracy stage, intrinsic Accuracy CS and all temporary Accuracy contributions into authoritative live hit resolution. "
                + "Minecraft/Cobblemon therefore must not run Chronicler profile matching, targeted_profiling bonus resolution, temporary Accuracy arithmetic, effective Accuracy/Evasion calculation, secondary stage mutation or hit outcome synthesis.";
    }

    public static String pythonOracleObservation() {
        return "AutoPTU Python main 85df7624416c5596a6c047977326b3b60cd733c1 changes Career decision-memory validation and does not replace the pinned battle oracle. "
                + "Battle parity remains pinned to AutoPTU 16d228efa63aabecb67fa788959a359aac7f8f03. In that oracle, _temporary_accuracy_bonus() delegates Chronicler to battle._chronicler_accuracy_bonus(attacker, defender), preserving the engine-owned split between runtime identity/profile matching, temporary Accuracy modifiers and final hit resolution.";
    }
}
