package io.autoptu.cobblemon.battlecore;

import java.util.EnumMap;
import java.util.Map;

/** Records the exact upstream heads inspected for this integration slice and bounded current evidence. */
public final class CurrentUpstreamCompatibilityInspection {
    public static final String AUTOPTU_JAVA_SHA = "cdb229db787ac93f28745f796c1d9944546676cc";
    public static final String AUTOPTU_PYTHON_SHA = "0d1cc8f3bd791485ed52f7b5e9cd63c0965ad944";

    public record Evidence(UpstreamCompatibilityMatrix.Support support, String contracts, String limitation) {
        public Evidence {
            if (support == null) throw new IllegalArgumentException("support is required");
            if (contracts == null || contracts.isBlank()) throw new IllegalArgumentException("contracts are required");
            if (limitation == null || limitation.isBlank()) throw new IllegalArgumentException("limitation is required");
        }
    }

    private static final Map<UpstreamCompatibilityMatrix.Capability, Evidence> EVIDENCE = build();
    private CurrentUpstreamCompatibilityInspection() {}

    public static Evidence evidence(UpstreamCompatibilityMatrix.Capability capability) {
        Evidence evidence = EVIDENCE.get(capability);
        if (evidence == null) throw new IllegalArgumentException("no current inspection evidence for " + capability);
        return evidence;
    }

    public static Map<UpstreamCompatibilityMatrix.Capability, Evidence> evidence() { return EVIDENCE; }

    private static Map<UpstreamCompatibilityMatrix.Capability, Evidence> build() {
        EnumMap<UpstreamCompatibilityMatrix.Capability, Evidence> result = new EnumMap<>(UpstreamCompatibilityMatrix.Capability.class);
        result.put(UpstreamCompatibilityMatrix.Capability.CORE_TARGETING, new Evidence(
                UpstreamCompatibilityMatrix.Support.VERIFIED,
                "EffectiveMoveTargetResolver derives affected tiles, authoritative positions, footprint overlap, line of sight and stable target order from BattleRuntimeState.",
                "Minecraft must not supply effective targets, footprint overlap, line-of-sight results or HP eligibility filters."));
        result.put(UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE, new Evidence(
                UpstreamCompatibilityMatrix.Support.VERIFIED,
                "BattleRuntimeState owns initiative inputs, action budgets and deterministic rollover; delayed-hit resource bookkeeping and Trainer temporary AP remain core-owned.",
                "Minecraft must not provide initiative order, queue/RNG mutation, action/frequency bookkeeping or Trainer AP mutation."));
        result.put(UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE, new Evidence(
                UpstreamCompatibilityMatrix.Support.PARTIAL,
                "ROUND_START has ordered field, delayed-hit, temporary-effect, Trainer action reset and declared-action cleanup hooks.",
                "Complete lifecycle parity remains broader; Minecraft must not advance currentRound or run lifecycle hooks."));
        result.put(UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE, new Evidence(
                UpstreamCompatibilityMatrix.Support.PARTIAL,
                "StatusStateStore preserves ordered stacked StatusEntry values and repeated normalized names. Current Java main also ports TrainerFeatureEffectRegistry apply_status/remove_status against the Python list/status oracle.",
                "Representation and selected Feature status mutations are verified, while complete status ticking, expiry, cures, immunities and interactions remain partial. Minecraft must not interpret or execute missing rules."));
        result.put(UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR, new Evidence(
                UpstreamCompatibilityMatrix.Support.PARTIAL,
                "Delayed-hit binding reuses authoritative effective-target resolution and current geometry.",
                "This is bounded delayed-hit support, not complete move-special coverage."));
        result.put(UpstreamCompatibilityMatrix.Capability.TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS, new Evidence(
                UpstreamCompatibilityMatrix.Support.PARTIAL,
                "BattleEnvironmentState and duration-bearing field entries have bounded authoritative progression.",
                "Full terrain, weather, hazards, zones, reactions and forced movement remain incomplete."));
        result.put(UpstreamCompatibilityMatrix.Capability.ABILITIES, new Evidence(
                UpstreamCompatibilityMatrix.Support.PARTIAL,
                "Selected phase, initiative, damage and post-damage ability behavior is authoritative and parity-backed.",
                "The complete ability library is not ported; Minecraft must not execute missing hooks."));
        result.put(UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS, new Evidence(
                UpstreamCompatibilityMatrix.Support.PARTIAL,
                "TrainerFeatureExecutionService keeps effect-before-resource/bookkeeping order. Current main includes heal/heal_active, raise_cs, grant_temp_hp, grant_ap, apply_status and remove_status handlers with authoritative target/state resolution.",
                "Trainer Features remain partial because the wider Python effect library, full catalog and lifecycle/resource semantics are incomplete; Minecraft must not grant Features, select targets, execute effects or spend resources."));
        result.put(UpstreamCompatibilityMatrix.Capability.AI_LEGAL_ACTION_INFRASTRUCTURE, new Evidence(
                UpstreamCompatibilityMatrix.Support.VERIFIED,
                "RuntimeAutobattlerActionSpace produces immutable server-owned legal BattleChoice values from current runtime state.",
                "AI and clients may select only from current core-produced choices; tactical scoring remains separate."));
        result.put(UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK, new Evidence(
                UpstreamCompatibilityMatrix.Support.PARTIAL,
                "Fabric 1.21.1 + Cobblemon 1.7.3 production-remapped CI verifies identity-only interception, authoritative relocation/HP projection and world-scoped persistence. FabricCanonicalPlayerStoreRuntime now opens FileVersionedCanonicalStateRepository, FileCanonicalPlayerEncounterProfileRepository, FileCanonicalPokemonRepository and FileCanonicalItemReservationRepository under one world root, with the item repository resolving Pokemon only through the durable canonical Pokemon store. The two-process restart smoke requires Trainer, encounter profile, Pokemon aggregate, item quantity and an active item reservation to survive reopening the same world.",
                "The authenticated graphical client encounter, cross-aggregate transaction recovery, reservation reconciliation/expiry, RuntimeCombatantState materialization, faint presentation and complete playback remain pending. Minecraft/Cobblemon state never supplies PTU stats, inventory truth, modifiers, legality or outcomes."));
        return Map.copyOf(result);
    }
}
