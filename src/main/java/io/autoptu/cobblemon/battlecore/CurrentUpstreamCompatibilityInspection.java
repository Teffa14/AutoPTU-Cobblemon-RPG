package io.autoptu.cobblemon.battlecore;

import java.util.EnumMap;
import java.util.Map;

/**
 * Records the exact upstream heads inspected for this integration slice and the bounded capability
 * deltas that landed after the last broad matrix refresh. It supplements, but never broadens,
 * UpstreamCompatibilityMatrix support classifications.
 */
public final class CurrentUpstreamCompatibilityInspection {
    public static final String AUTOPTU_JAVA_SHA = "846060ee6c2573e80416928275c5176fff5afa05";
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
                        "BattleRuntimeState supplies canonical initiative inputs. RuntimeInitiativeOrderAssembly.fromState derives Pokemon candidates, Trainer entries, Trick Room ordering and League ordering internally. InitiativeRoundRebuilder.authoritative composes state-derived assembly with InitiativeAssemblyInstaller during rollover. BattleRoundController.advanceInitiativeTurnWithRollover() is now the default production path and selects the authoritative rebuilder internally; the injectable rebuilder overload is deprecated for parity/migration tests. BattleEnvironmentState owns ordering/environment state and TrainerRuntimeState owns server-side Trainer initiative/action inputs. DelayedHitResourcePolicy additionally freezes that delayed-hit maturity does not spend ActionBudget or consume move frequency again because those resources belong to the originating move declaration.",
                        "Minecraft must never supply Trick Room or League ordering flags, initial action availability, actor kind, Trainer action state, resolved Speed, Trainer team, InitiativeEntry, participant filters, a precomputed rollover order, InitiativeRoundRebuilder, any alternative rollover strategy, or delayed-hit action/frequency bookkeeping. The integration also rejects any Trainer ID that collides with a reserved combatant ID before runtime assembly."));
        result.put(UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "The default production rollover call advances the round, derives and installs the next mixed Trainer/Pokemon initiative order only from BattleRuntimeState, applies initiative temporary-effect cleanup, resets the initiative cursor and opens the next actor turn without an adapter-supplied rebuilder. DelayedHitExecutionPolicy freezes the Python delayed-hit execution call chain: due entries enter target resolution, forward target_id and target_position, and that target resolver re-enters ordinary move-action resolution. DelayedHitResourcePolicy now also freezes maturity bookkeeping: it enters target resolution, resolves the attack, spends no action, consumes no frequency and records no ordinary move use at maturity.",
                        "Complete lifecycle remains broader than canonical initiative rollover. Delayed-hit execution and resource ownership are contract-frozen but are not yet connected to ROUND_START in Java. Trainer-specific action-space generation, complete Trainer Feature phase/turn dispatch, round-start Trainer AP/temporary-AP lifecycle and broader Python terrain/weather/round effects remain core-owned follow-up work. The adapter must consume the default authoritative rollover path and must not inject delayed-hit execution, resource bookkeeping or lifecycle strategy."));
        result.put(UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "DelayedHitExecutionPolicy freezes that matured delayed hits enter TARGET_RESOLUTION, forward both target identity and target position, and re-enter the ordinary move-action resolver exactly as the Python oracle does. DelayedHitResourcePolicy freezes the matching resource contract: action and move-frequency bookkeeping occur on the originating declaration path, while maturity resolves the attack without spending action, consuming frequency or recording normal move use again.",
                        "These are execution and resource-bookkeeping contracts, not live ROUND_START delayed-hit parity. Minecraft must not execute delayed hits, bypass target resolution, call move execution directly, consume or refund move frequency/actions, record ordinary move use at maturity, or invent target fallback semantics while Java has not wired the contracts into lifecycle execution."));
        result.put(UpstreamCompatibilityMatrix.Capability.TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "BattleEnvironmentState provides a server-owned runtime seam for weather, PTU terrain identity, Tailwind teams, per-combatant grounded state, mounted rider->mount relationships, Trick Room ordering and League battle ordering; initiative consumes those values authoritatively.",
                        "This is environment and battle-mode state ownership, not complete terrain/weather/hazard/zone/reaction support. Minecraft block observations, entity passenger state, live pose and UI/controller flags must not be converted directly into PTU terrain, weather, Tailwind, grounded, mounted, Trick Room or League semantics by the adapter."));
        result.put(UpstreamCompatibilityMatrix.Capability.ABILITIES,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "Initiative-time weather/terrain ability resolution and Rider Agility Training read canonical BattleRuntimeState environment, Trainer Feature and temporary-effect state rather than adapter inputs.",
                        "These remain bounded initiative paths and do not complete the PTU ability library. Minecraft must not grant abilities or calculate ability/Feature-driven initiative modifiers."));
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
