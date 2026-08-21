package io.autoptu.cobblemon.battlecore;

import java.util.EnumMap;
import java.util.Map;

/**
 * Records the exact upstream heads inspected for this integration slice and the bounded capability
 * deltas that landed after the last broad matrix refresh. It supplements, but never broadens,
 * UpstreamCompatibilityMatrix support classifications.
 */
public final class CurrentUpstreamCompatibilityInspection {
    public static final String AUTOPTU_JAVA_SHA = "3c82018e8f9f123500688d59cc94eba565593231";
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
                        "BattleRuntimeState supplies canonical initiative inputs. RuntimeInitiativeOrderAssembly.fromState derives Pokemon candidates, Trainer entries, Trick Room ordering and League ordering internally. InitiativeRoundRebuilder.authoritative composes state-derived assembly with InitiativeAssemblyInstaller during rollover. BattleRoundController.advanceInitiativeTurnWithRollover() is the default production path and selects the authoritative rebuilder internally; the injectable rebuilder overload is deprecated for parity/migration tests. BattleEnvironmentState owns ordering/environment state and TrainerRuntimeState owns server-side Trainer initiative/action inputs. DelayedHitResourcePolicy and BattleRuntime.applyDelayedAuthoritativeMove preserve the originating move declaration's action and frequency spend when a combatant-target delayed hit matures.",
                        "Minecraft must never supply Trick Room or League ordering flags, initial action availability, actor kind, Trainer action state, resolved Speed, Trainer team, InitiativeEntry, participant filters, a precomputed rollover order, InitiativeRoundRebuilder, any alternative rollover strategy, or delayed-hit action/frequency bookkeeping. The integration also rejects any Trainer ID that collides with a reserved combatant ID before runtime assembly."));
        result.put(UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "The default production rollover call advances the round, derives and installs the next mixed Trainer/Pokemon initiative order only from BattleRuntimeState, applies initiative temporary-effect cleanup, resets the initiative cursor and opens the next actor turn without an adapter-supplied rebuilder. DelayedHitExecutionPolicy freezes the Python delayed-hit call chain. RuntimeMoveResolution.applyDelayedUsingAuthoritativeCombatState now re-derives matured COMBATANT-target move metadata, current stat/status projections, evasion, accuracy stage, No Guard, Blur, Probability Control, STAB, type effectiveness, damage modifiers and post-damage hooks from BattleRuntimeState before BattleRuntime.applyDelayedAuthoritativeMove executes the normal authoritative RNG, HP, damage-history and MoveResolvedEvent path without spending action or move frequency again.",
                        "Complete lifecycle remains broader than canonical initiative rollover. Matured combatant-target delayed-hit execution exists, but Java has not yet connected delayed-hit maturity to ROUND_START and TILE target expansion remains deferred. Trainer-specific action-space generation, complete Trainer Feature phase/turn dispatch, round-start Trainer AP/temporary-AP lifecycle and broader Python terrain/weather/round effects remain core-owned follow-up work. The adapter must not trigger delayed-hit maturity, choose due entries, inject execution strategy or supply legacy delayed-hit combat inputs."));
        result.put(UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "DelayedHitExecutionPolicy freezes target-resolution re-entry and DelayedHitResourcePolicy freezes resource ownership. RuntimeMoveResolution.applyDelayedUsingAuthoritativeCombatState now prepares effective metadata and MoveResolutionInput from current BattleRuntimeState before calling BattleRuntime.applyDelayedAuthoritativeMove. Forged legacy AC, evasion, accuracy stage, combat stats, STAB/type-effectiveness inputs, damage modifiers and post-result hook inputs cannot override canonical runtime state; the live COMBATANT-target path still reuses normal PythonRandom, HP mutation, damage history and MoveResolvedEvent emission without a second action/frequency spend.",
                        "This is bounded live delayed-hit execution, not complete delayed-move parity. ROUND_START scheduling/maturity dispatch and TILE target expansion are still missing. Minecraft must not execute delayed hits, decide when they mature, select due entries, bypass target binding, consume or refund move frequency/actions, provide MoveResolutionInput-style combat values, or invent tile/area fallback semantics."));
        result.put(UpstreamCompatibilityMatrix.Capability.TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "BattleEnvironmentState provides a server-owned runtime seam for weather, PTU terrain identity, Tailwind teams, per-combatant grounded state, mounted rider->mount relationships, Trick Room ordering and League battle ordering; initiative consumes those values authoritatively.",
                        "This is environment and battle-mode state ownership, not complete terrain/weather/hazard/zone/reaction support. Minecraft block observations, entity passenger state, live pose and UI/controller flags must not be converted directly into PTU terrain, weather, Tailwind, grounded, mounted, Trick Room or League semantics by the adapter."));
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
