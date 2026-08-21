package io.autoptu.cobblemon.battlecore;

import java.util.EnumMap;
import java.util.Map;

/**
 * Records the exact upstream heads inspected for this integration slice and the bounded capability
 * deltas that landed after the last broad matrix refresh. It supplements, but never broadens,
 * UpstreamCompatibilityMatrix support classifications.
 */
public final class CurrentUpstreamCompatibilityInspection {
    public static final String AUTOPTU_JAVA_SHA = "4bab1de9abcc28dc1257af8ad7aa4b803dfaa9c3";
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
                        "BattleRuntimeState supplies canonical Pokemon initiative inputs and RuntimeInitiativeTrainerEntryFactory projects Trainer initiative from TrainerRuntimeState. TrainerRuntimeState owns Feature identities, AP, initiative modifier, skill ranks, explicitInitiativeSpeed, teamId and server-owned action buckets. InitiativeAssemblyInstaller accepts canonical Pokemon or Trainer identities, and BattleRoundController now advances mixed Trainer/Pokemon orders, resets the selected Trainer action buckets, opens START and emits turn_start.",
                        "Minecraft must never supply actor kind, Trainer action state, resolved Speed, Trainer explicit Speed, Trainer team, Tailwind key or eligibility, controlled-Pokemon Speed lists, InitiativeEntry, sorted initiative, Hardened bonuses, injury counts, Press On! eligibility, Intimidate rank, mounted-pair eligibility, Rider Agility Training doubling, weather/terrain ability outcomes or grounded claims during initiative resolution."));
        result.put(UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "Canonical Trainer initiative slots can now execute alongside Pokemon slots: TrainerRuntimeState owns action buckets, BattleRoundController selects the Trainer actor, resets its actions, opens START and emits turn_start while canonical initiative projection continues to consume runtime-owned round, environment, injury, temporary-effect and Trainer state.",
                        "Complete lifecycle remains broader than executable Trainer initiative slots. Trainer-specific action-space generation, complete Trainer Feature phase/turn dispatch, round-start Trainer AP/temporary-AP lifecycle, fully autonomous initiative rebuild installation from BattleRuntimeState and broader Python terrain/weather/round effects remain core-owned follow-up work."));
        result.put(UpstreamCompatibilityMatrix.Capability.TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "BattleEnvironmentState provides a server-owned runtime seam for weather, PTU terrain identity, Tailwind teams, per-combatant grounded state and semantic mounted rider->mount relationships; initiative resolution consumes those values authoritatively.",
                        "This is environment/spatial relationship state ownership, not complete terrain/weather/hazard/zone/reaction support. Minecraft block observations, entity passenger state and live pose must not be converted directly into PTU terrain, weather, Tailwind, grounded or mounted legality by the adapter."));
        result.put(UpstreamCompatibilityMatrix.Capability.ABILITIES,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "Initiative-time weather/terrain ability resolution and Rider Agility Training read canonical BattleRuntimeState environment, Trainer Feature and temporary-effect state rather than adapter inputs.",
                        "These remain bounded initiative paths and do not complete the PTU ability library. Minecraft must not grant abilities or calculate ability/Feature-driven initiative modifiers."));
        result.put(UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "TrainerRuntimeState owns Feature identities, AP, initiative modifier, skill ranks, explicit initiative Speed, team identity and Trainer action buckets. RuntimeInitiativeTrainerEntryFactory consumes that profile together with authoritative controlled Pokemon and environment state; Rider Agility Training and Hardened Initiative remain server-owned consumers.",
                        "Only bounded Trainer Feature/skill/initiative consumers and Trainer initiative-slot execution are implemented. Minecraft may transport frozen canonical Trainer identities and inputs but must not grant Features, choose skill ranks, invent Trainer Speed/team, infer mounts from passengers, execute perks, construct Trainer-specific action spaces or calculate Trainer/Rider/Hardened outcomes."));
        result.put(UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.BLOCKING,
                        "Adapter-neutral entity-bound playback, PresentationEntityGateway and a reservation-scoped live-handle registry/backend boundary exist in the integration project.",
                        "No Fabric/Cobblemon/Craftics runtime has executed this boundary yet, so live adapter/playback remains blocking despite the headless registry infrastructure."));
        return Map.copyOf(result);
    }
}
