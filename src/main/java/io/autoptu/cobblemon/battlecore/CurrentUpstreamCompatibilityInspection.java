package io.autoptu.cobblemon.battlecore;

import java.util.EnumMap;
import java.util.Map;

/**
 * Records the exact upstream heads inspected for this integration slice and the bounded capability
 * deltas that landed after the last broad matrix refresh. It supplements, but never broadens,
 * UpstreamCompatibilityMatrix support classifications.
 */
public final class CurrentUpstreamCompatibilityInspection {
    public static final String AUTOPTU_JAVA_SHA = "ce990c84ad133f9b0b56f774e2a59c8cb0c4d90b";
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
                        "BattleRuntimeState supplies canonical initiative inputs. RuntimeInitiativeOrderAssembly.fromState derives Pokemon candidates, Trainer entries, Trick Room ordering and League ordering internally. InitiativeRoundRebuilder.authoritative composes state-derived assembly with InitiativeAssemblyInstaller during rollover. BattleRoundController.advanceInitiativeTurnWithRollover() is the default production path and selects the authoritative rebuilder internally; the injectable rebuilder overload is deprecated for parity/migration tests. BattleEnvironmentState owns ordering/environment state and TrainerRuntimeState owns server-side Trainer initiative/action inputs. DelayedHitResourcePolicy plus BattleRuntimeState-owned delayed-hit state preserve the originating move declaration's action and frequency spend when a combatant-target delayed hit matures during ROUND_START.",
                        "Minecraft must never supply Trick Room or League ordering flags, initial action availability, actor kind, Trainer action state, resolved Speed, Trainer team, InitiativeEntry, participant filters, a precomputed rollover order, InitiativeRoundRebuilder, any alternative rollover strategy, delayed-hit RNG, delayed-hit queue mutation or delayed-hit action/frequency bookkeeping. The integration also rejects any Trainer ID that collides with a reserved combatant ID before runtime assembly."));
        result.put(UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "The default production rollover call advances the round, derives and installs the next mixed Trainer/Pokemon initiative order only from BattleRuntimeState, applies initiative temporary-effect cleanup, resets the initiative cursor and opens the next actor turn without an adapter-supplied rebuilder. BattleRuntimeState owns BattleDelayedHitState, including the delayed queue and single Python-compatible battle RNG stream. BuiltinLifecycleHooks registers FieldRoundLifecycleHook at ROUND_START order 10 and DelayedHitRoundLifecycleHook at order 20, so canonical terrain -> zones -> rooms progression completes before due COMBATANT-target delayed hits mature through DelayedHitLifecycleExecutor. Matured hits reuse ordinary authoritative move resolution, mutate HP, emit MoveResolvedEvent, leave the originating action/frequency spend unchanged, remove the due queue entry and participate in later Python-compatible damage-history rotation.",
                        "Complete lifecycle remains broader than these bounded services. Due TILE/area delayed hits remain unsupported, and Trainer AP/temporary-AP, send-out Features, Air Lock, Arena Trap, Intimidate, Impostor and other Python round-start behavior remain core-owned follow-up work. Minecraft must not advance field durations, remove Wonder Room statuses, own the delayed queue or mutable RNG, inject lifecycle hooks, choose lifecycle ordering, mature delayed hits, or duplicate action/frequency bookkeeping."));
        result.put(UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "DelayedHitExecutionPolicy freezes target-resolution re-entry and the current delayed target-geometry contract. A delayed entry with a canonical targetId remains COMBATANT targeting at maturity and now binds its aim anchor to that combatant's current authoritative RuntimeCombatantState.position rather than the stored scheduling coordinate. A position-only delayed entry remains TILE targeting. The pinned Python target-resolution contract also recomputes affected_tiles, footprint overlap and line of sight at maturity. DelayedHitResourcePolicy freezes resource ownership, BattleRuntimeState owns BattleDelayedHitState and its RNG, and BuiltinLifecycleHooks invokes DelayedHitRoundLifecycleHook automatically during ROUND_START after field progression. RuntimeMoveResolution.applyDelayedUsingAuthoritativeCombatState re-derives effective metadata and combat inputs from current BattleRuntimeState before executing the normal PythonRandom, HP mutation, damage-history and MoveResolvedEvent path without a second action/frequency spend.",
                        "This is bounded delayed-hit execution and target geometry, not complete delayed-move parity or the full move library. TILE/area delayed execution remains unsupported on main even though the Python geometry contract is pinned. The upstream exporter has a known review defect in its target-id-priority detector, so this integration does not use that bit as completeness evidence. Minecraft must not freeze a live combatant target to the stored scheduling position, replace the target's current authoritative position, precompute affected tiles, footprint overlap or line of sight, rewrite target mode, supply RNG/combat inputs, consume or refund move frequency/actions, bypass target binding or invent tile/area semantics."));
        result.put(UpstreamCompatibilityMatrix.Capability.TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "BattleEnvironmentState owns weather, PTU terrain identity, Tailwind teams, grounded state, mounted relationships, initiative ordering modes and duration-bearing FieldEffectEntry state for terrain, zones and rooms. FieldRoundLifecycleHook executes FieldRoundProgression first in the canonical ROUND_START registry, preserves terrain -> zones -> rooms ordering, mutates the authoritative environment, applies FieldStatusCleanupRequest for Wonder Room and emits FieldEffectEndedEvent semantic playback before delayed-hit maturity runs at the next lifecycle slot.",
                        "This remains partial field-system support. ROUND_START duration progression and expiry cleanup are authoritative, but full terrain effects, weather progression, hazards, zone mechanics, reactions, field creation, forced movement and complete Python round behavior are still incomplete. Minecraft block observations, entity state and presentation metadata must not create PTU field entries, advance durations, perform Wonder Room cleanup or invent missing field mechanics."));
        result.put(UpstreamCompatibilityMatrix.Capability.ABILITIES,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "Initiative-time weather/terrain ability resolution and Rider Agility Training read canonical BattleRuntimeState environment, Trainer Feature and temporary-effect state rather than adapter inputs. Matured delayed-hit combat preparation also re-runs current authoritative move/damage/post-damage hooks from runtime state during ROUND_START instead of preserving stale adapter-supplied modifiers.",
                        "These remain bounded initiative and delayed-hit paths and do not complete the PTU ability library. Minecraft must not grant abilities, calculate ability/Feature-driven initiative modifiers, or supply delayed-hit hook results."));
        result.put(UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "TrainerRuntimeState owns Feature identities, AP, initiative modifier, skill ranks, explicit initiative Speed, team identity and Trainer action buckets. Runtime initiative assembly consumes that profile together with authoritative Pokemon and BattleEnvironmentState ordering modes; Rider Agility Training and Hardened Initiative remain server-owned consumers.",
                        "Only bounded Trainer Feature/skill/initiative consumers and Trainer initiative-slot execution are implemented. Minecraft may transport frozen canonical Trainer identities and inputs but must not grant Features, choose skill ranks, invent Trainer Speed/team, infer mounts from passengers, choose League ordering, execute perks, construct Trainer-specific action spaces or calculate Trainer/Rider/Hardened outcomes."));
        result.put(UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.BLOCKING,
                        "Adapter-neutral entity-bound playback, PresentationEntityGateway and a reservation-scoped live-handle registry/backend boundary exist in the integration project. The semantic playback boundary preserves global field_effect expiry before later authoritative delayed move_resolved playback, while move animation and final HP projection remain derived from Java's stable event contract. The runtime environment seed preserves canonical duration-bearing field identities for later Java materialization.",
                        "No Fabric/Cobblemon/Craftics runtime has executed this boundary yet, so live adapter/playback remains blocking despite the headless presentation and runtime-seed infrastructure."));
        return Map.copyOf(result);
    }
}
