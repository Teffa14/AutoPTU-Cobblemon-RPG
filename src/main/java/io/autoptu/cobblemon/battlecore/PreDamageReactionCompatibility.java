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
    public static final String INSPECTED_AUTOPTU_JAVA_SHA = "ab29df99b0ac884805cb90d115818ad92c62a35d";
    public static final String INSPECTED_AUTOPTU_PYTHON_SHA = "65702f3816162c804a926c228d54d405f3236a97";
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
    public static boolean swayOracleContractIsFrozen() { return true; }
    public static boolean swayHookIsParityBacked() { return true; }
    public static boolean swayLiveRuntimeFollowUpWiringIsAvailable() { return false; }
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
        return "Consume only reaction movement and rule-effect events already emitted by AutoPTU-Java. "
                + "AutoPTU-Java owns PRE-damage registry invocation, threatened tiles, effective target kind, "
                + "Perception and Parry readiness/round-scoped usage, optional out-of-turn decisions, safe-tile "
                + "selection, supported reaction movement, and hit/damage cancellation. Java main now includes the "
                + "parity-backed Sway hook, server-owned sway_used/sway_redirect bookkeeping, STANDARD spend, the "
                + "generic runtime-owned PRE-damage follow-up move seam, the authoritative adjacent reaction push "
                + "primitive, and the frozen PRE-damage follow-up execution policy. However, current Java main still "
                + "constructs ordinary RuntimePreDamageReactionContextFactory contexts without a live follow-up "
                + "executor. The executable wiring exists only in unmerged draft PR #179 and must not be treated as "
                + "available upstream authority. Sway therefore cannot complete its nested redirect through ordinary "
                + "BattleRuntime move resolution and remains blocked end-to-end at the adapter boundary. Minecraft "
                + "may translate authoritative coordinates and render playback, but must not invoke the registry or "
                + "follow-up executor, construct threatened tiles, classify a move as melee/ranged/area, consume "
                + "perception_ready or parry_ready, create or mutate perception_used, parry_used, sway_used or "
                + "sway_redirect, choose escape or push destinations, recursively re-resolve a move, spend STANDARD, "
                + "mutate action economy, or cancel hit, damage or type effectiveness itself. This verifies the "
                + "Perception, Perception [Errata], Parry and Telepathy hooks plus the merged Sway hook primitives, "
                + "the frozen follow-up execution contract and generic playback boundary; it does not promote Sway "
                + "to live executable adapter support until the authoritative runtime wiring merges to Java main. "
                + "It does not imply complete abilities, reactions, forced movement, terrain, status, items, Trainer "
                + "Features, move-specific behavior, or full stateful damage parity upstream.";
    }
}
