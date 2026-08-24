package io.autoptu.cobblemon.battlecore;

/**
 * Compatibility guard for the upstream generic PRE-damage reaction seam.
 *
 * <p>AutoPTU-Java owns the generic PRE-damage registry invocation, derives reaction context from
 * canonical battle state, applies reaction movement in core, and emits semantic events. Minecraft
 * remains projection-only and must not re-evaluate Perception, Perception [Errata], Parry,
 * Telepathy, or other PTU rules.</p>
 */
public final class PreDamageReactionCompatibility {
    public static final String INSPECTED_AUTOPTU_JAVA_SHA = "5bb41ed8d137e3067258d38e9dd04b2cf0840750";
    public static final String INSPECTED_AUTOPTU_PYTHON_SHA = "7605265f2548a3967b2de3eb00cc0db33b0e9303";
    public static final String JAVA_TELEPATHY_ORACLE_PIN_SHA = "16d228efa63aabecb67fa788959a359aac7f8f03";

    private PreDamageReactionCompatibility() {}

    public static boolean genericPreDamageReactionRegistryIsAvailable() { return true; }
    public static boolean authoritativeReactionEscapeMovementIsAvailable() { return true; }
    public static boolean authoritativeThreatenedAreaContextIsAvailable() { return true; }
    public static boolean authoritativeEffectiveTargetKindIsAvailable() { return true; }
    public static boolean telepathyHookIsParityBacked() { return true; }
    public static boolean perceptionHookIsParityBacked() { return true; }
    public static boolean perceptionErrataHookIsParityBacked() { return true; }
    public static boolean parryHookIsParityBacked() { return true; }
    public static boolean perceptionReadyAndRoundScopedUsageAreCoreOwned() { return true; }
    public static boolean parryReadyAndRoundScopedUsageAreCoreOwned() { return true; }
    public static boolean preDamagePipelineOrderingIsParityBacked() { return true; }
    public static boolean ordinaryMoveResolutionInvokesPreDamageReactions() { return true; }
    public static boolean semanticReactionPlaybackFixtureIsAvailable() { return true; }
    public static boolean minecraftMayExecutePreDamageReactionRules() { return false; }

    public static boolean minecraftMayRenderAuthoritativeReactionEvents() {
        return ordinaryMoveResolutionInvokesPreDamageReactions()
                && semanticReactionPlaybackFixtureIsAvailable()
                && UpstreamCompatibilityMatrix.mayProjectAuthoritativeBehavior(
                UpstreamCompatibilityMatrix.Capability.ABILITIES)
                && UpstreamCompatibilityMatrix.mayProjectAuthoritativeBehavior(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK);
    }

    public static String adapterPolicy() {
        return "Consume only reaction movement and rule-effect events already emitted by AutoPTU-Java. "
                + "AutoPTU-Java owns PRE-damage registry invocation, threatened tiles, effective target kind, "
                + "Perception and Parry readiness/round-scoped usage, optional out-of-turn decisions, safe-tile "
                + "selection, reaction movement, and hit/damage cancellation. Minecraft may translate authoritative "
                + "coordinates and render playback, but must not invoke the registry, construct threatened tiles, "
                + "classify a move as melee/ranged/area, consume perception_ready or parry_ready, create perception_used "
                + "or parry_used, choose escape destinations, mutate action economy, or cancel hit, damage or type "
                + "effectiveness itself. This verifies the Perception, Perception [Errata], Parry and Telepathy hooks "
                + "plus the generic playback boundary; it does not imply complete abilities, reactions, forced movement, "
                + "terrain, status, items, Trainer Features, AoE behavior, or the full stateful damage pipeline upstream.";
    }
}