package io.autoptu.cobblemon.battlecore;

import java.util.EnumMap;
import java.util.Map;

/**
 * Records the exact upstream heads inspected for this integration slice and the bounded capability
 * deltas that landed after the last broad matrix refresh. It supplements, but never broadens,
 * UpstreamCompatibilityMatrix support classifications.
 */
public final class CurrentUpstreamCompatibilityInspection {
    public static final String AUTOPTU_JAVA_SHA = "b705561395b0ae776740e9207b44c1c53856f326";
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
                        "RuntimeInitiativePokemonCandidateFactory now projects canonical BattleRuntimeState through StatusStatResolution, StatResolution, InitiativeSpeedAbilityResolution, InitiativeAdditionalBonusResolution and PokemonInitiativeEntryResolution. HP, status, abilities, temporary effects, active/fainted state and controller identity are read from runtime state rather than adapter-computed initiative values.",
                        "Minecraft must never supply resolved Speed, an InitiativeEntry, participation state, status/ability effects or a sorted order. RuntimeInitiativePokemonContext still carries semantic environment/trainer inputs such as weather, terrain, Tailwind and Trainer modifiers; those inputs must remain server-owned and must not be inferred from client presentation state."));
        result.put(UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "Authoritative initiative candidate projection now reads current round, canonical combatant state and controller binding directly from BattleRuntimeState before order assembly.",
                        "Complete round lifecycle remains broader than candidate projection and initiative installation. Remaining Python round/phase effects and any still-unbound semantic initiative context remain core/domain-owned."));
        result.put(UpstreamCompatibilityMatrix.Capability.ABILITIES,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "Initiative-time ability resolution is now reached from canonical runtime ability identities when producing Pokemon initiative candidates.",
                        "This is a bounded initiative ability path and does not complete the PTU ability library. Minecraft must not grant abilities or calculate initiative ability modifiers."));
        result.put(UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.BLOCKING,
                        "Adapter-neutral entity-bound playback, PresentationEntityGateway and a reservation-scoped live-handle registry/backend boundary exist in the integration project.",
                        "No Fabric/Cobblemon/Craftics runtime has executed this boundary yet, so live adapter/playback remains blocking despite the new headless registry infrastructure."));
        return Map.copyOf(result);
    }
}
