package io.autoptu.cobblemon.battlecore;

import java.util.EnumMap;
import java.util.Map;

/** Records exact read-only upstream heads and bounded evidence for the current integration slice. */
public final class CurrentUpstreamCompatibilityInspection {
    public static final String AUTOPTU_JAVA_SHA = "edf8db216ab88a10b896f2bb144cf5d08de49d8e";
    public static final String AUTOPTU_PYTHON_SHA = "0d56ea7b5a2b99a96f7ac4ca40b405e0ffbf83b8";

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
                        "EffectiveMoveTargetResolver derives affected tiles, current authoritative combatant positions, footprint overlap, line of sight and stable candidate order from BattleRuntimeState. Current HP eligibility excludes hp <= 0 while preserving inactive positive-HP candidates. RuntimeAreaMoveTargeting revalidates legal TILE choices against the live canonical moveset, frequency state and action space before expanding effective targets.",
                        "Minecraft must not supply effective target lists, live target anchors, footprint overlap, line-of-sight results, HP eligibility filters or a generic active-state filter. Target legality and expansion remain core-owned."));

        result.put(UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.VERIFIED,
                        "BattleRuntimeState supplies canonical initiative inputs. RuntimeInitiativeOrderAssembly and InitiativeRoundRebuilder own ordering and rollover. TrainerRuntimeState owns its ActionBudget and temporary AP grants. Multi-target execution consumes the declaration action and records move frequency exactly once outside sequential per-target resolution.",
                        "Minecraft must not spend or refund the declaration action, validate or record move frequency, provide initiative ordering, grant temporary AP, perform temporary AP grants/expiry, reset Trainer actions or perform per-target resource consumption."));

        result.put(UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "ROUND_START executes FieldRoundLifecycleHook at order 10, DelayedHitRoundLifecycleHook at order 20, RoundTemporaryEffectExpiryHook at order 30, server-owned TemporaryApGrant expiry and Trainer action reset at order 40, Pokemon round-temporary-effect cleanup at order 45, then DeclaredActionRoundLifecycleHook at order 50. BattleRuntimeState owns current round and immutable declared-action state.",
                        "Complete Python lifecycle parity is still broader. Minecraft must not supply currentRound, temporary-effect metadata, temporary AP grants, Trainer action reset, cleanup ordering, delayed maturity, queue/RNG mutation, send-out decisions or manufacture missing lifecycle effects."));

        result.put(UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "AutoPTU-Java main contains RuntimeMoveResolution.applyAreaUsingAuthoritativeCombatState. It revalidates a TILE declaration, expands authoritative targets, resolves targets sequentially through canonical accuracy/damage/PRE/post hooks and HP/history mutation, and consumes declaration-level action/frequency exactly once. The current main test suite also proves Telepathy can escape an authoritative declared area before damage without spending the normal Shift action.",
                        "This bounded AoE executor does not establish complete stateful damage parity for every move, ability, item, terrain or special. Minecraft may project returned events/state but must not re-run target loops, accuracy, damage, hooks, HP/history mutation or bookkeeping."));

        result.put(UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "StatusStateStore preserves canonical status entries. Selected target-owned prevention, Safeguard and spatial ability prevention are authoritative and emit generic semantic cues.",
                        "Complete ticking, expiry, cures, immunities and source-sensitive interactions remain partial. Minecraft must not evaluate missing status rules."));

        result.put(UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "DelayedHitBindingResolver expands stale target-id anchors and position-only delayed requests through EffectiveMoveTargetResolver. Stored target_position remains an authoritative aim anchor rather than forcing TILE semantics; current geometry, footprints, line of sight and HP eligibility determine affected combatants in stable order, while live target IDs follow their current authoritative position. The merged generic TILE multi-target execution boundary is authoritative for its verified contract.",
                        "This is bounded delayed-hit and generic TILE execution support, not complete move-special coverage. Minecraft must not choose delayed targets, supply RNG/combat inputs, consume or refund move frequency/actions, infer or execute unported specials, or manufacture missing forced-movement semantics."));

        result.put(UpstreamCompatibilityMatrix.Capability.TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "BattleEnvironmentState owns field state and lifecycle seams. The authoritative PRE-damage reaction pipeline is present, area execution supplies the authoritative area anchor to PRE-damage reactions, and main proves Telepathy movement can cancel the affected hit in an area declaration.",
                        "Full terrain, weather, hazards, zones, reactions and forced movement remain incomplete. Minecraft must not infer reaction legality or manufacture push, pull, knockback, interception or other interaction-driven movement."));

        result.put(UpstreamCompatibilityMatrix.Capability.ABILITIES,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "Representative generic hook families include combat-stage prevention/reflection, spatial status prevention, post-damage hooks and the PRE-damage reaction pipeline. Multi-target execution invokes that authoritative PRE/post pipeline per resolved target. AutoPTU-Java main edf8db21 adds an executable Telepathy area-execution proof while keeping the mechanic inside the generic semantic hook/event boundary.",
                        "Representative families do not establish complete ability parity. Open AutoPTU-Java PR #172 ports base Perception but is not merged and therefore is not enabled here. Minecraft may render semantic events only and must not calculate ability legality or effects."));

        result.put(UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "TrainerFeaturePrerequisiteResolution, TrainerFeatureContextResolution, TrainerFeatureFrequencyResolution, TrainerFeatureResourceResolution and TrainerFeatureUsageResolution are parity-backed for bounded families. AutoPTU Python main 0d56ea7b was inspected read-only; its newest changes are Career persistence recovery work and do not promote the Feature library.",
                        "Trainer Features remain partial. Minecraft must not grant Features, decide gates, mutate resources/AP or execute missing Feature effects."));

        result.put(UpstreamCompatibilityMatrix.Capability.AI_LEGAL_ACTION_INFRASTRUCTURE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.VERIFIED,
                        "RuntimeAutobattlerActionSpace derives immutable legal BattleChoice values from canonical state. TILE choices are revalidated before authoritative target expansion and execution.",
                        "AI or Minecraft may select only from core-produced legal choices. Tactical scoring remains separate and incomplete."));

        result.put(UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "Fabric/Cobblemon dedicated-server smoke coverage, authenticated identity, canonical roster reservation and generic semantic-event playback are present. The merged upstream executor unlocks authoritative multi-target event projection through the same projection-only boundary, including semantic PRE-damage ability cues already emitted by Java.",
                        "A successful authenticated graphical campaign encounter, RuntimeCombatantState materialization and complete battle playback remain pending. Minecraft must not execute PTU area rules, derive target order, roll damage, mutate HP/history or implement missing move, ability, reaction or forced-movement semantics."));

        return Map.copyOf(result);
    }
}
