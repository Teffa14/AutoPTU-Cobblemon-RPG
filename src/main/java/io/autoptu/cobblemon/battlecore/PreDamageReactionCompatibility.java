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
    public static final String INSPECTED_AUTOPTU_JAVA_SHA = "7a7a6d93cedf82aa16e427b166160b6e39756676";
    public static final String INSPECTED_AUTOPTU_PYTHON_SHA = "4b35bc2b37b7f3e536c3974982729025740fcd79";
    public static final String JAVA_PRE_DAMAGE_ORACLE_PIN_SHA = "16d228efa63aabecb67fa788959a359aac7f8f03";

    private PreDamageReactionCompatibility() {}

    public static boolean genericPreDamageReactionRegistryIsAvailable() { return true; }
    public static boolean genericPreDamageFollowUpMoveSeamIsAvailable() { return true; }
    public static boolean authoritativeReactionEscapeMovementIsAvailable() { return true; }
    public static boolean authoritativeAdjacentReactionPushPrimitiveIsAvailable() { return true; }
    public static boolean authoritativeThreatenedAreaContextIsAvailable() { return true; }
    public static boolean authoritativeEffectiveTargetKindIsAvailable() { return true; }
    public static boolean telepathyHookIsParityBacked() { return true; }
    public static boolean perceptionHookIsParityBacked() { return true; }
    public static boolean perceptionErrataHookIsParityBacked() { return true; }
    public static boolean parryHookIsParityBacked() { return true; }
    public static boolean swayOracleContractIsFrozen() { return true; }
    public static boolean swayHookIsParityBacked() { return true; }
    public static boolean swayLiveRuntimeFollowUpWiringIsAvailable() { return false; }
    public static boolean swayAuthoritativeExecutionIsAvailable() {
        return swayHookIsParityBacked()
                && authoritativeAdjacentReactionPushPrimitiveIsAvailable()
                && swayLiveRuntimeFollowUpWiringIsAvailable();
    }
    public static boolean perceptionReadyAndRoundScopedUsageAreCoreOwned() { return true; }
    public static boolean parryReadyAndRoundScopedUsageAreCoreOwned() { return true; }
    public static boolean swayUsageGuardAndStandardSpendAreCoreOwned() { return true; }
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
                + "selection, supported reaction movement, and hit/damage cancellation. Java main now includes the "
                + "parity-backed Sway hook, server-owned sway_used/sway_redirect bookkeeping, STANDARD spend, the "
                + "generic runtime-owned PRE-damage follow-up move seam, and the authoritative adjacent reaction "
                + "push primitive. However, the live RuntimePreDamageReactionContextFactory still constructs the "
                + "ordinary runtime context without a follow-up executor. Sway therefore cannot complete its nested "
                + "redirect through ordinary BattleRuntime move resolution and remains blocked end-to-end at the "
                + "adapter boundary. Minecraft may translate authoritative coordinates and render playback, but "
                + "must not invoke the registry or follow-up executor, construct threatened tiles, classify a move "
                + "as melee/ranged/area, consume perception_ready or parry_ready, create or mutate perception_used, "
                + "parry_used, sway_used or sway_redirect, choose escape or push destinations, recursively re-resolve "
                + "a move, spend STANDARD, mutate action economy, or cancel hit, damage or type effectiveness itself. "
                + "This verifies the Perception, Perception [Errata], Parry and Telepathy hooks plus the merged Sway "
                + "hook primitives and generic playback boundary; it does not promote Sway to live executable adapter "
                + "support until the authoritative runtime injects the follow-up executor. It does not imply complete "
                + "abilities, reactions, forced movement, terrain, status, items, Trainer Features, move-specific "
                + "behavior, or full stateful damage parity upstream.";
    }
}
