package io.autoptu.cobblemon.battlecore;

import java.util.LinkedHashMap;
import java.util.Map;

/** Compatibility gate for ordinary multi-target move execution. */
public final class MultiTargetMoveExecutionCompatibility {
    public static final String INSPECTED_AUTOPTU_JAVA_SHA = "edf8db216ab88a10b896f2bb144cf5d08de49d8e";
    public static final String INSPECTED_AUTOPTU_PYTHON_SHA = "0d56ea7b5a2b99a96f7ac4ca40b405e0ffbf83b8";
    public static final String PYTHON_ORACLE_PIN_SHA = "16d228efa63aabecb67fa788959a359aac7f8f03";

    private MultiTargetMoveExecutionCompatibility() {}

    public static boolean ownershipContractIsParityBacked() {
        return true;
    }

    public static boolean javaExecutesAuthoritativeMultiTargetDamage() {
        return true;
    }

    /** Minecraft remains projection-only; it never executes PTU multi-target rules itself. */
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

    public static Map<UpstreamCompatibilityMatrix.Capability, String> dependencies() {
        LinkedHashMap<UpstreamCompatibilityMatrix.Capability, String> dependencies = new LinkedHashMap<>();
        dependencies.put(
                UpstreamCompatibilityMatrix.Capability.CORE_TARGETING,
                "RuntimeAreaMoveTargeting revalidates the TILE declaration and expands authoritative targets in stable order");
        dependencies.put(
                UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE,
                "the declaration consumes its action and records move frequency exactly once outside per-target resolution");
        dependencies.put(
                UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE,
                "RuntimeMoveResolution.applyAreaUsingAuthoritativeCombatState runs each target through authoritative accuracy, damage, PRE and post hooks, HP and history mutation");
        dependencies.put(
                UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR,
                "only the merged generic TILE/AoE execution contract is promoted; unported move specials remain core-owned and unavailable");
        dependencies.put(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK,
                "adapter may project ordered semantic BattleEvents and final authoritative state but may not re-run PTU rules");
        return Map.copyOf(dependencies);
    }

    public static String adapterPolicy() {
        return "AutoPTU-Java main provides authoritative multi-target TILE execution through "
                + "RuntimeMoveResolution.applyAreaUsingAuthoritativeCombatState. Minecraft may project the ordered "
                + "semantic BattleEvents and final authoritative state returned by that runtime. Minecraft must not "
                + "loop targets to execute PTU effects, choose target order, spend or refund actions, check or record "
                + "move frequency, roll accuracy or damage, run PRE/post hooks, or mutate HP/history. AutoPTU-Java main "
                + "also proves Telepathy movement inside this pipeline; Minecraft still only plays back the emitted "
                + "semantic event and authoritative movement/state. Unported move specials, reactions and forced-movement "
                + "semantics remain disabled unless Java emits authoritative results for them.";
    }
}
