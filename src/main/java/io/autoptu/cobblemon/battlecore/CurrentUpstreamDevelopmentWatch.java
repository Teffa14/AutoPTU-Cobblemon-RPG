package io.autoptu.cobblemon.battlecore;

/**
 * Records upstream movement observed after the last compatibility-matrix promotion.
 *
 * <p>This watch is informational. Open or reference-only upstream work must never promote adapter
 * authority. Capability support changes only when the corresponding contract is merged, inspected,
 * and reflected in {@link CurrentUpstreamCompatibilityInspection}.</p>
 */
public final class CurrentUpstreamDevelopmentWatch {
    public static final String AUTOPTU_JAVA_MAIN_SHA = "3caac611a987322a70dbdc34c56d613b96dadb92";
    public static final String AUTOPTU_PYTHON_MAIN_SHA = "6affb828cc72ab76e6952847e2275d14d166d216";
    public static final int AUTOPTU_JAVA_POST_DAMAGE_TIMING_PR = 189;

    private CurrentUpstreamDevelopmentWatch() {}

    public static boolean postDamageRuntimeMayBePromoted() {
        return false;
    }

    public static String postDamageRuntimeBlocker() {
        return "AutoPTU-Java PR #189 is open and not part of main; POST_DAMAGE timing remains upstream-owned and fail-closed in Minecraft.";
    }

    public static String pythonMainObservation() {
        return "AutoPTU Python main 6affb82 contains Career sponsor-settlement persistence hardening; it does not replace the frozen battle-oracle contract used for move-special phase parity.";
    }
}
