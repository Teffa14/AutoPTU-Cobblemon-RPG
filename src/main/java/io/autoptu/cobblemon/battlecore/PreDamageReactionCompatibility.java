package io.autoptu.cobblemon.battlecore;

/**
 * Compatibility guard for the upstream generic PRE-damage reaction seam.
 *
 * <p>AutoPTU-Java owns the generic PRE-damage registry invocation, derives reaction context from
 * canonical battle state, applies supported reaction movement in core, and emits semantic events.
 * Minecraft remains projection-only and must not re-evaluate Perception, Perception [Errata], Parry,
 * Telepathy, Sway, or other PTU rules.</p>
 */
public final class PreDamageReactionCompatibility {
    public static final String INSPECTED_AUTOPTU_JAVA_SHA = "d829224655f057d19a7470a0bc5cfa1f0bdefeda";
    public static final String INSPECTED_AUTOPTU_PYTHON_SHA = "7605265f2548a3967b2de3eb00cc0db33b0e9303";
    public static final String JAVA_PRE_DAMAGE_ORACLE_PIN_SHA = "16d228efa63aabecb67fa788959a359aac7f8f03";

    private PreDamageReactionCompatibility() {}

    public static boolean genericPreDamageReactionRegistryIsAvailable() { return true; }
    public static boolean authoritativeReactionEscapeMovementIsAvailable() { return true; }
    public static boolean authoritativeThreatenedAreaContextIsAvailable() { return true; }
    public static boolean authoritativeEffectiveTargetKindIsAvailable() { return true; }
    public static boolean telepathyHookIsParityBacked() { return true; }
    public static boolean perceptionHookIsParityBacked() { return true; }
    public static boolean perceptionErrataHookIsParityBacked() { return true; }
    public static boolean parryHookIsParityBacked() { return true; }
    public static boolean swayOracleContractIsFrozen() { return true; }
    public static boolean swayAuthoritativeExecutionIsAvailable() { return false; }
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
                + "selection, supported reaction movement, and hit/damage cancellation. The Sway Python contract is "
                + "frozen upstream, but Java does not yet provide authoritative recursive redirect plus post-redirect "
                + "push execution, so Sway remains blocked at the adapter boundary. Minecraft may translate authoritative "
                + "coordinates and render playback, but must not invoke the registry, construct threatened tiles, classify "
                + "a move as melee/ranged/area, consume perception_ready or parry_ready, create perception_used, parry_used, "
                + "sway_used or sway_redirect, choose escape or push destinations, recursively re-resolve a move, mutate "
                + "action economy, or cancel hit, damage or type effectiveness itself. This verifies the Perception, "
                + "Perception [Errata], Parry and Telepathy hooks plus the generic playback boundary; it records Sway only "
                + "as a frozen oracle contract and does not imply complete abilities, reactions, forced movement, terrain, "
                + "status, items, Trainer Features, AoE behavior, or the full stateful damage pipeline upstream.";
    }
}