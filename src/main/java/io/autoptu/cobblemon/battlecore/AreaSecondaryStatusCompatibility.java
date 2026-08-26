package io.autoptu.cobblemon.battlecore;

import java.util.Set;

public final class AreaSecondaryStatusCompatibility {
    public static final String AUTOPTU_JAVA_MAIN_SHA =
            "d2d232a4a5be9facbeaeea706081deb93b9c4b7c";
    public static final String AUTOPTU_PYTHON_MAIN_SHA =
            "e9c4173e066da999046818d9ca066bd013f26431";
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
    public static final int MERGED_INTRINSIC_ACCURACY_OWNERSHIP_PR = 218;
    public static final int MERGED_TEMPORARY_ACCURACY_CONTRACT_PR = 219;
    public static final int MERGED_TEMPORARY_ACCURACY_RUNTIME_INPUTS_PR = 220;
    public static final int MERGED_FOCUSED_TRAINING_CHRONICLER_CONTRACT_PR = 221;
    public static final int MERGED_FOCUSED_TRAINING_RUNTIME_PR = 222;
    public static final int MERGED_CHRONICLER_METADATA_PR = 223;
    public static final int MERGED_CHRONICLER_PROFILE_MATCH_PR = 225;
    public static final int DRAFT_CHRONICLER_ACCURACY_BONUS_PR = 226;
    public static final String DRAFT_CHRONICLER_ACCURACY_BONUS_SHA =
            "f3c9eab585a48aeec5fad27aa712dd1d38aa4b3a";

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
        return "AutoPTU-Java PR #211 through PR #225 establish increasingly complete server-owned Combat Stage and Accuracy inputs, including intrinsic Accuracy CS, temporary Accuracy contracts, Focused Training ownership, Chronicler metadata and canonical Chronicler profile matching. "
                + "PR #225 is merged on Java main at d2d232a4a5be9facbeaeea706081deb93b9c4b7c. "
                + "Draft PR #226 at f3c9eab585a48aeec5fad27aa712dd1d38aa4b3a adds a server-side Chronicler Accuracy bonus resolver over TemporaryEffectStore with pinned-oracle parity, but explicitly leaves runtime ownership of Chronicler metadata and combatant identity plus live Accuracy preparation for later slices. "
                + "No merged contract yet composes mutable Accuracy stage, intrinsic Accuracy CS and all temporary Accuracy contributions into authoritative live hit resolution. "
                + "Minecraft/Cobblemon therefore must not run Chronicler profile matching, targeted_profiling bonus resolution, temporary Accuracy arithmetic, effective Accuracy/Evasion calculation, secondary stage mutation or hit outcome synthesis.";
    }

    public static String pythonOracleObservation() {
        return "AutoPTU Python main e9c4173e066da999046818d9ca066bd013f26431 changes Career ranked persistence ordering and does not replace the pinned battle oracle. "
                + "Battle parity remains pinned to AutoPTU 16d228efa63aabecb67fa788959a359aac7f8f03. Java PR #225 freezes _chronicler_profile_matches() against that oracle, while draft PR #226 freezes _chronicler_accuracy_bonus() behavior without wiring it into live Accuracy.";
    }
}
