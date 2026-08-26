package io.autoptu.cobblemon.battlecore;

import java.util.Set;

public final class AreaSecondaryStatusCompatibility {
    public static final String AUTOPTU_JAVA_MAIN_SHA =
            "b35f09bbcc4246b1846e57c5c4f9bb5771d474e8";
    public static final String AUTOPTU_PYTHON_MAIN_SHA =
            "12d13d535f4cb0132dd609d374f1739163f71261";
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
    public static final int DRAFT_ACCURACY_HELPER_OWNERSHIP_PR = 221;
    public static final String DRAFT_ACCURACY_HELPER_OWNERSHIP_SHA =
            "63dabea92042964811cc1ad46011fbc36526bec0";

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

    public static boolean secondaryCombatStageMayBeProjected() { return false; }
    public static boolean accuracyEvasionCombatStageMayBeProjected() { return false; }
    public static boolean effectiveAccuracyEvasionArithmeticMayBeProjected() { return false; }
    public static boolean delayedSecondaryStatusMayBeProjected() { return false; }

    public static String mergedAreaBoundary() {
        return "AutoPTU-Java PR #210 is merged on main. Projection is permitted only while every declared upstream dependency remains non-BLOCKING. Minecraft/Cobblemon must not calculate secondary-effect outcomes or mutate status state.";
    }

    public static String combatStageBoundary() {
        return "AutoPTU-Java PR #211 through PR #215 establish parsing, application, seven-stage state and authoritative mutation hooks. "
                + "PR #216 freezes the effective Accuracy/Evasion arithmetic contract against the pinned Python oracle. "
                + "PR #217 adds EffectiveAccuracyStageProjection, PR #218 owns intrinsic accuracy_cs in trusted combatant profile state, PR #219 freezes temporary Accuracy semantics, and PR #220 materializes ordinary temporary Accuracy inputs from BattleRuntimeState. "
                + "Draft PR #221 at 63dabea92042964811cc1ad46011fbc36526bec0 only freezes Focused Training/Chronicler helper ownership and still has a failing Combat Stage Accuracy Evasion Parity workflow. "
                + "No merged contract wires the resolved temporary bonus plus mutable Accuracy stage plus intrinsic accuracy_cs into live hit resolution. The pinned oracle also preserves the current asymmetry where combat_stages['accuracy'] contributes to Accuracy while evasion_value does not consume combat_stages['evasion']. "
                + "Minecraft/Cobblemon therefore must not calculate effective Accuracy/Evasion, supply helper contributions, derive temporary Accuracy independently, apply secondary stage changes, reinterpret the oracle asymmetry, or synthesize hit/stage outcomes.";
    }

    public static String pythonOracleObservation() {
        return "AutoPTU Python main 12d13d535f4cb0132dd609d374f1739163f71261 changes Career rival progression presentation validation and does not replace the pinned battle oracle. "
                + "Battle parity remains pinned to AutoPTU 16d228efa63aabecb67fa788959a359aac7f8f03; Accuracy helper and stage semantics remain server-owned upstream behavior.";
    }
}
