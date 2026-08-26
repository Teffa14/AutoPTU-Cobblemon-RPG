package io.autoptu.cobblemon.battlecore;

import java.util.Set;

/**
 * Bounded compatibility gate for the authoritative area-target secondary-status path merged in
 * AutoPTU-Java PR #210. This class records integration authority only; it does not implement PTU
 * rules in Minecraft.
 */
public final class AreaSecondaryStatusCompatibility {
    public static final String AUTOPTU_JAVA_MAIN_SHA =
            "453210d46a04ebc52babc675ce7824f83991da5d";
    public static final String AUTOPTU_PYTHON_MAIN_SHA =
            "68427feebcc1728fd6bcb53b6520a82595ab956b";
    public static final String PINNED_PYTHON_BATTLE_ORACLE_SHA =
            "16d228efa63aabecb67fa788959a359aac7f8f03";
    public static final int MERGED_AREA_SECONDARY_STATUS_PR = 210;
    public static final int OPEN_SECONDARY_COMBAT_STAGE_PR = 211;
    public static final String OPEN_SECONDARY_COMBAT_STAGE_HEAD_SHA =
            "cb2eabaf7d9874c0a4738ddaecc5eb04eb8a4e00";

    private static final Set<UpstreamCompatibilityMatrix.Capability> DEPENDENCIES = Set.of(
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
        return true;
    }

    public static boolean secondaryCombatStageMayBeProjected() {
        return false;
    }

    public static boolean delayedSecondaryStatusMayBeProjected() {
        return false;
    }

    public static String mergedAreaBoundary() {
        return "AutoPTU-Java PR #210 is merged on main at 453210d46a04ebc52babc675ce7824f83991da5d. "
                + "BattleRuntime now supplies RuntimeMoveSpecialHooks.standardRegistry for each authoritative area target while the outer declaration retains one action/frequency spend. "
                + "RuntimeMultiTargetSecondaryStatusIntegrationTest verifies two Burst targets, Poison application to one target, Immunity status_block on the other, stable authoritative target ids, and exactly one declaration-level STANDARD/frequency consumption. "
                + "Minecraft/Cobblemon may project the resulting authoritative state/events for this path but must not calculate per-target effect rolls, parse effects text, choose statuses, run prevention hooks, or mutate status state.";
    }

    public static String draftCombatStageBoundary() {
        return "AutoPTU-Java draft PR #211 at cb2eabaf7d9874c0a4738ddaecc5eb04eb8a4e00 freezes Python-compatible generic secondary Combat Stage parsing into ordered semantic stage-change requests. "
                + "The draft explicitly does not mutate combat-stage state; authoritative application, prevention and reflection remain owned by CombatStageMutationService and its registries. "
                + "Green parser/parity checks are reference evidence only while the PR is open and no live runtime mutation contract is merged. Minecraft/Cobblemon must not parse stage text, apply stage changes, evaluate prevention/reflection, or synthesize semantic stage events.";
    }

    public static String pythonOracleObservation() {
        return "AutoPTU Python main 68427feebcc1728fd6bcb53b6520a82595ab956b contains a Career draw-dialogue change and does not replace the battle oracle. "
                + "Battle parity for this feature remains pinned to AutoPTU 16d228efa63aabecb67fa788959a359aac7f8f03, including _generic_post_damage_from_text semantics used by the Java secondary-effect contracts.";
    }
}
