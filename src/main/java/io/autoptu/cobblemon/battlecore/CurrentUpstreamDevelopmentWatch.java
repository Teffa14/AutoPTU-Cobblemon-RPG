package io.autoptu.cobblemon.battlecore;

/**
 * Records upstream movement observed after the last compatibility-matrix promotion.
 *
 * <p>This watch is informational. Open or reference-only upstream work must never promote adapter
 * authority. Capability support changes only when the corresponding contract is merged, inspected,
 * and reflected in {@link CurrentUpstreamCompatibilityInspection}.</p>
 */
public final class CurrentUpstreamDevelopmentWatch {
    public static final String AUTOPTU_JAVA_MAIN_SHA = "e2fc29f32a5204d564947219c0e25a4e625b4e66";
    public static final String AUTOPTU_PYTHON_MAIN_SHA = "4f75b652fa14e935b0f0f7c2903b946cfbb56526";
    public static final int AUTOPTU_JAVA_MERGED_POST_DAMAGE_TIMING_PR = 189;
    public static final int AUTOPTU_JAVA_REACTION_HANDOFF_PR = 190;

    private CurrentUpstreamDevelopmentWatch() {}

    public static boolean postDamageRuntimeMayBePromoted() {
        return false;
    }

    public static String postDamageRuntimeBlocker() {
        return "AutoPTU-Java PR #189 is merged as a post-applied-outcome POST_DAMAGE timing seam, but it is not a BattleRuntime call site. Draft PR #190 only freezes shared move-special result handoff across defender reactions. POST_DAMAGE remains upstream-owned and fail-closed in Minecraft until the authoritative runtime wires the full phase ordering.";
    }

    public static String pythonMainObservation() {
        return "AutoPTU Python main 4f75b65 contains Career relationship-role persistence hardening; it does not replace the frozen battle-oracle contract used for move-special phase parity.";
    }
}
