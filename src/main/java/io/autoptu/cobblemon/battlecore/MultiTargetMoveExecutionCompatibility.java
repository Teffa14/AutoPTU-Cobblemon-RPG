package io.autoptu.cobblemon.battlecore;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Compatibility gate for ordinary multi-target move execution.
 *
 * <p>AutoPTU-Java freezes the Python ownership contract for resolving effective targets
 * sequentially while declaration-level action economy and move-frequency bookkeeping happen once.
 * The frozen policy is not an implementation of authoritative multi-target damage. Minecraft must
 * therefore remain projection-only until Java exposes the completed runtime execution contract.</p>
 */
public final class MultiTargetMoveExecutionCompatibility {
    public static final String INSPECTED_AUTOPTU_JAVA_SHA = "14662fb67778e71f2d55fc7a74c43dd9a8b06fa1";
    public static final String INSPECTED_AUTOPTU_PYTHON_SHA = "cd4668a1b0e7c995bc12f3768f7b04cfa0f1c896";
    public static final String PYTHON_ORACLE_PIN_SHA = "16d228efa63aabecb67fa788959a359aac7f8f03";

    private MultiTargetMoveExecutionCompatibility() {}

    public static boolean ownershipContractIsParityBacked() {
        return true;
    }

    public static boolean javaExecutesAuthoritativeMultiTargetDamage() {
        return false;
    }

    public static boolean minecraftMayExecuteMultiTargetDamage() {
        return false;
    }

    public static boolean minecraftMayProjectResolvedMultiTargetEvents() {
        return javaExecutesAuthoritativeMultiTargetDamage()
                && UpstreamCompatibilityMatrix.mayProjectAuthoritativeBehavior(
                UpstreamCompatibilityMatrix.Capability.CORE_TARGETING)
                && UpstreamCompatibilityMatrix.mayProjectAuthoritativeBehavior(
                UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE)
                && UpstreamCompatibilityMatrix.mayProjectAuthoritativeBehavior(
                UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE)
                && UpstreamCompatibilityMatrix.mayProjectAuthoritativeBehavior(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK);
    }

    /** Feature-to-upstream compatibility mapping for this integration slice. */
    public static Map<UpstreamCompatibilityMatrix.Capability, String> dependencies() {
        LinkedHashMap<UpstreamCompatibilityMatrix.Capability, String> dependencies = new LinkedHashMap<>();
        dependencies.put(
                UpstreamCompatibilityMatrix.Capability.CORE_TARGETING,
                "authoritative TILE anchor validation and affected-target expansion");
        dependencies.put(
                UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE,
                "ordinary action marks once; move frequency is checked/recorded once outside the per-target loop");
        dependencies.put(
                UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE,
                "each effective target must receive the authoritative per-target attack/damage pipeline");
        dependencies.put(
                UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR,
                "ordinary move target semantics and move-frequency bookkeeping remain core-owned");
        dependencies.put(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK,
                "adapter may only render semantic events and final state emitted by the authoritative runtime");
        return Map.copyOf(dependencies);
    }

    public static String adapterPolicy() {
        return "AutoPTU-Java currently freezes the Python multi-target ownership contract but does not yet "
                + "provide completed authoritative multi-target damage execution. Minecraft must not loop over "
                + "affected targets, spend or refund actions, check or record move frequency, roll attacks or "
                + "damage, apply HP changes, or infer target ordering. Keep AoE execution disabled until Java "
                + "returns authoritative per-target results/events; then project those outputs without re-running "
                + "PTU legality or bookkeeping in the adapter.";
    }
}
