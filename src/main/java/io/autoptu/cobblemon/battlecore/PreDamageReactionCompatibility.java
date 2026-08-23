package io.autoptu.cobblemon.battlecore;

/**
 * Compatibility guard for the upstream generic PRE-damage reaction seam.
 *
 * <p>AutoPTU-Java now owns the generic PRE-damage registry invocation for
 * ordinary authoritative move resolution, derives the reaction context from
 * canonical BattleRuntimeState, applies reaction movement in core, and emits
 * semantic events. Minecraft remains projection-only: it must not re-evaluate
 * Telepathy or any other PTU reaction rule.</p>
 */
public final class PreDamageReactionCompatibility {
    public static final String INSPECTED_AUTOPTU_JAVA_SHA = "28f141be5471e23f660fb2cda09bab02244ee62e";
    public static final String INSPECTED_AUTOPTU_PYTHON_SHA = "894f66771ca3f0d3c331f86c3ab888cdc38dd6f9";
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
        return true;
    }

    public static boolean semanticReactionPlaybackFixtureIsAvailable() {
        return true;
    }

    public static boolean minecraftMayExecutePreDamageReactionRules() {
        return false;
    }

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
                + "AutoPTU-Java owns PRE-damage registry invocation, derives threatened tiles from canonical "
                + "BattleRuntimeState, applies reaction escape movement, and cancels or adjusts the authoritative "
                + "move result before later damage stages. Minecraft may translate authoritative coordinates and "
                + "render playback, but must not invoke the reaction registry, construct or override threatened "
                + "tiles, decide Telepathy eligibility, choose escape destinations, mutate action economy, or "
                + "cancel hit, damage or type effectiveness itself. The semantic playback fixture verifies stable "
                + "combatant identity, authoritative event ordering, grid-to-world translation and immutable "
                + "projection inputs only. This promotion verifies the ordinary runtime seam; it does not imply "
                + "complete abilities, reactions, forced movement, terrain, status, item, AoE execution or "
                + "stateful-damage coverage upstream.";
    }
}
