package io.autoptu.cobblemon.battlecore;

/**
 * Compatibility guard for bounded upstream movement contracts.
 *
 * <p>Reaction escape movement can now be applied authoritatively by AutoPTU-Java and emits
 * a semantic ShiftResolvedEvent. Generic forced movement remains blocked because PUSH/PULL
 * instruction parsing still does not own direction, path, collision, interception, reactions,
 * terrain interaction, or final relocation.</p>
 */
public final class ForcedMovementInstructionCompatibility {
    public static final String INSPECTED_AUTOPTU_JAVA_SHA = "aefc058328a9217d634477835a4851d521aaeccb";
    public static final String INSPECTED_AUTOPTU_PYTHON_SHA = "29a8e62e24c3e58233ca2c8154a30d796099f90a";
    public static final String JAVA_ORACLE_PIN_SHA = "16d228efa63aabecb67fa788959a359aac7f8f03";

    private ForcedMovementInstructionCompatibility() {}

    public static boolean instructionDetectionIsAvailable() {
        return true;
    }

    public static boolean reactionEscapeDestinationSelectionIsAvailable() {
        return true;
    }

    public static boolean reactionMovementApplicationIsAuthoritative() {
        return true;
    }

    public static boolean reactionMovementEmitsSemanticShiftEvent() {
        return true;
    }

    public static boolean forcedMovementPlaybackIsAllowed() {
        return !IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.FORCED_MOVEMENT_PLAYBACK
        ).hasBlockingDependency();
    }

    public static String adapterPolicy() {
        return "Consume authoritative reaction movement state and ShiftResolvedEvent from AutoPTU-Java. "
                + "Do not parse move text, choose push/pull direction, resolve instruction distance into a path, "
                + "decide collision/interception/reaction ordering, or relocate entities from partial generic "
                + "forced-movement instructions.";
    }
}
