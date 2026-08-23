package io.autoptu.cobblemon.battlecore;

/**
 * Compatibility guard for the upstream generic PRE-damage reaction seam.
 *
 * <p>AutoPTU-Java exposes a generic pre-damage hook registry, authoritative
 * reaction-escape movement, a parity-backed Telepathy hook, and a frozen Python
 * oracle for PRE-damage ordering. The inspected Java ordinary move-resolution
 * entrypoint still does not invoke that registry, so Minecraft must not trigger
 * Telepathy, calculate threatened areas, choose a reaction destination, or cancel
 * hit/damage itself.</p>
 */
public final class PreDamageReactionCompatibility {
    public static final String INSPECTED_AUTOPTU_JAVA_SHA = "9819146364b67da51d039c5d380c8a4aa3c378c5";
    public static final String INSPECTED_AUTOPTU_PYTHON_SHA = "8d7de9f70d301e136672b66f460f9233a463cc7a";
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

    public static boolean preDamagePipelineOrderingIsParityBacked() {
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
                + "or cancel hit, damage or type effectiveness. The Python oracle now freezes ordering "
                + "relative to ordinary resolution, shields, post-result hooks, item bonuses and HP mutation, "
                + "but Minecraft must wait until the authoritative Java ordinary move-resolution path owns "
                + "registry invocation and affected-area construction.";
    }
}
