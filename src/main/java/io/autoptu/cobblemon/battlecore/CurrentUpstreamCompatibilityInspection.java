package io.autoptu.cobblemon.battlecore;

import java.util.EnumMap;
import java.util.Map;

/** Records exact read-only upstream heads and bounded evidence for the current integration slice. */
public final class CurrentUpstreamCompatibilityInspection {
    public static final String AUTOPTU_JAVA_SHA = "4f16e07862008b8fb00ee405a9cbc160ae8fbcec";
    public static final String AUTOPTU_PYTHON_SHA = "928c31a7b72243434536fdf05731ced421403f08";

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
        EnumMap<UpstreamCompatibilityMatrix.Capability, Evidence> result =
                new EnumMap<>(UpstreamCompatibilityMatrix.Capability.class);

        result.put(UpstreamCompatibilityMatrix.Capability.CORE_TARGETING,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.VERIFIED,
                        "EffectiveMoveTargetResolver derives affected tiles, authoritative positions, footprint overlap, line of sight and stable candidate order from BattleRuntimeState. RuntimeAreaMoveTargeting revalidates legal TILE choices against the live canonical moveset, frequency state and action space before expanding effective targets.",
                        "Minecraft must not supply effective target lists, anchors, footprint overlap, line-of-sight results or eligibility filters. Target legality and expansion remain core-owned."));

        result.put(UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.VERIFIED,
                        "BattleRuntimeState owns action budgets and initiative state. Multi-target execution consumes the declaration action and records move frequency exactly once outside sequential per-target resolution.",
                        "Minecraft must not spend or refund the declaration action, validate or record move frequency, or perform per-target resource consumption."));

        result.put(UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "ROUND_START has ordered field, delayed-hit, temporary-effect, Trainer reset and declared-action cleanup hooks with canonical BattleRuntimeState ownership.",
                        "Complete Python lifecycle parity is still broader. Minecraft must not advance rounds, run cleanup, schedule delayed maturity or manufacture missing lifecycle effects."));

        result.put(UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "AutoPTU-Java main now contains RuntimeMoveResolution.applyAreaUsingAuthoritativeCombatState. It revalidates a TILE declaration, expands authoritative targets, resolves targets sequentially through canonical accuracy/damage/PRE/post hooks and HP/history mutation, and consumes declaration-level action/frequency exactly once.",
                        "This bounded AoE executor does not establish complete stateful damage parity for every move, ability, item, terrain or special. Minecraft may project returned events/state but must not re-run target loops, accuracy, damage, hooks, HP/history mutation or bookkeeping."));

        result.put(UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "StatusStateStore, selected status prevention, Safeguard and spatial ability prevention are authoritative and emit generic semantic cues.",
                        "Complete ticking, expiry, cures, immunities and source-sensitive interactions remain partial. Minecraft must not evaluate missing status rules."));

        result.put(UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "Delayed-hit binding/execution and the merged generic TILE multi-target execution boundary are authoritative for their verified contracts.",
                        "This does not promote the wider move-special library. Minecraft must not infer or execute unported specials, including missing forced-movement semantics."));

        result.put(UpstreamCompatibilityMatrix.Capability.TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "BattleEnvironmentState, field lifecycle seams and the authoritative PRE-damage reaction pipeline are present. Area execution supplies the authoritative area anchor to PRE-damage reactions.",
                        "Full terrain, weather, hazards, zones, reactions and forced movement remain incomplete. Minecraft must not infer reaction legality or manufacture push/pull/knockback."));

        result.put(UpstreamCompatibilityMatrix.Capability.ABILITIES,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "Representative generic hook families include combat-stage prevention/reflection, spatial status prevention, post-damage hooks and the PRE-damage reaction pipeline. Multi-target execution now invokes that authoritative PRE/post pipeline per resolved target.",
                        "Representative families do not establish complete ability parity. Minecraft may render semantic events only and must not calculate ability legality or effects."));

        result.put(UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "TrainerFeaturePrerequisiteResolution, TrainerFeatureContextResolution, TrainerFeatureFrequencyResolution, TrainerFeatureResourceResolution and TrainerFeatureUsageResolution are parity-backed for bounded families. AutoPTU Python main 928c31a7 was inspected read-only; its newest changes are Career persistence/deploy work and do not promote the Feature library.",
                        "Trainer Features remain partial. Minecraft must not grant Features, decide gates, mutate resources/AP or execute missing Feature effects."));

        result.put(UpstreamCompatibilityMatrix.Capability.AI_LEGAL_ACTION_INFRASTRUCTURE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.VERIFIED,
                        "RuntimeAutobattlerActionSpace derives immutable legal BattleChoice values from canonical state. TILE choices are revalidated before authoritative target expansion and execution.",
                        "AI or Minecraft may select only from core-produced legal choices. Tactical scoring remains separate and incomplete."));

        result.put(UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "Fabric/Cobblemon dedicated-server smoke coverage, authenticated identity, canonical roster reservation and generic semantic-event playback are present. The merged upstream executor now unlocks authoritative multi-target event projection through the same projection-only boundary.",
                        "A successful authenticated graphical campaign encounter and complete battle playback remain pending. Minecraft must not execute PTU area rules, derive target order, roll damage, mutate HP/history or implement missing move/ability/reaction semantics."));

        return Map.copyOf(result);
    }
}
