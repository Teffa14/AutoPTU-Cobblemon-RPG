package io.autoptu.cobblemon.battlecore;

import java.util.Set;

public final class AreaSecondaryStatusCompatibility {
    public static final String AUTOPTU_JAVA_MAIN_SHA =
            "57c7c2a9751cf02facf5d176b9d0f95b996a9bd1";
    public static final String AUTOPTU_PYTHON_MAIN_SHA =
            "ab57fef84387759fa8b959b4bd024c78a7d349bb";
    public static final String PINNED_PYTHON_BATTLE_ORACLE_SHA =
            "16d228efa63aabecb67fa788959a359aac7f8f03";
    public static final int MERGED_AREA_SECONDARY_STATUS_PR = 210;
    public static final int MERGED_EFFECTIVE_ACCURACY_RUNTIME_PREPARATION_PR = 230;

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

    public static String accuracyBoundary() {
        return "AutoPTU-Java PR #230 is merged and RuntimeAuthoritativeMovePreparation now composes mutable Accuracy stage, intrinsic accuracy_cs and server-owned temporary Accuracy. However current public RuntimeMoveResolution direct/area/delayed entry points still retain authoritativeStateBoundInput using actor.accuracyStage() only. Minecraft/Cobblemon must therefore remain fail-closed for effective Accuracy projection until Java main proves those public paths use the complete server-owned projection.";
    }

    public static String pythonOracleObservation() {
        return "AutoPTU Python main ab57fef84387759fa8b959b4bd024c78a7d349bb changes Career browser resilience and does not replace the pinned battle oracle. Battle parity remains pinned to AutoPTU 16d228efa63aabecb67fa788959a359aac7f8f03.";
    }
}
