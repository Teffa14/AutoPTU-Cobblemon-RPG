package io.autoptu.cobblemon.battlecore;

/**
 * Compatibility guard for the upstream generic PRE-damage reaction seam.
 *
 * <p>AutoPTU-Java owns reaction legality, state mutation, nested move execution and semantic events.
 * Minecraft remains projection-only and must not re-evaluate PTU reaction rules.</p>
 */
public final class PreDamageReactionCompatibility {
    public static final String INSPECTED_AUTOPTU_JAVA_SHA = "ab520743d8d99f06fa28fd4d6fa06a0c4ecd3fee";
    public static final String INSPECTED_AUTOPTU_PYTHON_SHA = "03321a2eba42437180fddf5c4b2570c50ba429a6";
    public static final String JAVA_PRE_DAMAGE_ORACLE_PIN_SHA = "16d228efa63aabecb67fa788959a359aac7f8f03";

    private PreDamageReactionCompatibility() {}

    public static boolean genericPreDamageReactionRegistryIsAvailable() { return true; }
    public static boolean genericPreDamageFollowUpMoveSeamIsAvailable() { return true; }
    public static boolean preDamageFollowUpExecutionPolicyIsFrozen() { return true; }
    public static boolean authoritativeReactionEscapeMovementIsAvailable() { return true; }
    public static boolean authoritativeAdjacentReactionPushPrimitiveIsAvailable() { return true; }
    public static boolean authoritativeThreatenedAreaContextIsAvailable() { return true; }
    public static boolean authoritativeEffectiveTargetKindIsAvailable() { return true; }
    public static boolean telepathyHookIsParityBacked() { return true; }
    public static boolean perceptionHookIsParityBacked() { return true; }
    public static boolean perceptionErrataHookIsParityBacked() { return true; }
    public static boolean parryHookIsParityBacked() { return true; }
    public static boolean shellShieldHookIsParityBacked() { return true; }
    public static boolean swayOracleContractIsFrozen() { return true; }
    public static boolean swayHookIsParityBacked() { return true; }
    public static boolean swayLiveRuntimeFollowUpWiringIsAvailable() { return true; }
    public static boolean swayAuthoritativeExecutionIsAvailable() {
        return swayHookIsParityBacked()
                && authoritativeAdjacentReactionPushPrimitiveIsAvailable()
                && preDamageFollowUpExecutionPolicyIsFrozen()
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
        return "Consume only semantic reaction events and authoritative state already produced by AutoPTU-Java. "
                + "AutoPTU-Java owns PRE-damage registry invocation, threatened tiles, effective target kind, "
                + "readiness and usage bookkeeping, optional out-of-turn decisions, supported reaction movement, "
                + "nested follow-up move execution, action economy, HP/history mutation and hit/damage cancellation. "
                + "Current Java main contains the runtime-owned synchronous PRE-damage follow-up execution scope and "
                + "RuntimeMoveResolutionWithFollowUps boundary. Its live Sway regression proves redirected attacker/target "
                + "resolution through the authoritative runtime while reusing the original move and RNG, without spending "
                + "ordinary action economy or move frequency twice. The Sway hook remains responsible for STANDARD spend, "
                + "sway_used and sway_redirect bookkeeping, authoritative adjacent push selection, recursion protection and "
                + "original-hit cancellation. Current main also adds parity-backed Shell Shield through the same generic "
                + "PRE-damage registry; this does not broaden the adapter into ability-specific rule execution. Minecraft "
                + "may translate authoritative coordinates and render ordered semantic playback, but must not invoke the "
                + "reaction registry or follow-up executor, construct threatened tiles, classify a move, evaluate Sway or "
                + "Shell Shield eligibility, consume readiness, create usage guards, choose escape or push destinations, "
                + "recursively resolve a move, spend STANDARD, mutate combat stages/status/HP, or cancel a hit itself. "
                + "This promotes Sway end-to-end authoritative execution availability only. It does not imply complete "
                + "abilities, reactions, forced movement, terrain, status, items, Trainer Features, move-specific behavior, "
                + "full stateful damage parity or complete Minecraft playback.";
    }
}
