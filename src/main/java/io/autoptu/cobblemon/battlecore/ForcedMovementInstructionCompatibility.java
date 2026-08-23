package io.autoptu.cobblemon.battlecore;

/**
 * Compatibility guard for bounded upstream movement contracts that are not yet sufficient
 * to authorize Minecraft-side spatial playback.
 */
public final class ForcedMovementInstructionCompatibility {
    public static final String INSPECTED_AUTOPTU_JAVA_SHA = "3ede4a8493738ddc70b2f0eb3959973488f78db9";
    public static final String INSPECTED_AUTOPTU_PYTHON_SHA = "ff069a928f936f4a1dca54597ef3f85348ea4b0b";
    public static final String JAVA_ORACLE_PIN_SHA = "16d228efa63aabecb67fa788959a359aac7f8f03";

    private ForcedMovementInstructionCompatibility() {}

    public static boolean instructionDetectionIsAvailable() {
        return true;
    }

    public static boolean reactionEscapeDestinationSelectionIsAvailable() {
        return true;
    }

    public static boolean forcedMovementPlaybackIsAllowed() {
        return !IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.FORCED_MOVEMENT_PLAYBACK
        ).hasBlockingDependency();
    }

    public static String adapterPolicy() {
        return "Consume only authoritative movement state/events produced by AutoPTU-Java. "
                + "Do not parse move text, choose push/pull direction, resolve instruction distance into a path, "
                + "recompute reaction escape destinations, decide collision/interception/reaction ordering, "
                + "or relocate entities from partial movement primitives alone.";
    }
}
