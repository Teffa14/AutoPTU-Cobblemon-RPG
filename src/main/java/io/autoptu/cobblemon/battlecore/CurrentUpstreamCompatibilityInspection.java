package io.autoptu.cobblemon.battlecore;

import java.util.EnumMap;
import java.util.Map;

/**
 * Records the exact upstream heads inspected for this integration slice and the bounded capability
 * deltas that landed after the last broad matrix refresh. It supplements, but never broadens,
 * UpstreamCompatibilityMatrix support classifications.
 */
public final class CurrentUpstreamCompatibilityInspection {
    public static final String AUTOPTU_JAVA_SHA = "77877b3940a02c91e694c0907a89dafaa56726c8";
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
                        "BattleRuntimeState supplies canonical initiative inputs. RuntimeInitiativeOrderAssembly.fromState derives Pokemon candidates, Trainer entries, Trick Room ordering and League ordering internally. InitiativeRoundRebuilder.authoritative composes state-derived assembly with InitiativeAssemblyInstaller during rollover. BattleRoundController.advanceInitiativeTurnWithRollover() is the default production path and selects the authoritative rebuilder internally; the injectable rebuilder overload is deprecated for parity/migration tests. BattleEnvironmentState owns ordering/environment state and TrainerRuntimeState owns server-side Trainer initiative/action inputs. DelayedHitResourcePolicy and DelayedHitLifecycleExecutor preserve the originating move declaration's action and frequency spend when a combatant-target delayed hit matures.",
                        "Minecraft must never supply Trick Room or League ordering flags, initial action availability, actor kind, Trainer action state, resolved Speed, Trainer team, InitiativeEntry, participant filters, a precomputed rollover order, InitiativeRoundRebuilder, any alternative rollover strategy, delayed-hit RNG or delayed-hit action/frequency bookkeeping. The integration also rejects any Trainer ID that collides with a reserved combatant ID before runtime assembly."));
        result.put(UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "The default production rollover call advances the round, derives and installs the next mixed Trainer/Pokemon initiative order only from BattleRuntimeState, applies initiative temporary-effect cleanup, resets the initiative cursor and opens the next actor turn without an adapter-supplied rebuilder. BattleDelayedHitState now owns the delayed queue plus the battle Python-compatible RNG stream, and DelayedHitLifecycleExecutor resolves due COMBATANT-target entries in insertion order using canonical runtime binding and the ordinary authoritative move-resolution path. The upstream round-order parity contract freezes delayed-hit placement after terrain/zones/rooms and before Follow Me/Foresight expiry.",
                        "Complete lifecycle remains broader than the delayed-hit executor. The default BuiltinLifecycleHooks/ BattleRoundController path does not yet expose complete Python terrain, zone, room, delayed-hit, Trainer AP/temporary-AP, send-out Feature and other round-start behavior as one fully ported lifecycle. Due TILE/area delayed hits still fail closed pending a dedicated target-resolution parity slice. Minecraft must not own the delayed queue, RNG, due-entry selection, execution ordering or resource consumption."));
        result.put(UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "DelayedHitExecutionPolicy freezes target-resolution re-entry, DelayedHitResourcePolicy freezes resource ownership, BattleDelayedHitState owns the queue/RNG boundary, and DelayedHitLifecycleExecutor now resolves due COMBATANT-target entries in insertion order. RuntimeMoveResolution.applyDelayedUsingAuthoritativeCombatState re-derives effective metadata and combat inputs from current BattleRuntimeState before executing the normal PythonRandom, HP mutation, damage-history and MoveResolvedEvent path without a second action/frequency spend.",
                        "This is bounded delayed-hit execution, not complete delayed-move parity or the full move library. TILE/area delayed targets remain unsupported. Minecraft must not schedule or mature delayed hits, choose due entries, supply RNG/combat inputs, consume or refund move frequency/actions, bypass target binding or invent tile/area semantics."));
        result.put(UpstreamCompatibilityMatrix.Capability.TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "BattleEnvironmentState provides a server-owned runtime seam for weather, PTU terrain identity, Tailwind teams, per-combatant grounded state, mounted rider->mount relationships, Trick Room ordering and League battle ordering; initiative consumes those values authoritatively. The delayed-hit round-order contract also records the Python ordering position relative to terrain, zones and rooms.",
                        "This is environment/battle-mode state ownership plus ordering evidence, not complete terrain/weather/hazard/zone/reaction support. Minecraft block observations, entity passenger state, live pose and UI/controller flags must not be converted directly into PTU terrain, weather, Tailwind, grounded, mounted, Trick Room, League, zone or hazard semantics by the adapter."));
        result.put(UpstreamCompatibilityMatrix.Capability.ABILITIES,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "Initiative-time weather/terrain ability resolution and Rider Agility Training read canonical BattleRuntimeState environment, Trainer Feature and temporary-effect state rather than adapter inputs. Matured delayed-hit combat preparation also re-runs current authoritative move/damage/post-damage hooks from runtime state instead of preserving stale adapter-supplied modifiers.",
                        "These remain bounded initiative and delayed-hit paths and do not complete the PTU ability library. Minecraft must not grant abilities, calculate ability/Feature-driven initiative modifiers, or supply delayed-hit hook results."));
        result.put(UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "TrainerRuntimeState owns Feature identities, AP, initiative modifier, skill ranks, explicit initiative Speed, team identity and Trainer action buckets. Runtime initiative assembly consumes that profile together with authoritative Pokemon and BattleEnvironmentState ordering modes; Rider Agility Training and Hardened Initiative remain server-owned consumers.",
                        "Only bounded Trainer Feature/skill/initiative consumers and Trainer initiative-slot execution are implemented. Minecraft may transport frozen canonical Trainer identities and inputs but must not grant Features, choose skill ranks, invent Trainer Speed/team, infer mounts from passengers, choose League ordering, execute perks, construct Trainer-specific action spaces or calculate Trainer/Rider/Hardened outcomes."));
        result.put(UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.BLOCKING,
                        "Adapter-neutral entity-bound playback, PresentationEntityGateway and a reservation-scoped live-handle registry/backend boundary exist in the integration project.",
                        "No Fabric/Cobblemon/Craftics runtime has executed this boundary yet, so live adapter/playback remains blocking despite the headless registry infrastructure."));
        return Map.copyOf(result);
    }
}
