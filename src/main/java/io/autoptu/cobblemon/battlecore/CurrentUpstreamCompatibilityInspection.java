package io.autoptu.cobblemon.battlecore;

import java.util.EnumMap;
import java.util.Map;

/**
 * Records the exact upstream heads inspected for this integration slice and bounded current evidence.
 * This supplements, but never broadens, the permanent support classifications.
 */
public final class CurrentUpstreamCompatibilityInspection {
    public static final String AUTOPTU_JAVA_SHA = "1b4a38e871190844ae296a0fbb5966ea6f3da8bf";
    public static final String AUTOPTU_PYTHON_SHA = "e4bb0ca38b7018710af476ce365d515a387de4e7";

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

        result.put(UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.VERIFIED,
                        "BattleRuntimeState supplies canonical initiative inputs. RuntimeInitiativeOrderAssembly.fromState and InitiativeRoundRebuilder.authoritative own ordering and rollover. DelayedHitResourcePolicy preserves the originating action and frequency spend during ROUND_START.",
                        "Minecraft must not provide a precomputed rollover order, delayed-hit RNG, delayed-hit queue mutation, action/frequency bookkeeping, or a Trainer ID that collides with a combatant ID."));

        result.put(UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "BattleDelayedHitState owns the Python-compatible battle RNG stream. FieldRoundLifecycleHook at ROUND_START order 10 executes terrain -> zones -> rooms before DelayedHitRoundLifecycleHook at order 20 and DelayedHitLifecycleExecutor. Matured COMBATANT-target hits emit MoveResolvedEvent with the originating action/frequency spend unchanged and participate in damage-history rotation.",
                        "Complete lifecycle remains broader. TILE/area delayed hits remain unsupported. Trainer AP/temporary-AP, Air Lock and other Python round behavior remain pending. Minecraft must not advance field durations, inject lifecycle hooks, mature delayed hits, own the delayed queue or mutable RNG, or duplicate action/frequency bookkeeping."));

        result.put(UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "DelayedHitExecutionPolicy keeps a targetId as COMBATANT targeting at maturity, uses current authoritative RuntimeCombatantState.position while the defender exists, and falls back to the stored target position when the defender is missing without rewriting the move to TILE. A position-only delayed entry remains TILE targeting. Target resolution recomputes affected_tiles, footprint overlap and line of sight and preserves explicit target-id priority. DelayedHitResourcePolicy and BattleRuntimeState owns BattleDelayedHitState; DelayedHitRoundLifecycleHook automatically during ROUND_START calls RuntimeMoveResolution.applyDelayedUsingAuthoritativeCombatState with PythonRandom, HP mutation, damage-history and MoveResolvedEvent without a second action/frequency spend.",
                        "This is bounded delayed-hit execution. TILE/area delayed execution remains unsupported on main. The earlier exporter had a known review defect, so completeness is based on current code/tests rather than that bit. Minecraft must not freeze a live combatant target to the stored scheduling position, precompute affected tiles, footprint overlap or line of sight, supply RNG/combat inputs, consume or refund move frequency/actions, rewrite target mode, or implement missing-target area selection."));

        result.put(UpstreamCompatibilityMatrix.Capability.TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "BattleEnvironmentState owns duration-bearing FieldEffectEntry state. FieldRoundLifecycleHook executes FieldRoundProgression, updates the authoritative environment, emits FieldEffectEndedEvent and applies FieldStatusCleanupRequest before delayed-hit maturity.",
                        "This is partial field-system support. Full terrain effects, weather progression, hazards, zones, reactions and forced movement remain incomplete. Minecraft must not create PTU field entries or perform Wonder Room cleanup."));

        result.put(UpstreamCompatibilityMatrix.Capability.ABILITIES,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "Initiative-time ability behavior and delayed-hit combat preparation read authoritative runtime state and rerun authoritative move/damage/post-damage hooks during ROUND_START.",
                        "The complete PTU ability library is not ported; Minecraft must not grant abilities or supply hook results."));

        result.put(UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "TrainerRuntimeState owns Features, AP, initiative inputs, explicit initiative Speed and team identity; Rider Agility Training and Hardened Initiative are server-owned consumers.",
                        "Only bounded Trainer Feature/skill/initiative consumers exist; Minecraft must not grant Features, execute perks or calculate their outcomes."));

        result.put(UpstreamCompatibilityMatrix.Capability.AI_LEGAL_ACTION_INFRASTRUCTURE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.VERIFIED,
                        "BattleChoice is the legal decision contract. RuntimeAutobattlerActionSpace.legalChoices derives ShiftChoice and MoveChoice from BattleRuntimeState-owned position, geometry, affiliation, active/HP state, moveset, move-frequency usage, movement profile and ActionBudget, then returns an immutable stable-key-sorted list.",
                        "A client, AI or Minecraft adapter may select only from the core-produced BattleChoice list. It must not manufacture a ShiftChoice/MoveChoice, grant a move, bypass frequency/action budget, or replace targeting/range/LoS legality. Tactical scoring remains separate and incomplete."));

        result.put(UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.BLOCKING,
                        "Adapter-neutral entity-bound playback preserves field_effect and delayed move_resolved playback from the stable event contract; the runtime environment seed remains headless integration infrastructure.",
                        "No Fabric/Cobblemon/Craftics runtime has executed this boundary yet, so live adapter/playback remains blocking."));

        return Map.copyOf(result);
    }
}
