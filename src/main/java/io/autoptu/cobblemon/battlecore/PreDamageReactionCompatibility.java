package io.autoptu.cobblemon.battlecore;

/**
 * Compatibility guard for the upstream generic PRE-damage reaction seam.
 *
 * <p>AutoPTU-Java now exposes a generic pre-damage hook registry, an authoritative
 * reaction-escape movement application, and a parity-backed Telepathy hook. The
 * inspected direct move-resolution entrypoint does not yet invoke that registry,
 * so Minecraft must not trigger Telepathy, calculate threatened areas, choose a
 * reaction destination, or cancel hit/damage itself.</p>
 */
public final class PreDamageReactionCompatibility {
    public static final String INSPECTED_AUTOPTU_JAVA_SHA = "ebfdf7b29da5cdde9b7df7bd6d193ae03f5203f7";
    public static final String INSPECTED_AUTOPTU_PYTHON_SHA = "2ec841a4bab8ce7de0698afaf37e0169ae61a277";
    public static final String JAVA_TELEPATHY_ORACLE_PIN_SHA = "16d228efa63aabecb67fa788959a359aac7f8f03";

    private PreDamageReactionCompatibility() {}

    public static boolean genericPreDamageReactionRegistryIsAvailable() {
        return true;
    }

    public static boolean authoritativeReactionEscapeMovementIsAvailable() {
        return true;
    }

    public static boolean telepathyHookIsParityBacked() {
        return true;
    }

    public static boolean ordinaryMoveResolutionInvokesPreDamageReactions() {
        return false;
    }

    public static boolean minecraftMayExecutePreDamageReactionRules() {
        return false;
    }

    public static boolean minecraftMayRenderAuthoritativeReactionEvents() {
        return UpstreamCompatibilityMatrix.mayProjectAuthoritativeBehavior(
                UpstreamCompatibilityMatrix.Capability.ABILITIES)
                && UpstreamCompatibilityMatrix.mayProjectAuthoritativeBehavior(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK);
    }

    public static String adapterPolicy() {
        return "Render only reaction movement and rule-effect events already emitted by AutoPTU-Java. "
                + "Do not invoke the pre-damage registry from Minecraft, construct threatened tiles, "
                + "decide Telepathy eligibility, choose the escape destination, mutate action economy, "
                + "or cancel hit, damage or type effectiveness. Wait until the authoritative ordinary "
                + "move-resolution path owns hook ordering and affected-area construction.";
    }
}
