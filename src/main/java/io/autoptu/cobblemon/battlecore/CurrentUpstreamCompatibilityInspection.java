package io.autoptu.cobblemon.battlecore;

import java.util.EnumMap;
import java.util.Map;

/**
 * Records the exact upstream heads inspected for this integration slice and the bounded capability
 * deltas that landed after the last broad matrix refresh. It supplements, but never broadens,
 * UpstreamCompatibilityMatrix support classifications.
 */
public final class CurrentUpstreamCompatibilityInspection {
    public static final String AUTOPTU_JAVA_SHA = "cd4941d146d18e34d985a8783ea8f670dfd6eef0";
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
                        "BattleRuntimeState now supplies RuntimeInitiativePokemonCandidateFactory with BattleEnvironmentState, current injuries, current round, temporary effects, canonical Trainer Features, initiative modifier and Trainer skill ranks. HardenedInitiativeResolution derives Hardened and Press On! bonuses from that server-owned state.",
                        "Minecraft must never supply resolved Speed, InitiativeEntry, sorted initiative, Hardened bonuses, injury counts, Press On! eligibility, Intimidate rank, weather/terrain ability outcomes, Tailwind eligibility or grounded claims during initiative resolution."));
        result.put(UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "Authoritative initiative candidate projection consumes current round, environment state, injury history, temporary effects and Trainer runtime state.",
                        "Complete round lifecycle remains broader than initiative candidate resolution. Remaining Python terrain/weather progression, round effects, Trainer turns and other lifecycle hooks remain core/domain-owned."));
        result.put(UpstreamCompatibilityMatrix.Capability.TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "BattleEnvironmentState provides a server-owned runtime seam for weather, PTU terrain identity, Tailwind teams and per-combatant grounded state; initiative-time weather/terrain ability resolution consumes it authoritatively.",
                        "This is environment state ownership, not complete terrain/weather/hazard/zone/reaction support. Minecraft block observations must not be converted directly into PTU terrain, weather, Tailwind, grounded legality, hazards or reactions by the adapter."));
        result.put(UpstreamCompatibilityMatrix.Capability.ABILITIES,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "Initiative-time weather/terrain ability resolution and Hardened-related temporary-effect state are read from canonical BattleRuntimeState rather than adapter inputs.",
                        "These remain bounded initiative paths and do not complete the PTU ability library. Minecraft must not grant abilities or calculate ability-driven initiative modifiers."));
        result.put(UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "TrainerRuntimeState now owns case-insensitive skill ranks in addition to Feature ownership, AP and initiative modifier. Hardened Initiative reads Press On! ownership and Intimidate rank from the controlling Trainer.",
                        "Only bounded Trainer Feature/skill consumers are implemented. Minecraft may transport frozen canonical Trainer identities and ranks but must not grant Features, choose skill ranks, execute perks or calculate Hardened/Press On! outcomes."));
        result.put(UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.BLOCKING,
                        "Adapter-neutral entity-bound playback, PresentationEntityGateway and a reservation-scoped live-handle registry/backend boundary exist in the integration project.",
                        "No Fabric/Cobblemon/Craftics runtime has executed this boundary yet, so live adapter/playback remains blocking despite the headless registry infrastructure."));
        return Map.copyOf(result);
    }
}
