package io.autoptu.cobblemon.battlecore;

import java.util.Set;

/**
 * Bounded compatibility gate for the authoritative area-target secondary-status path merged in
 * AutoPTU-Java PR #210. This class records integration authority only; it does not implement PTU
 * rules in Minecraft.
 */
public final class AreaSecondaryStatusCompatibility {
    public static final String AUTOPTU_JAVA_MAIN_SHA =
            "7e1115df0c7937699f179dc1f23040c06c78f719";
    public static final String AUTOPTU_PYTHON_MAIN_SHA =
            "f0b8f2a31ac3626dfb5c51f9ee8195780fd3c560";
    public static final String PINNED_PYTHON_BATTLE_ORACLE_SHA =
            "16d228efa63aabecb67fa788959a359aac7f8f03";
    public static final int MERGED_AREA_SECONDARY_STATUS_PR = 210;
    public static final int MERGED_SECONDARY_COMBAT_STAGE_PARSER_PR = 211;
    public static final int OPEN_SECONDARY_COMBAT_STAGE_APPLICATION_PR = 212;
    public static final String OPEN_SECONDARY_COMBAT_STAGE_APPLICATION_HEAD_SHA =
            "0b3f005dded417f59175c9424b545580cf654e2e";

    private static final Set<UpstreamCompatibilityMatrix.Capability> DEPENDENCIES = Set.of(
            UpstreamCompatibilityMatrix.Capability.CORE_TARGETING,
            UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE,
            UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE,
            UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE,
            UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR,
            UpstreamCompatibilityMatrix.Capability.ABILITIES,
            UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS,
            UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK);

    private AreaSecondaryStatusCompatibility() {}

    public static Set<UpstreamCompatibilityMatrix.Capability> dependencies() {
        return DEPENDENCIES;
    }

    public static boolean authoritativeAreaSecondaryStatusMayBeProjected() {
        return DEPENDENCIES.stream().allMatch(UpstreamCompatibilityMatrix::mayProjectAuthoritativeBehavior);
    }

    public static boolean secondaryCombatStageMayBeProjected() {
        return false;
    }

    public static boolean delayedSecondaryStatusMayBeProjected() {
        return false;
    }

    public static String mergedAreaBoundary() {
        return "AutoPTU-Java PR #210 is merged on main. BattleRuntime supplies RuntimeMoveSpecialHooks.standardRegistry for each authoritative area target while the outer declaration retains one action/frequency spend. "
                + "RuntimeMultiTargetSecondaryStatusIntegrationTest verifies two Burst targets, Poison application to one target, Immunity status_block on the other, stable authoritative target ids, and exactly one declaration-level STANDARD/frequency consumption. "
                + "Projection is permitted only while every declared upstream dependency remains non-BLOCKING. Minecraft/Cobblemon must not calculate per-target effect rolls, parse effects text, choose statuses, run prevention hooks, or mutate status state.";
    }

    public static String combatStageBoundary() {
        return "AutoPTU-Java PR #211 is merged on main at 7e1115df0c7937699f179dc1f23040c06c78f719 and freezes Python-compatible generic secondary Combat Stage parsing into ordered semantic stage-change requests. "
                + "The merged parser does not mutate combat-stage state. AutoPTU-Java draft PR #212 at 0b3f005dded417f59175c9424b545580cf654e2e composes supported ATK/DEF/SPATK/SPDEF/SPD requests with CombatStageMutationService, including prevention and post-apply reactions, while Accuracy and Evasion fail closed before mutation. "
                + "PR #212 remains draft and does not establish live BattleRuntime execution. Minecraft/Cobblemon therefore must not parse stage text, apply stage changes, evaluate prevention/reflection, or synthesize semantic stage events.";
    }

    public static String pythonOracleObservation() {
        return "AutoPTU Python main f0b8f2a31ac3626dfb5c51f9ee8195780fd3c560 changes Career sponsor timeline presentation and does not replace the battle oracle. "
                + "Battle parity for this feature remains pinned to AutoPTU 16d228efa63aabecb67fa788959a359aac7f8f03, including _generic_post_damage_from_text semantics used by the Java secondary-effect contracts.";
    }
}
