package io.autoptu.cobblemon.battlecore;

/**
 * Compatibility guard for the upstream generic PRE-damage reaction seam.
 *
 * <p>AutoPTU-Java exposes a generic pre-damage hook registry, authoritative
 * reaction-escape movement, a parity-backed Telepathy hook, frozen Python
 * ordering, and an authoritative runtime context factory that derives threatened
 * tiles from canonical BattleRuntimeState. The inspected Java ordinary
 * move-resolution entrypoint still does not invoke that registry, so Minecraft
 * must not trigger Telepathy, calculate threatened areas, choose a reaction
 * destination, or cancel hit/damage itself.</p>
 */
public final class PreDamageReactionCompatibility {
    public static final String INSPECTED_AUTOPTU_JAVA_SHA = "7a657fcca6d986a1010af65faa9dc2208eaa94a6";
    public static final String INSPECTED_AUTOPTU_PYTHON_SHA = "576d2922e04b065308eeda19f4d65f6b01219c80";
    public static final String JAVA_TELEPATHY_ORACLE_PIN_SHA = "16d228efa63aabecb67fa788959a359aac7f8f03";

    private PreDamageReactionCompatibility() {}

    public static boolean genericPreDamageReactionRegistryIsAvailable() {
        return true;
    }

    public static boolean authoritativeReactionEscapeMovementIsAvailable() {
        return true;
    }

    public static boolean authoritativeThreatenedAreaContextIsAvailable() {
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
                + "AutoPTU-Java now derives threatened tiles from canonical BattleRuntimeState through its "
                + "runtime pre-damage reaction context factory. Do not invoke the pre-damage registry from "
                + "Minecraft, construct or override threatened tiles, decide Telepathy eligibility, choose "
                + "the escape destination, mutate action economy, or cancel hit, damage or type effectiveness. "
                + "The Python oracle freezes ordering relative to ordinary resolution, shields, post-result "
                + "hooks, item bonuses and HP mutation, but Minecraft must wait until the authoritative Java "
                + "ordinary move-resolution path owns registry invocation.";
    }
}
