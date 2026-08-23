package io.autoptu.cobblemon.battlecore;

/**
 * Compatibility guard for the bounded upstream forced-movement instruction parser.
 *
 * AutoPTU-Java can currently identify Push/Pull intent and distance from canonical
 * move metadata. It does not yet resolve or execute spatial forced movement.
 */
public final class ForcedMovementInstructionCompatibility {
    public static final String INSPECTED_AUTOPTU_JAVA_SHA = "7de79dcd30b241d439724050fb24ee893a7c5c63";
    public static final String INSPECTED_AUTOPTU_PYTHON_SHA = "99ba07ea47b8896d96bd37f6c06cffb8695f69bb";
    public static final String JAVA_ORACLE_PIN_SHA = "16d228efa63aabecb67fa788959a359aac7f8f03";

    private ForcedMovementInstructionCompatibility() {}

    public static boolean instructionDetectionIsAvailable() {
        return true;
    }

    public static boolean forcedMovementPlaybackIsAllowed() {
        return !IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.FORCED_MOVEMENT_PLAYBACK
        ).hasBlockingDependency();
    }

    public static String adapterPolicy() {
        return "Consume only future authoritative forced-movement state/events. "
                + "Do not parse move text, choose push/pull direction, resolve distance into a path, "
                + "decide collision/interception/reactions, or relocate entities from the instruction alone.";
    }
}
